package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Keyframe
import com.zaaam.zcut.core.model.SpeedConfig
import com.zaaam.zcut.core.model.SpeedPoint
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.accepts
import java.util.UUID
import kotlin.math.roundToLong

/**
 * Pure timeline operations. Keeping these out of the ViewModel makes them easy
 * to unit-test and keeps every edit path (drag, trim, split, undo) consistent.
 */
object TimelineEditor {

    fun durationMs(clips: List<Clip>): Long =
        clips.maxOfOrNull { it.endMs.coerceAtLeast(0L) } ?: 0L

    fun recalculate(timeline: Timeline): Timeline {
        val duration = timeline.tracks
            .flatMap { it.clips }
            .maxOfOrNull { it.endMs.coerceAtLeast(0L) } ?: 0L
        return timeline.copy(
            durationMs = duration,
            playheadMs = timeline.playheadMs.coerceIn(0L, duration)
        )
    }

    fun addClip(timeline: Timeline, trackId: String, clip: Clip): Timeline {
        val track = timeline.track(trackId) ?: return timeline
        if (!track.type.accepts(clip.type) || !clip.isValid()) return timeline
        val updatedTrack = track.copy(
            clips = (track.clips + clip).sortedBy { it.startMs }
        )
        return recalculate(
            timeline.copy(
                tracks = timeline.tracks.map {
                    if (it.id == updatedTrack.id) updatedTrack else it
                }
            )
        )
    }

    fun removeClip(timeline: Timeline, clipId: String): Timeline {
        val tracks = timeline.tracks.map { track ->
            track.copy(clips = track.clips.filterNot { it.id == clipId })
        }
        return recalculate(timeline.copy(tracks = tracks))
    }

    fun updateClip(timeline: Timeline, clip: Clip): Timeline {
        val sourceTrack = trackOf(timeline, clip.id) ?: return timeline
        if (!sourceTrack.type.accepts(clip.type) || !clip.isValid()) return timeline
        var found = false
        val tracks = timeline.tracks.map { track ->
            val index = track.clips.indexOfFirst { it.id == clip.id }
            if (index < 0) {
                track
            } else {
                found = true
                val clips = track.clips.toMutableList()
                clips[index] = clip
                track.copy(clips = clips.sortedBy { it.startMs })
            }
        }
        return if (found) recalculate(timeline.copy(tracks = tracks)) else timeline
    }

    fun trimClip(
        timeline: Timeline,
        clipId: String,
        newStartMs: Long,
        newEndMs: Long
    ): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        if (!clip.isValid()) return timeline
        val newDurationMs = newEndMs - newStartMs
        if (newStartMs < 0L || newEndMs <= newStartMs || newDurationMs <= 0L) return timeline

        val speed = clip.speed.safeSpeed
        val deltaStartMs = newStartMs - clip.startMs
        val sourceEndMs = if (clip.sourceDurationMs > 0L) {
            val end = clip.sourceStartMs + clip.sourceDurationMs
            if (end < clip.sourceStartMs) Long.MAX_VALUE else end
        } else {
            Long.MAX_VALUE
        }
        val requestedSourceStart = safeAdd(
            clip.sourceStartMs,
            (deltaStartMs.toDouble() * speed).roundToLong()
        )
        val sourceStartMs = requestedSourceStart.coerceIn(0L, sourceEndMs)
        if (clip.sourceDurationMs > 0L && sourceStartMs >= sourceEndMs) return timeline

        val requestedSourceDuration = (newDurationMs.toDouble() * speed).roundToLong()
        val sourceDurationMs = if (clip.sourceDurationMs > 0L) {
            requestedSourceDuration.coerceAtMost(sourceEndMs - sourceStartMs)
        } else {
            requestedSourceDuration
        }
        val keyframes = shiftKeyframes(clip.keyframes, -deltaStartMs)
            .filter { it.timeMs in 0L..newDurationMs }
        val audio = clip.audio.copy(
            fadeInMs = clip.audio.fadeInMs.coerceAtMost(newDurationMs),
            fadeOutMs = clip.audio.fadeOutMs.coerceAtMost(newDurationMs)
        )
        val transitionIn = clip.transitionIn?.copy(
            durationMs = clip.transitionIn.durationMs.coerceAtMost(newDurationMs)
        )
        val transitionOut = clip.transitionOut?.copy(
            durationMs = clip.transitionOut.durationMs.coerceAtMost(newDurationMs)
        )

        val updated = clip.copy(
            startMs = newStartMs,
            durationMs = newDurationMs,
            sourceStartMs = sourceStartMs,
            sourceDurationMs = sourceDurationMs,
            keyframes = keyframes,
            audio = audio,
            transitionIn = transitionIn,
            transitionOut = transitionOut
        )
        return updateClip(timeline, updated)
    }

    fun splitClip(timeline: Timeline, clipId: String, atMs: Long): Timeline {
        var changed = false
        val tracks = timeline.tracks.map { track ->
            val index = track.clips.indexOfFirst { it.id == clipId }
            if (index < 0) {
                track
            } else {
                val clip = track.clips[index]
                if (!clip.isValid() || atMs <= clip.startMs || atMs >= clip.endMs) {
                    track
                } else {
                    val leftDurationMs = atMs - clip.startMs
                    val rightDurationMs = clip.durationMs - leftDurationMs
                    val speed = clip.speed.safeSpeed
                    val totalSourceMs = clip.effectiveSourceDurationMs
                    val leftSourceMs = (leftDurationMs.toDouble() * speed)
                        .roundToLong()
                        .coerceIn(0L, totalSourceMs)
                    val rightSourceMs = (totalSourceMs - leftSourceMs).coerceAtLeast(0L)
                    val fraction = leftDurationMs.toFloat() / clip.durationMs.toFloat()
                    val (leftCurve, rightCurve) = splitCurve(clip.speed, fraction)
                    val leftKeyframes = clip.keyframes
                        .filter { it.timeMs <= leftDurationMs }
                    // Re-seed the right half with each property's value at the cut so
                    // the animation does not jump at the seam.
                    val seamKeyframes = leftKeyframes
                        .groupBy { it.property }
                        .map { (_, keyframes) -> keyframes.maxBy { it.timeMs } }
                        .map { it.copy(timeMs = 0L) }
                    val rightKeyframes = seamKeyframes + clip.keyframes
                        .filter { it.timeMs > leftDurationMs }
                        .map { it.copy(timeMs = it.timeMs - leftDurationMs) }

                    val left = clip.copy(
                        durationMs = leftDurationMs,
                        sourceDurationMs = leftSourceMs,
                        speed = clip.speed.copy(curve = leftCurve),
                        keyframes = leftKeyframes,
                        transitionOut = null,
                        audio = clip.audio.copy(
                            fadeInMs = clip.audio.fadeInMs.coerceAtMost(leftDurationMs),
                            fadeOutMs = 0L
                        )
                    )
                    val right = clip.copy(
                        id = UUID.randomUUID().toString(),
                        startMs = atMs,
                        durationMs = rightDurationMs,
                        sourceStartMs = safeAdd(clip.sourceStartMs, leftSourceMs),
                        sourceDurationMs = rightSourceMs,
                        speed = clip.speed.copy(curve = rightCurve),
                        keyframes = rightKeyframes,
                        transitionIn = null,
                        audio = clip.audio.copy(
                            fadeInMs = 0L,
                            fadeOutMs = clip.audio.fadeOutMs.coerceAtMost(rightDurationMs)
                        )
                    )
                    val clips = track.clips.toMutableList()
                    clips[index] = left
                    clips.add(index + 1, right)
                    changed = true
                    track.copy(clips = clips)
                }
            }
        }
        return if (changed) recalculate(timeline.copy(tracks = tracks)) else timeline
    }

    fun duplicateClip(timeline: Timeline, clipId: String): Timeline {
        val track = trackOf(timeline, clipId) ?: return timeline
        val clip = track.clips.first { it.id == clipId }
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            startMs = clip.endMs,
            transitionIn = null,
            audio = clip.audio.copy(fadeInMs = 0L)
        )
        return addClip(timeline, track.id, copy)
    }

    fun moveClip(
        timeline: Timeline,
        clipId: String,
        targetTrackId: String,
        newStartMs: Long
    ): Timeline {
        val sourceTrack = trackOf(timeline, clipId) ?: return timeline
        val targetTrack = timeline.track(targetTrackId) ?: return timeline
        val clip = sourceTrack.clips.first { it.id == clipId }
        if (!clip.isValid()) return timeline
        if (!targetTrack.type.accepts(clip.type)) return timeline
        if (newStartMs < 0L || newStartMs > Long.MAX_VALUE - clip.durationMs) return timeline
        val without = timeline.copy(
            tracks = timeline.tracks.map { track ->
                track.copy(clips = track.clips.filterNot { it.id == clipId })
            }
        )
        val moved = clip.copy(startMs = newStartMs)
        return addClip(without, targetTrackId, moved)
    }

    fun setPlayhead(timeline: Timeline, positionMs: Long): Timeline {
        val duration = timeline.durationMs.coerceAtLeast(0L)
        return timeline.copy(playheadMs = positionMs.coerceIn(0L, duration))
    }

    fun sortClips(timeline: Timeline): Timeline =
        timeline.copy(
            tracks = timeline.tracks.map { track ->
                track.copy(clips = track.clips.sortedBy { it.startMs })
            }
        )

    /**
     * Drops clips a track cannot hold or that fail [isValid], then recalculates.
     * Used for untrusted input (decoded drafts, imported assets) so a damaged clip
     * cannot linger in a permanently uneditable state.
     */
    fun sanitize(timeline: Timeline): Timeline {
        val tracks = timeline.tracks.map { track ->
            track.copy(
                clips = track.clips
                    .filter { track.type.accepts(it.type) && it.isValid() }
                    .sortedBy { it.startMs }
            )
        }
        return recalculate(timeline.copy(tracks = tracks))
    }

    fun trackOf(timeline: Timeline, clipId: String): Track? =
        timeline.tracks.firstOrNull { track ->
            track.clips.any { it.id == clipId }
        }

    private fun Clip.isValid(): Boolean {
        if (startMs < 0L || durationMs <= 0L) return false
        if (startMs > Long.MAX_VALUE - durationMs) return false
        if (sourceStartMs < 0L || sourceDurationMs < 0L) return false
        if (sourceStartMs > Long.MAX_VALUE - sourceDurationMs) return false
        return true
    }

    private fun safeAdd(left: Long, right: Long): Long = when {
        right > 0L && left > Long.MAX_VALUE - right -> Long.MAX_VALUE
        right < 0L && left < Long.MIN_VALUE - right -> Long.MIN_VALUE
        else -> left + right
    }

    private fun shiftKeyframes(keyframes: List<Keyframe>, deltaMs: Long): List<Keyframe> =
        keyframes.map { keyframe ->
            keyframe.copy(timeMs = safeAdd(keyframe.timeMs, deltaMs))
        }

    private fun splitCurve(
        speed: SpeedConfig,
        fraction: Float
    ): Pair<List<SpeedPoint>, List<SpeedPoint>> {
        if (speed.curve.isEmpty()) return emptyList<SpeedPoint>() to emptyList()
        val safeFraction = fraction.coerceIn(0.0001f, 0.9999f)
        val left = speed.curve
            .filter { it.atFraction <= safeFraction }
            .map { it.copy(atFraction = (it.atFraction / safeFraction).coerceIn(0f, 1f)) }
        val right = speed.curve
            .filter { it.atFraction >= safeFraction }
            .map {
                it.copy(
                    atFraction = ((it.atFraction - safeFraction) / (1f - safeFraction))
                        .coerceIn(0f, 1f)
                )
            }
        return left to right
    }
}
