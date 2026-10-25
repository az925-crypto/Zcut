package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import java.util.UUID

/**
 * Text-specific timeline operations.
 *
 * Text lives on a `TEXT` track as a [ClipType.TEXT] clip with a [TextConfig].
 * The editor preview draws it with Compose; Media3 never sees it. All writes go
 * through [TimelineEditor] so overlap, duration and playhead invariants stay in
 * one place.
 */
object TextEditor {

    const val DEFAULT_DURATION_MS = 3_000L
    const val MIN_DURATION_MS = 300L
    const val MAX_DURATION_MS = 3_600_000L
    const val MAX_TEXT_LENGTH = 1_000

    /** Adds a text clip at [atMs], pushing right to the first free slot. */
    fun addText(
        timeline: Timeline,
        config: TextConfig,
        atMs: Long,
        durationMs: Long = DEFAULT_DURATION_MS,
        opacity: Float = 1f,
        clipId: String = UUID.randomUUID().toString()
    ): Timeline {
        val safeConfig = normalize(config)
        if (safeConfig.text.isBlank()) return timeline
        val safeDuration = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        val existing = timeline.tracks.firstOrNull { it.type == TrackType.TEXT }
        val track = existing ?: Track(type = TrackType.TEXT)
        val start = firstFreeStart(track.clips, atMs.coerceAtLeast(0L), safeDuration)
        if (start > Long.MAX_VALUE - safeDuration) return timeline
        val clip = Clip(
            id = clipId,
            type = ClipType.TEXT,
            startMs = start,
            durationMs = safeDuration,
            opacity = opacity.coerceIn(0f, 1f),
            text = safeConfig
        )
        val withTrack = if (existing == null) {
            timeline.copy(tracks = timeline.tracks + track)
        } else {
            timeline
        }
        return TimelineEditor.addClip(withTrack, track.id, clip)
    }

    /** Updates an existing text clip; non-text ids are rejected, never rewritten. */
    fun setText(
        timeline: Timeline,
        clipId: String,
        config: TextConfig,
        opacity: Float? = null
    ): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (clip.type != ClipType.TEXT) return timeline
        val safe = normalize(config)
        return TimelineEditor.editClip(timeline, clipId) { current ->
            current.copy(
                text = safe,
                opacity = opacity?.coerceIn(0f, 1f) ?: current.opacity
            )
        }
    }

    fun removeText(timeline: Timeline, clipId: String): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (clip.type != ClipType.TEXT) return timeline
        return TimelineEditor.removeClip(timeline, clipId)
    }

    fun textClips(timeline: Timeline): List<Clip> =
        timeline.tracks
            .asSequence()
            .filter { it.type == TrackType.TEXT }
            .flatMap { it.clips.asSequence() }
            .sortedBy { it.startMs }
            .toList()

    /** Text clips whose `[start, end)` range contains [positionMs]. */
    fun activeAt(timeline: Timeline, positionMs: Long): List<Clip> =
        textClips(timeline).filter { positionMs >= it.startMs && positionMs < it.endMs }

    fun hasText(timeline: Timeline): Boolean =
        timeline.tracks.any { it.type == TrackType.TEXT && it.clips.isNotEmpty() }

    /** First line of the text, used as the timeline clip label. */
    fun label(clip: Clip): String {
        val firstLine = clip.text?.text
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
        return firstLine?.take(40) ?: "Teks"
    }

    /**
     * Clamps absurd values from an untrusted draft. Never drops a clip: a user's
     * text must survive a decode even when its style was corrupted.
     */
    fun sanitize(timeline: Timeline): Timeline = timeline.copy(
        tracks = timeline.tracks.map { track ->
            if (track.type != TrackType.TEXT) {
                track
            } else {
                track.copy(
                    clips = track.clips.map { clip ->
                        val config = clip.text
                        if (clip.type == ClipType.TEXT && config != null) {
                            clip.copy(text = normalize(config))
                        } else {
                            clip
                        }
                    }
                )
            }
        }
    )

    fun normalize(config: TextConfig): TextConfig = config.copy(
        text = config.text.take(MAX_TEXT_LENGTH),
        fontFamily = TextFonts.canonical(config.fontFamily),
        fontSizeSp = safe(config.fontSizeSp, 42f).coerceIn(8f, 200f),
        letterSpacingEm = safe(config.letterSpacingEm, 0f).coerceIn(-0.05f, 1f),
        lineHeight = safe(config.lineHeight, 1.2f).coerceIn(0.8f, 3f),
        strokeWidth = safe(config.strokeWidth, 0f).coerceIn(0f, 20f)
    )

    private fun safe(value: Float, fallback: Float): Float =
        if (value.isFinite()) value else fallback

    private fun firstFreeStart(
        clips: List<Clip>,
        desiredStartMs: Long,
        durationMs: Long
    ): Long {
        var start = desiredStartMs.coerceAtLeast(0L)
        repeat(clips.size + 1) {
            val hit = clips
                .filter { overlaps(start, durationMs, it) }
                .minByOrNull { it.startMs }
                ?: return start
            start = hit.endMs
            if (start > Long.MAX_VALUE - durationMs) return Long.MAX_VALUE
        }
        return start
    }

    private fun overlaps(startMs: Long, durationMs: Long, other: Clip): Boolean {
        if (durationMs <= 0L) return false
        val endMs = if (startMs > Long.MAX_VALUE - durationMs) {
            Long.MAX_VALUE
        } else {
            startMs + durationMs
        }
        return startMs < other.endMs && other.startMs < endMs
    }
}
