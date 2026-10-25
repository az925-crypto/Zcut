package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ChromaConfig
import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.EffectConfig
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.Keyframe
import com.zaaam.zcut.core.model.MaskConfig
import com.zaaam.zcut.core.model.SpeedConfig
import com.zaaam.zcut.core.model.SpeedPoint
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TransitionConfig
import com.zaaam.zcut.core.model.accepts
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/** Which side of a clip a transition belongs to. */
enum class TransitionSide { IN, OUT }

/**
 * Pure timeline operations. Keeping these out of the ViewModel makes them easy
 * to unit-test and keeps every edit path (drag, trim, split, undo) consistent.
 *
 * Invariants:
 * - clips on one track never overlap after an edit;
 * - `durationMs` is always recomputed from clip ends;
 * - source ranges stay inside `[sourceStart, sourceStart + sourceDuration]`;
 * - a speed curve is remapped, never discarded, when a clip is cut.
 */
object TimelineEditor {

    /** Magnet distance used by drag/trim in the editor. */
    const val SNAP_THRESHOLD_MS = 120L

    /** Transitions shorter than this are not useful and usually accidental. */
    const val MIN_TRANSITION_MS = 100L

    fun recalculate(timeline: Timeline): Timeline {
        val duration = timeline.tracks
            .flatMap { it.clips }
            .maxOfOrNull { it.endMs.coerceAtLeast(0L) } ?: 0L
        return timeline.copy(
            durationMs = duration,
            playheadMs = timeline.playheadMs.coerceIn(0L, duration),
            // Every mutation funnels through here, so a structural edit can never
            // leave a transition dangling at a boundary that no longer exists.
            tracks = timeline.tracks.map { track ->
                track.copy(clips = repairTransitions(track.clips))
            }
        )
    }

    /**
     * Drops or clamps transitions whose boundary changed under a structural
     * edit: no neighbour, a neighbour shorter than the transition, or a clip
     * too short for any transition. A transition can only survive when both
     * sides are at least [MIN_TRANSITION_MS] long.
     */
    private fun repairTransitions(clips: List<Clip>): List<Clip> =
        clips.mapIndexed { index, clip ->
            val previous = clips.getOrNull(index - 1)
            val next = clips.getOrNull(index + 1)
            clip.copy(
                transitionIn = clampedTransition(clip.transitionIn, clip.durationMs, previous?.durationMs),
                transitionOut = clampedTransition(clip.transitionOut, clip.durationMs, next?.durationMs)
            )
        }

    private fun clampedTransition(
        config: TransitionConfig?,
        ownDurationMs: Long,
        neighbourDurationMs: Long?
    ): TransitionConfig? {
        if (config == null) return null
        val max = neighbourDurationMs?.let { minOf(ownDurationMs, it) } ?: 0L
        if (max < MIN_TRANSITION_MS) return null
        return config.copy(durationMs = config.durationMs.coerceIn(MIN_TRANSITION_MS, max))
    }

    fun addClip(timeline: Timeline, trackId: String, clip: Clip): Timeline {
        val track = timeline.track(trackId) ?: return timeline
        if (!track.type.accepts(clip.type) || !clip.isValid()) return timeline
        if (track.clips.any { it.id != clip.id && overlaps(clip.startMs, clip.durationMs, it) }) {
            return timeline
        }
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

    /** Removes a clip and closes the gap it leaves on its own track. */
    fun rippleDelete(timeline: Timeline, clipId: String): Timeline {
        val track = trackOf(timeline, clipId) ?: return timeline
        val index = track.clips.indexOfFirst { it.id == clipId }
        if (index < 0) return timeline
        val removed = track.clips[index]
        val clips = track.clips.toMutableList()
        clips.removeAt(index)
        for (position in index until clips.size) {
            val clip = clips[position]
            val shifted = (clip.startMs - removed.durationMs).coerceAtLeast(removed.startMs)
            clips[position] = clip.copy(startMs = shifted)
        }
        val updatedTrack = track.copy(clips = clips)
        return recalculate(
            timeline.copy(
                tracks = timeline.tracks.map {
                    if (it.id == updatedTrack.id) updatedTrack else it
                }
            )
        )
    }

    fun deleteClip(timeline: Timeline, clipId: String, ripple: Boolean): Timeline =
        if (ripple) rippleDelete(timeline, clipId) else removeClip(timeline, clipId)

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

    /** Applies a non-structural edit (filter, colour, mask, keyframe). */
    fun editClip(timeline: Timeline, clipId: String, transform: (Clip) -> Clip): Timeline {
        val clip = timeline.clip(clipId) ?: return timeline
        return updateClip(timeline, transform(clip))
    }

    fun trimClip(
        timeline: Timeline,
        clipId: String,
        newStartMs: Long,
        newEndMs: Long
    ): Timeline {
        val track = trackOf(timeline, clipId) ?: return timeline
        val index = track.clips.indexOfFirst { it.id == clipId }
        if (index < 0) return timeline
        val clip = track.clips[index]
        if (!clip.isValid()) return timeline

        // Trimming must never create an overlap with a neighbour. The caller can
        // still trim the other edge; silently clamping is better than rejecting
        // the whole gesture when a handle is dragged past a boundary.
        val previousEnd = track.clips.take(index).maxOfOrNull { it.endMs } ?: 0L
        val nextStart = track.clips.drop(index + 1).minOfOrNull { it.startMs } ?: Long.MAX_VALUE
        val start = newStartMs.coerceAtLeast(previousEnd).coerceAtLeast(0L)
        val end = newEndMs.coerceAtMost(nextStart)
        if (start < 0L || end <= start || end > Long.MAX_VALUE - 1L) return timeline

        val speed = clip.speed.safeSpeed
        val totalSourceMs = clip.effectiveSourceDurationMs.coerceAtLeast(0L)
        if (totalSourceMs <= 0L) return timeline
        val sourceEndMs = safeAdd(clip.sourceStartMs, totalSourceMs)
        val deltaStartMs = start - clip.startMs
        val leftSourceMs = SpeedCurve.sourceDurationForPresentationMs(
            sourceDurationMs = totalSourceMs,
            speed = speed,
            curve = clip.speed.curve,
            presentationMs = deltaStartMs
        )
        val newSourceStartMs = safeAdd(clip.sourceStartMs, leftSourceMs)
        if (newSourceStartMs >= sourceEndMs) return timeline

        val fromFraction = (leftSourceMs.toDouble() / totalSourceMs).toFloat()
        val subCurve = SpeedCurve.subCurve(clip.speed.curve, fromFraction, 1f, speed)
        val remainingSourceMs = sourceEndMs - newSourceStartMs
        val maxDurationMs = SpeedCurve.presentationDurationMs(
            sourceDurationMs = remainingSourceMs,
            speed = speed,
            curve = subCurve
        )
        val durationMs = (end - start).coerceAtMost(maxDurationMs)
        if (durationMs <= 0L) return timeline

        val sourceDurationMs = SpeedCurve.sourceDurationForPresentationMs(
            sourceDurationMs = remainingSourceMs,
            speed = speed,
            curve = subCurve,
            presentationMs = durationMs
        ).coerceAtLeast(1L)
        val toFraction = (
            fromFraction + sourceDurationMs.toDouble() / totalSourceMs
            ).toFloat().coerceIn(fromFraction, 1f)
        val keyframes = shiftKeyframes(clip.keyframes, -deltaStartMs)
            .filter { it.timeMs in 0L..durationMs }
        val audio = clip.audio.copy(
            fadeInMs = clip.audio.fadeInMs.coerceAtMost(durationMs),
            fadeOutMs = clip.audio.fadeOutMs.coerceAtMost(durationMs)
        )
        val transitionIn = clip.transitionIn?.copy(
            durationMs = clip.transitionIn.durationMs.coerceAtMost(durationMs)
        )
        val transitionOut = clip.transitionOut?.copy(
            durationMs = clip.transitionOut.durationMs.coerceAtMost(durationMs)
        )

        val updated = clip.copy(
            startMs = start,
            durationMs = durationMs,
            sourceStartMs = newSourceStartMs,
            sourceDurationMs = sourceDurationMs,
            speed = clip.speed.copy(
                curve = SpeedCurve.subCurve(
                    clip.speed.curve,
                    fromFraction,
                    toFraction,
                    speed
                )
            ),
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
                    val totalSourceMs = clip.effectiveSourceDurationMs.coerceAtLeast(1L)
                    val leftSourceMs = SpeedCurve.sourceDurationForPresentationMs(
                        sourceDurationMs = totalSourceMs,
                        speed = speed,
                        curve = clip.speed.curve,
                        presentationMs = leftDurationMs
                    ).coerceIn(0L, totalSourceMs)
                    val rightSourceMs = (totalSourceMs - leftSourceMs).coerceAtLeast(0L)
                    val cutFraction = (leftSourceMs.toDouble() / totalSourceMs)
                        .toFloat().coerceIn(0f, 1f)
                    val leftCurve = SpeedCurve.subCurve(
                        clip.speed.curve, 0f, cutFraction, speed
                    )
                    val rightCurve = SpeedCurve.subCurve(
                        clip.speed.curve, cutFraction, 1f, speed
                    )
                    val leftKeyframes = clip.keyframes
                        .filter { it.timeMs <= leftDurationMs }
                    // Re-seed both halves with every animated property's sampled
                    // value at the cut so the animation shape is preserved (a
                    // ramp keeps ramping instead of truncating to a hold).
                    val cutProperties = clip.keyframes.map { it.property }.distinct()
                    val seamValue = { property: AnimatableProperty ->
                        KeyframeEditor.sample(clip, property, leftDurationMs)
                    }
                    val leftSeams = cutProperties
                        .filter { property ->
                            leftKeyframes.none {
                                it.property == property && it.timeMs == leftDurationMs
                            }
                        }
                        .map { property -> Keyframe(leftDurationMs, property, seamValue(property)) }
                    val shiftedRight = clip.keyframes
                        .filter { it.timeMs > leftDurationMs }
                        .map { it.copy(timeMs = it.timeMs - leftDurationMs) }
                    val rightSeams = cutProperties
                        .filter { property ->
                            shiftedRight.none { it.property == property && it.timeMs == 0L }
                        }
                        .map { property -> Keyframe(0L, property, seamValue(property)) }
                    val rightKeyframes = rightSeams + shiftedRight

                    val left = clip.copy(
                        durationMs = leftDurationMs,
                        sourceDurationMs = leftSourceMs,
                        speed = clip.speed.copy(curve = leftCurve),
                        keyframes = leftKeyframes + leftSeams,
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
        val startMs = firstFreeStart(
            clips = track.clips.filterNot { it.id == clipId },
            desiredStartMs = clip.endMs,
            durationMs = clip.durationMs
        )
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            startMs = startMs,
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
        val startMs = firstFreeStart(
            clips = targetTrack.clips.filterNot { it.id == clipId },
            desiredStartMs = newStartMs,
            durationMs = clip.durationMs
        )
        if (startMs > Long.MAX_VALUE - clip.durationMs) return timeline
        val without = timeline.copy(
            tracks = timeline.tracks.map { track ->
                track.copy(clips = track.clips.filterNot { it.id == clipId })
            }
        )
        val moved = clip.copy(startMs = startMs)
        return addClip(without, targetTrackId, moved)
    }

    /**
     * Changes speed (and optionally a curve), keeping the source range and
     * rippling later clips so the track stays gap-free and overlap-free.
     */
    fun retimeClip(
        timeline: Timeline,
        clipId: String,
        speed: Float,
        curve: List<SpeedPoint> = emptyList()
    ): Timeline {
        val track = trackOf(timeline, clipId) ?: return timeline
        val index = track.clips.indexOfFirst { it.id == clipId }
        if (index < 0) return timeline
        val clip = track.clips[index]
        if (!clip.isValid()) return timeline
        val sourceMs = clip.effectiveSourceDurationMs.coerceAtLeast(1L)
        val safeSpeed = SpeedCurve.clampSpeed(speed)
        // A still image has no frames to ramp, so its duration stays a plain
        // division and any stored curve is dropped instead of silently lying.
        val safeCurve = if (clip.type == ClipType.IMAGE) {
            emptyList()
        } else {
            SpeedCurve.normalized(curve, safeSpeed)
        }
        val durationMs = SpeedCurve.presentationDurationMs(
            sourceDurationMs = sourceMs,
            speed = safeSpeed,
            curve = safeCurve
        ).coerceAtLeast(1L)
        if (clip.startMs > Long.MAX_VALUE - durationMs) return timeline

        // Keyframes are timed in presentation ms; rescale them with the new
        // length so they stay inside the clip instead of drifting off-canvas.
        val keyframes = if (clip.durationMs > 0L) {
            clip.keyframes.map { keyframe ->
                val scaled = (keyframe.timeMs.toDouble() * durationMs / clip.durationMs)
                    .roundToLong()
                keyframe.copy(timeMs = scaled.coerceIn(0L, durationMs))
            }
        } else {
            clip.keyframes
        }
        val updated = clip.copy(
            durationMs = durationMs,
            sourceDurationMs = sourceMs,
            keyframes = keyframes,
            speed = SpeedConfig(
                speed = safeSpeed,
                preservePitch = clip.speed.preservePitch,
                curve = safeCurve
            )
        )
        val deltaMs = durationMs - clip.durationMs
        val clips = track.clips.toMutableList()
        clips[index] = updated
        for (position in index + 1 until clips.size) {
            val later = clips[position]
            val shifted = safeAdd(later.startMs, deltaMs).coerceAtLeast(updated.endMs)
            clips[position] = later.copy(startMs = shifted)
        }
        val updatedTrack = track.copy(clips = clips)
        return recalculate(
            timeline.copy(
                tracks = timeline.tracks.map {
                    if (it.id == updatedTrack.id) updatedTrack else it
                }
            )
        )
    }

    fun setReversed(timeline: Timeline, clipId: String, reversed: Boolean): Timeline =
        editClip(timeline, clipId) { it.copy(isReversed = reversed) }

    fun setFilter(timeline: Timeline, clipId: String, filter: FilterConfig?): Timeline =
        editClip(timeline, clipId) {
            it.copy(filters = if (filter == null) emptyList() else listOf(filter))
        }

    fun setColor(
        timeline: Timeline,
        clipId: String,
        color: ColorAdjustments
    ): Timeline = editClip(timeline, clipId) { it.copy(color = color) }

    fun setEffects(
        timeline: Timeline,
        clipId: String,
        effects: List<EffectConfig>
    ): Timeline = editClip(timeline, clipId) { it.copy(effects = effects) }

    fun setMask(timeline: Timeline, clipId: String, mask: MaskConfig?): Timeline =
        editClip(timeline, clipId) { it.copy(mask = mask) }

    fun setChroma(timeline: Timeline, clipId: String, chroma: ChromaConfig?): Timeline =
        editClip(timeline, clipId) { it.copy(chroma = chroma) }

    fun setTransition(
        timeline: Timeline,
        clipId: String,
        side: TransitionSide,
        config: TransitionConfig?
    ): Timeline {
        val track = trackOf(timeline, clipId) ?: return timeline
        val index = track.clips.indexOfFirst { it.id == clipId }
        if (index < 0) return timeline
        val clip = track.clips[index]
        val neighbour = when (side) {
            TransitionSide.IN -> track.clips.getOrNull(index - 1)
            TransitionSide.OUT -> track.clips.getOrNull(index + 1)
        }
        if (config == null) {
            return editClip(timeline, clipId) {
                if (side == TransitionSide.IN) {
                    it.copy(transitionIn = null)
                } else {
                    it.copy(transitionOut = null)
                }
            }
        }
        if (neighbour == null) return timeline
        val maxDuration = minOf(clip.durationMs, neighbour.durationMs)
        if (maxDuration < MIN_TRANSITION_MS) return timeline
        val safe = config.copy(
            durationMs = config.durationMs.coerceIn(MIN_TRANSITION_MS, maxDuration)
        )
        return editClip(timeline, clipId) {
            if (side == TransitionSide.IN) {
                it.copy(transitionIn = safe)
            } else {
                it.copy(transitionOut = safe)
            }
        }
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

    /** Clip boundaries the editor can magnet to, excluding the clip being moved. */
    fun snapCandidates(timeline: Timeline, excludeClipId: String? = null): List<Long> {
        val candidates = ArrayList<Long>()
        candidates += 0L
        candidates += timeline.durationMs.coerceAtLeast(0L)
        candidates += timeline.playheadMs.coerceAtLeast(0L)
        timeline.tracks.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.id != excludeClipId) {
                    candidates += clip.startMs
                    candidates += clip.endMs
                }
            }
        }
        return candidates.filter { it >= 0L }.distinct().sorted()
    }

    fun snapPosition(
        positionMs: Long,
        candidates: List<Long>,
        thresholdMs: Long = SNAP_THRESHOLD_MS
    ): Long {
        if (positionMs < 0L || thresholdMs <= 0L) return positionMs
        val nearest = candidates.minByOrNull { abs(it - positionMs) } ?: return positionMs
        return if (abs(nearest - positionMs) <= thresholdMs) nearest else positionMs
    }

    /**
     * Drops clips a track cannot hold or that fail [isValid], then resolves
     * legacy overlaps by shifting the later clip right. Used for untrusted input
     * (decoded drafts, imported assets) so a damaged clip cannot linger in a
     * permanently uneditable state.
     */
    fun sanitize(timeline: Timeline): Timeline {
        val tracks = timeline.tracks.map { track ->
            val valid = track.clips
                .filter { track.type.accepts(it.type) && it.isValid() }
                .sortedBy { it.startMs }
            var cursor = 0L
            val repaired = valid.mapNotNull { clip ->
                val start = clip.startMs.coerceAtLeast(cursor)
                if (start > Long.MAX_VALUE - clip.durationMs) {
                    // No room left on the timeline; dropping beats a clip stuck
                    // at Long.MAX_VALUE that no later edit can ever touch.
                    null
                } else {
                    cursor = start + clip.durationMs
                    if (start == clip.startMs) clip else clip.copy(startMs = start)
                }
            }
            track.copy(clips = repaired)
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

    private fun overlaps(startMs: Long, durationMs: Long, other: Clip): Boolean {
        if (durationMs <= 0L) return false
        val endMs = if (startMs > Long.MAX_VALUE - durationMs) {
            Long.MAX_VALUE
        } else {
            startMs + durationMs
        }
        return startMs < other.endMs && other.startMs < endMs
    }

    /**
     * First start at or after [desiredStartMs] where a clip fits without
     * overlapping. Pushing right is the least surprising "fix clash" behaviour:
     * the dragged clip is the one the user is holding.
     */
    internal fun firstFreeStart(
        clips: List<Clip>,
        desiredStartMs: Long,
        durationMs: Long
    ): Long {
        var start = desiredStartMs.coerceAtLeast(0L)
        var guard = 0
        while (guard++ <= clips.size) {
            val hit = clips
                .filter { overlaps(start, durationMs, it) }
                .minByOrNull { it.startMs }
                ?: return start
            if (hit.endMs <= start) return start
            start = hit.endMs
            if (start > Long.MAX_VALUE - durationMs) return Long.MAX_VALUE
        }
        return start
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
}
