package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import java.util.UUID

/**
 * Caption/transcript operations.
 *
 * Captions live in [com.zaaam.zcut.core.model.Transcript] on the project, not on
 * the timeline: a cue is a time window plus text, and the editor composites the
 * cues that cover the playhead. All writes are pure functions on [Transcript] so
 * they can be tested off-device.
 */
object CaptionEditor {

    const val MIN_DURATION_MS = 200L
    const val MAX_DURATION_MS = 60_000L
    const val MAX_CUES = 2_000
    const val MAX_LINE_LENGTH = 200

    /** Default line used until the user styles it (bottom-centred like the mockup). */
    fun defaultStyle(): TextConfig = TextConfig(
        text = "",
        fontSizeSp = 30f,
        stylePreset = TextStylePreset.SHADOW
    )

    /** Cues sorted by start time; the canonical order for display and SRT. */
    fun ordered(transcript: Transcript): List<CaptionCue> =
        transcript.cues.sortedWith(compareBy({ it.startMs }, { it.endMs }))

    /** Cues whose `[start, end)` window contains [positionMs]. */
    fun cuesAt(transcript: Transcript, positionMs: Long): List<CaptionCue> =
        ordered(transcript).filter { positionMs >= it.startMs && positionMs < it.endMs }

    fun addCue(
        transcript: Transcript,
        text: String,
        startMs: Long,
        endMs: Long,
        language: String = transcript.language,
        style: TextConfig? = null,
        cueId: String = UUID.randomUUID().toString()
    ): Transcript {
        val safeText = text.trim().take(MAX_LINE_LENGTH)
        if (safeText.isEmpty()) return transcript
        if (transcript.cues.size >= MAX_CUES) return transcript
        val cue = normalize(
            CaptionCue(
                id = cueId,
                startMs = startMs,
                endMs = endMs,
                text = safeText,
                language = language,
                style = style ?: transcript.cues.firstOrNull()?.style
            )
        )
        return withCues(transcript, transcript.cues + cue, TranscriptSource.MANUAL)
    }

    fun updateCue(
        transcript: Transcript,
        cueId: String,
        text: String? = null,
        startMs: Long? = null,
        endMs: Long? = null
    ): Transcript {
        if (transcript.cues.none { it.id == cueId }) return transcript
        val cues = transcript.cues.map { cue ->
            if (cue.id != cueId) {
                cue
            } else {
                normalize(
                    cue.copy(
                        text = text?.trim()?.take(MAX_LINE_LENGTH) ?: cue.text,
                        startMs = startMs ?: cue.startMs,
                        endMs = endMs ?: cue.endMs
                    )
                )
            }
        }.filter { it.text.isNotEmpty() }
        return withCues(transcript, cues, TranscriptSource.MANUAL)
    }

    fun removeCue(transcript: Transcript, cueId: String): Transcript =
        withCues(transcript, transcript.cues.filterNot { it.id == cueId }, TranscriptSource.MANUAL)

    /**
     * Replaces the whole transcript. Used by the local recogniser and SRT import,
     * which produce a complete list at once; the source keeps the provenance.
     */
    fun replaceCues(
        transcript: Transcript,
        cues: List<CaptionCue>,
        source: TranscriptSource,
        language: String = transcript.language
    ): Transcript = withCues(
        transcript.copy(language = language),
        cues,
        source
    )

    /** Applies one style to every cue (the mockup's "Style" tab). */
    fun setStyle(transcript: Transcript, style: TextConfig): Transcript {
        val safe = TextEditor.normalize(style)
        return withCues(
            transcript,
            transcript.cues.map { it.copy(style = safe) },
            TranscriptSource.MANUAL
        )
    }

    fun clear(transcript: Transcript): Transcript =
        transcript.copy(cues = emptyList(), source = TranscriptSource.MANUAL)

    /** Style actually drawn for a cue, falling back to the default line style. */
    fun styleOf(cue: CaptionCue): TextConfig =
        TextEditor.normalize(cue.style ?: defaultStyle())

    /**
     * Clamps absurd values from an untrusted draft (or a hand-edited SRT) and
     * drops cues that carry no text. Never reorders silently: it sorts, so the
     * stored order matches what the UI and SRT export show.
     */
    fun sanitize(transcript: Transcript): Transcript {
        val cues = transcript.cues
            .mapNotNull { cue ->
                val text = cue.text.trim().take(MAX_LINE_LENGTH)
                if (text.isEmpty()) null else normalize(cue.copy(text = text))
            }
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))
            .take(MAX_CUES)
        return transcript.copy(cues = cues)
    }

    private fun withCues(
        transcript: Transcript,
        cues: List<CaptionCue>,
        source: TranscriptSource
    ): Transcript = transcript.copy(
        cues = cues
            .map(::normalize)
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))
            .take(MAX_CUES),
        source = source
    )

    /** Keeps a cue addressable: non-negative start, minimum duration, real style. */
    private fun normalize(cue: CaptionCue): CaptionCue {
        val start = cue.startMs.coerceAtLeast(0L)
        val end = cue.endMs.coerceAtLeast(start + MIN_DURATION_MS)
        val clampedEnd = if (end - start > MAX_DURATION_MS) start + MAX_DURATION_MS else end
        return cue.copy(
            startMs = start,
            endMs = clampedEnd,
            style = cue.style?.let(TextEditor::normalize)
        )
    }
}