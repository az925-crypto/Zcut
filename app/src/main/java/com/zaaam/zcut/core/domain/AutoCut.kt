package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TrackType
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Local, on-device highlight finder behind the AI Clipper (F22 parity).
 *
 * The artifact (`com.vega.edit.aiclipper`: `AiClipperTask`, `ASRNode`, `AEDNode`,
 * `AIRecognitionHighlightNode`, `AIApplyDraftNode`) runs speech recognition and
 * an audio-event detector and then scores candidate windows before applying the
 * winning ones to a draft. ASR and any trained scorer need a backend, so Zcut
 * implements the part that is honestly local: an energy envelope over the
 * decoded waveform picks the loudest and steadiest windows, and those windows
 * become real clips on a fresh draft. Nothing is uploaded and no score is
 * invented beyond the mapping documented below.
 */
object AutoCut {

    /** Analysis frame / step. 250 ms is fine enough for a 15–60 s window. */
    const val HOP_MS = 250L

    /** The mockup lists three suggested cuts, so three is the cap. */
    const val MAX_SUGGESTIONS = 3

    /** dBFS band mapped onto the 0–100 score badge. */
    const val SILENCE_DBFS = -50f
    const val LOUD_DBFS = -6f

    /** A window this far above/below the mean is called loud/quiet. */
    const val KIND_MARGIN_DB = 3f

    /** Score added to a window whose kind matches the requested style. */
    const val PREFERRED_BONUS = 8

    /** Accepted target lengths offered by the mockup's chips. */
    val TARGETS_MS = listOf(15_000L, 30_000L, 60_000L)

    /** What a window sounds like; the UI shows the label verbatim. */
    enum class Kind(val label: String) {
        LOUD("loudest"),
        STEADY("steady"),
        QUIET("calm")
    }

    data class Window(
        val startMs: Long,
        val durationMs: Long,
        /** 0–100, mapped from the window's RMS level. */
        val score: Int,
        val kind: Kind
    ) {
        val endMs: Long get() = if (startMs > Long.MAX_VALUE - durationMs) {
            Long.MAX_VALUE
        } else {
            startMs + durationMs
        }
    }

    /**
     * Mean RMS in dBFS per [HOP_MS] frame. Silence and an empty input both map
     * to [SILENCE_DBFS] so a caller never divides by zero.
     */
    fun energyEnvelope(
        samples: FloatArray,
        sampleRate: Int,
        frameMs: Long = HOP_MS
    ): FloatArray {
        if (sampleRate <= 0 || frameMs <= 0L) return FloatArray(0)
        val frameSamples = (sampleRate * frameMs / 1_000L).toInt().coerceAtLeast(1)
        val frames = samples.size / frameSamples
        if (frames <= 0) return FloatArray(0)
        return FloatArray(frames) { frame ->
            val from = frame * frameSamples
            var sum = 0.0
            for (index in from until from + frameSamples) {
                val sample = samples[index]
                sum += sample.toDouble() * sample.toDouble()
            }
            val rms = sqrt(sum / frameSamples)
            if (rms <= 1e-6) SILENCE_DBFS else (20.0 * log10(rms)).toFloat()
        }
    }

    /**
     * Every window of [targetDurationMs] across the envelope, scored. A clip
     * shorter than the target yields one window covering the whole envelope
     * rather than nothing.
     */
    fun candidates(
        envelope: FloatArray,
        targetDurationMs: Long,
        frameMs: Long = HOP_MS
    ): List<Window> {
        if (envelope.isEmpty() || targetDurationMs <= 0L) return emptyList()
        val mean = envelope.average().toFloat()
        val wanted = (targetDurationMs / frameMs).toInt().coerceAtLeast(1)
        val span = wanted.coerceAtMost(envelope.size)
        val lastStart = envelope.size - span
        return (0..lastStart).map { start ->
            var sum = 0f
            for (index in start until start + span) sum += envelope[index]
            val level = sum / span
            Window(
                startMs = start * frameMs,
                durationMs = span * frameMs,
                score = scoreFor(level),
                kind = kindFor(level, mean)
            )
        }
    }

    /**
     * Picks up to [max] non-overlapping windows, best score first, returned in
     * playback order.
     *
     * ponytail: greedy-by-score, not the artifact's weighted interval
     * scheduling. With at most three windows the outcome is the same for every
     * input that is not adversarially packed; exact DP buys nothing here. Upgrade
     * path: swap in the DP when the suggestion cap grows past a handful.
     */
    fun select(
        candidates: List<Window>,
        max: Int = MAX_SUGGESTIONS,
        /** Windows of this kind get a nudge; null keeps the plain ranking. */
        prefer: Kind? = null
    ): List<Window> {
        if (candidates.isEmpty() || max <= 0) return emptyList()
        fun rank(window: Window): Int =
            window.score + if (prefer != null && window.kind == prefer) PREFERRED_BONUS else 0

        val picked = ArrayList<Window>()
        candidates
            .sortedWith(compareByDescending<Window> { rank(it) }.thenBy { it.startMs })
            .forEach { candidate ->
                if (picked.size >= max) return@forEach
                val clash = picked.any { it.startMs < candidate.endMs && candidate.startMs < it.endMs }
                if (!clash) picked += candidate
            }
        return picked.sortedBy { it.startMs }
    }

    /**
     * Clones the winning windows onto a fresh draft as real clips of [asset].
     * Source offsets stay inside the asset: a window that starts past the end is
     * dropped, and one that would run off the end is shortened.
     */
    fun draft(asset: MediaAsset, windows: List<Window>, name: String): Project {
        val base = ProjectFactory.create(name = name.ifBlank { asset.displayName })
        if (!asset.isUsable || !asset.isVisual) return base
        val limit = asset.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE

        val track = base.timeline.tracks.first { it.type == TrackType.VIDEO }
        var timeline = base.timeline
        var cursor = 0L
        windows.sortedBy { it.startMs }.forEach { window ->
            if (window.startMs >= limit) return@forEach
            val duration = window.durationMs.coerceAtMost(limit - window.startMs)
            if (duration <= 0L) return@forEach
            val next = TimelineEditor.addClip(
                timeline,
                track.id,
                Clip(
                    assetId = asset.id,
                    type = MediaAppender.clipTypeFor(asset.kind),
                    startMs = cursor,
                    durationMs = duration,
                    sourceStartMs = window.startMs,
                    sourceDurationMs = duration
                )
            )
            if (next != timeline) {
                timeline = next
                cursor += duration
            }
        }
        if (timeline == base.timeline) return base
        return base.copy(assets = listOf(asset), timeline = timeline)
    }

    /** Level → badge score. Loud audio scores high, silence scores zero. */
    private fun scoreFor(dbfs: Float): Int {
        val span = LOUD_DBFS - SILENCE_DBFS
        val fraction = ((dbfs - SILENCE_DBFS) / span).coerceIn(0f, 1f)
        return (fraction * 100f).roundToInt()
    }

    private fun kindFor(dbfs: Float, mean: Float): Kind = when {
        dbfs >= mean + KIND_MARGIN_DB -> Kind.LOUD
        dbfs <= mean - KIND_MARGIN_DB -> Kind.QUIET
        else -> Kind.STEADY
    }
}