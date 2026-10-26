package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.StickerConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import com.zaaam.zcut.core.model.Transform
import java.util.UUID

/**
 * Sticker-specific timeline operations.
 *
 * A sticker lives on a `STICKER` track as a [ClipType.STICKER] clip whose
 * [Clip.sticker] names a glyph from [StickerCatalog]. The clip's own
 * [Transform]/[Clip.opacity] carry the placement, so a sticker keyframes and
 * animates exactly like any other clip. All writes go through [TimelineEditor]
 * so overlap, duration and playhead invariants stay in one place.
 *
 * The preview draws stickers with Compose; Media3 never sees them (same render
 * limit as text). Object tracking would need a backend and is not implemented.
 */
object StickerEditor {

    const val DEFAULT_DURATION_MS = 3_000L
    const val MIN_DURATION_MS = 300L
    const val MAX_DURATION_MS = 3_600_000L

    /** Adds a sticker clip at [atMs], pushing right to the first free slot. */
    fun addSticker(
        timeline: Timeline,
        stickerId: String,
        atMs: Long,
        durationMs: Long = DEFAULT_DURATION_MS,
        transform: Transform = Transform(),
        opacity: Float = 1f,
        clipId: String = UUID.randomUUID().toString()
    ): Timeline {
        val safeDuration = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        val existing = timeline.tracks.firstOrNull { it.type == TrackType.STICKER }
        val track = existing ?: Track(type = TrackType.STICKER)
        val start = TimelineEditor.firstFreeStart(track.clips, atMs.coerceAtLeast(0L), safeDuration)
        if (start > Long.MAX_VALUE - safeDuration) return timeline
        val clip = Clip(
            id = clipId,
            type = ClipType.STICKER,
            startMs = start,
            durationMs = safeDuration,
            transform = normalizeTransform(transform),
            opacity = opacity.coerceIn(0f, 1f),
            sticker = StickerConfig(StickerCatalog.canonical(stickerId).id)
        )
        val withTrack = if (existing == null) {
            timeline.copy(tracks = timeline.tracks + track)
        } else {
            timeline
        }
        return TimelineEditor.addClip(withTrack, track.id, clip)
    }

    /** Updates an existing sticker; other clip types are rejected, never rewritten. */
    fun setSticker(
        timeline: Timeline,
        clipId: String,
        stickerId: String,
        transform: Transform,
        opacity: Float
    ): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (clip.type != ClipType.STICKER) return timeline
        return TimelineEditor.editClip(timeline, clipId) { current ->
            current.copy(
                sticker = StickerConfig(StickerCatalog.canonical(stickerId).id),
                transform = normalizeTransform(transform),
                opacity = opacity.coerceIn(0f, 1f)
            )
        }
    }

    fun removeSticker(timeline: Timeline, clipId: String): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (clip.type != ClipType.STICKER) return timeline
        return TimelineEditor.removeClip(timeline, clipId)
    }

    fun stickerClips(timeline: Timeline): List<Clip> =
        timeline.tracks
            .asSequence()
            .filter { it.type == TrackType.STICKER }
            .flatMap { it.clips.asSequence() }
            .sortedBy { it.startMs }
            .toList()

    /** Sticker clips whose `[start, end)` range contains [positionMs]. */
    fun activeAt(timeline: Timeline, positionMs: Long): List<Clip> =
        stickerClips(timeline).filter { positionMs >= it.startMs && positionMs < it.endMs }

    fun hasStickers(timeline: Timeline): Boolean =
        timeline.tracks.any { it.type == TrackType.STICKER && it.clips.isNotEmpty() }

    /** Catalogue label of the clip's sticker, used as the timeline clip label. */
    fun label(clip: Clip): String =
        StickerCatalog.canonical(clip.sticker?.stickerId.orEmpty()).label

    /** Glyph of the clip's sticker, for the timeline and preview. */
    fun glyph(clip: Clip): String =
        StickerCatalog.canonical(clip.sticker?.stickerId.orEmpty()).glyph

    /**
     * Clamps absurd values from an untrusted draft. Never drops a clip: a user's
     * sticker must survive a decode even when its placement was corrupted.
     */
    fun sanitize(timeline: Timeline): Timeline = timeline.copy(
        tracks = timeline.tracks.map { track ->
            if (track.type != TrackType.STICKER) {
                track
            } else {
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.type == ClipType.STICKER) {
                            clip.copy(
                                sticker = normalize(clip.sticker),
                                transform = normalizeTransform(clip.transform)
                            )
                        } else {
                            clip
                        }
                    }
                )
            }
        }
    )

    fun normalize(config: StickerConfig?): StickerConfig =
        StickerConfig(StickerCatalog.canonical(config?.stickerId.orEmpty()).id)

    /**
     * Reuses the keyframe ranges as the single source of truth for what a
     * transform value may be, so a decoded draft cannot put a sticker off the
     * scale the slider (and the keyframe editor) can address.
     */
    fun normalizeTransform(transform: Transform): Transform = transform.copy(
        positionX = clamp(transform.positionX, AnimatableProperty.POSITION_X),
        positionY = clamp(transform.positionY, AnimatableProperty.POSITION_Y),
        scale = clamp(transform.scale, AnimatableProperty.SCALE),
        rotationDegrees = clamp(transform.rotationDegrees, AnimatableProperty.ROTATION)
    )

    private fun clamp(value: Float, property: AnimatableProperty): Float {
        // Neutral defaults: only scale is centred at 1; position and rotation at 0.
        if (!value.isFinite()) {
            return if (property == AnimatableProperty.SCALE) 1f else 0f
        }
        val range = KeyframeEditor.rangeOf(property)
        return value.coerceIn(range.start, range.endInclusive)
    }
}