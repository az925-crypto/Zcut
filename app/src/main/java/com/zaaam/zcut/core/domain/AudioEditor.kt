package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AudioConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType

/**
 * Audio-specific timeline operations: the per-clip gain envelope and the
 * placement of newly created audio (imported music, recorded voiceover, TTS).
 *
 * The editor preview plays the source audio untouched; the volume/fade envelope
 * is applied by the export mixer (M13). Keeping the maths here means export
 * reuses one implementation instead of inventing a second one.
 */
object AudioEditor {

    const val MAX_VOLUME = 2f
    const val MIN_PITCH = 0.5f
    const val MAX_PITCH = 2f
    const val MAX_FADE_MS = 30_000L

    /** Smallest accepted clip; a zero-length audio clip can never be played. */
    const val MIN_DURATION_MS = 100L

    /** Upper bound on stored beat markers; a corrupt draft cannot flood memory. */
    const val MAX_BEATS = 5_000

    fun audioClips(timeline: Timeline): List<Clip> =
        timeline.tracks
            .asSequence()
            .filter { it.type == TrackType.AUDIO }
            .flatMap { it.clips.asSequence() }
            .sortedBy { it.startMs }
            .toList()

    /**
     * Sets the clip gain. A non-finite request (never produced by the UI, but
     * possible from a bad caller) is ignored instead of resetting the volume.
     */
    fun setVolume(timeline: Timeline, clipId: String, volume: Float): Timeline =
        if (!volume.isFinite()) {
            timeline
        } else {
            updateAudio(timeline, clipId) { it.copy(volume = volume) }
        }

    fun setFades(
        timeline: Timeline,
        clipId: String,
        fadeInMs: Long,
        fadeOutMs: Long
    ): Timeline = updateAudio(timeline, clipId) {
        it.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs)
    }

    fun setMuted(timeline: Timeline, clipId: String, muted: Boolean): Timeline =
        updateAudio(timeline, clipId) { it.copy(muted = muted) }

    fun setLooping(timeline: Timeline, clipId: String, looping: Boolean): Timeline =
        updateAudio(timeline, clipId) { it.copy(looping = looping) }

    fun setPitch(timeline: Timeline, clipId: String, pitch: Float): Timeline =
        updateAudio(timeline, clipId) { it.copy(pitch = pitch) }

    /**
     * Applies [transform] to the clip's audio config and clamps the result.
     * Non-audio clips are rejected, never rewritten.
     */
    fun updateAudio(
        timeline: Timeline,
        clipId: String,
        transform: (AudioConfig) -> AudioConfig
    ): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (clip.type != ClipType.AUDIO) return timeline
        return TimelineEditor.editClip(timeline, clipId) { current ->
            current.copy(audio = normalize(transform(current.audio), current.durationMs))
        }
    }

    /**
     * Gain multiplier at [positionMs] inside a clip of [durationMs], combining
     * the fade-in and fade-out ramps. The weaker of the two ramps wins
     * ([minOf]), so overlapping fades do not multiply into an inaudible clip.
     */
    fun fadeGain(
        positionMs: Long,
        durationMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long
    ): Float {
        val inGain = if (fadeInMs > 0L) {
            (positionMs.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        val remaining = durationMs - positionMs
        val outGain = if (fadeOutMs > 0L) {
            (remaining.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        return minOf(inGain, outGain)
    }

    /** Volume actually used by the mixer: muted wins over any stored gain. */
    fun effectiveVolume(config: AudioConfig): Float =
        if (config.muted) 0f else config.volume

    /**
     * Clamps an audio config against its clip length. Fades are capped at the
     * clip duration so a fade can never outlive the audio it belongs to.
     */
    fun normalize(config: AudioConfig, durationMs: Long): AudioConfig {
        val maxFade = minOf(MAX_FADE_MS, durationMs.coerceAtLeast(0L))
        return config.copy(
            volume = safe(config.volume, 1f).coerceIn(0f, MAX_VOLUME),
            fadeInMs = config.fadeInMs.coerceIn(0L, maxFade),
            fadeOutMs = config.fadeOutMs.coerceIn(0L, maxFade),
            pitch = safe(config.pitch, 1f).coerceIn(MIN_PITCH, MAX_PITCH)
        )
    }

    /**
     * Adds [asset] to the draft and places a clip on the audio track at [atMs],
     * pushing right to the first free slot when the requested time is taken.
     * Returns the draft unchanged when the asset cannot produce a clip.
     */
    fun insertAudio(
        project: Project,
        asset: MediaAsset,
        atMs: Long,
        clipId: String = java.util.UUID.randomUUID().toString()
    ): Project {
        if (!asset.isUsable || asset.kind != MediaKind.AUDIO) return project
        val durationMs = MediaAppender.durationFor(asset).coerceAtLeast(MIN_DURATION_MS)
        if (durationMs > Long.MAX_VALUE - atMs) return project

        val existing = project.timeline.tracks.firstOrNull { it.type == TrackType.AUDIO }
        val track = existing ?: Track(type = TrackType.AUDIO)
        val start = TimelineEditor.firstFreeStart(
            track.clips,
            atMs.coerceAtLeast(0L),
            durationMs
        )
        if (start > Long.MAX_VALUE - durationMs) return project

        val clip = Clip(
            id = clipId,
            assetId = asset.id,
            type = ClipType.AUDIO,
            startMs = start,
            durationMs = durationMs,
            sourceDurationMs = durationMs
        )
        val baseTimeline = if (existing == null) {
            project.timeline.copy(tracks = project.timeline.tracks + track)
        } else {
            project.timeline
        }
        val timeline = TimelineEditor.addClip(baseTimeline, track.id, clip)
        if (timeline == baseTimeline) return project

        val assets = if (project.assets.any { it.id == asset.id }) {
            project.assets
        } else {
            project.assets + asset
        }
        return project.copy(assets = assets, timeline = timeline)
    }

    /**
     * Clamps every audio clip's gain envelope. Never drops a clip: a user's
     * audio must survive a decode even when its envelope was corrupted.
     */
    fun sanitize(timeline: Timeline): Timeline = timeline.copy(
        tracks = timeline.tracks.map { track ->
            if (track.type != TrackType.AUDIO) {
                track
            } else {
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.type == ClipType.AUDIO) {
                            clip.copy(audio = normalize(clip.audio, clip.durationMs))
                        } else {
                            clip
                        }
                    }
                )
            }
        }
    )

    private fun safe(value: Float, fallback: Float): Float =
        if (value.isFinite()) value else fallback

    /** Drops impossible markers and keeps the grid ascending and unique. */
    fun sanitizeBeats(beats: List<Long>): List<Long> = beats
        .filter { it >= 0L }
        .distinct()
        .sorted()
        .take(MAX_BEATS)
}
