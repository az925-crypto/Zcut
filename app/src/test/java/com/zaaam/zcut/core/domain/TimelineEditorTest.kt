package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.AudioConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Keyframe
import com.zaaam.zcut.core.model.SpeedConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import com.zaaam.zcut.core.model.TransitionConfig
import com.zaaam.zcut.core.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEditorTest {

    private fun baseTimeline(): Triple<com.zaaam.zcut.core.model.Timeline, String, String> {
        val project = ProjectFactory.create("Test")
        val video = project.timeline.tracks.first { it.type == TrackType.VIDEO }
        val overlay = project.timeline.tracks.first { it.type == TrackType.OVERLAY }
        return Triple(project.timeline, video.id, overlay.id)
    }

    private fun clip(durationMs: Long = 1_000L): Clip = Clip(
        assetId = "asset-1",
        startMs = 0L,
        durationMs = durationMs,
        sourceDurationMs = durationMs
    )

    @Test
    fun addClipUpdatesTimelineDuration() {
        val (timeline, videoId, _) = baseTimeline()
        val updated = TimelineEditor.addClip(timeline, videoId, clip())

        assertEquals(1_000L, updated.durationMs)
        assertEquals(1, updated.tracks.first { it.id == videoId }.clips.size)
    }

    @Test
    fun splitClipCreatesTwoSourceRanges() {
        val (timeline, videoId, _) = baseTimeline()
        val source = clip()
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val split = TimelineEditor.splitClip(withClip, source.id, 400L)
        val clips = split.tracks.first { it.id == videoId }.clips

        assertEquals(2, clips.size)
        assertEquals(400L, clips[0].durationMs)
        assertEquals(600L, clips[1].durationMs)
        assertEquals(400L, clips[1].sourceStartMs)
        assertEquals(1_000L, split.durationMs)
    }

    @Test
    fun trimClipShiftsSourceStart() {
        val (timeline, videoId, _) = baseTimeline()
        val source = clip()
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val trimmed = TimelineEditor.trimClip(withClip, source.id, 100L, 700L)
        val result = trimmed.tracks.first { it.id == videoId }.clips.first()

        assertEquals(100L, result.startMs)
        assertEquals(600L, result.durationMs)
        assertEquals(100L, result.sourceStartMs)
    }

    @Test
    fun moveClipChangesTrackAndStart() {
        val (timeline, videoId, overlayId) = baseTimeline()
        val source = clip()
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val moved = TimelineEditor.moveClip(withClip, source.id, overlayId, 500L)

        assertTrue(moved.tracks.first { it.id == videoId }.clips.isEmpty())
        val movedClip = moved.tracks.first { it.id == overlayId }.clips.single()
        assertEquals(500L, movedClip.startMs)
        assertEquals(1_500L, moved.durationMs)
    }

    @Test
    fun videoClipCannotMoveToAudioTrack() {
        val project = ProjectFactory.create("Test")
        val video = project.timeline.tracks.first { it.type == TrackType.VIDEO }
        val audio = project.timeline.tracks.first { it.type == TrackType.AUDIO }
        val source = clip()
        val withClip = TimelineEditor.addClip(project.timeline, video.id, source)

        val moved = TimelineEditor.moveClip(withClip, source.id, audio.id, 0L)

        assertEquals(1, moved.tracks.first { it.id == video.id }.clips.size)
        assertTrue(moved.tracks.first { it.id == audio.id }.clips.isEmpty())
    }

    @Test
    fun duplicateClipPlacesCopyAfterOriginal() {
        val (timeline, videoId, _) = baseTimeline()
        val source = clip()
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val duplicated = TimelineEditor.duplicateClip(withClip, source.id)
        val clips = duplicated.tracks.first { it.id == videoId }.clips

        assertEquals(2, clips.size)
        assertEquals(1_000L, clips[1].startMs)
        assertEquals(2_000L, duplicated.durationMs)
    }

    @Test
    fun invalidNegativeClipIsIgnored() {
        val (timeline, videoId, _) = baseTimeline()
        val updated = TimelineEditor.addClip(
            timeline,
            videoId,
            Clip(startMs = -100L, durationMs = 50L)
        )

        assertTrue(updated.tracks.first { it.id == videoId }.clips.isEmpty())
        assertEquals(0L, updated.durationMs)
    }

    @Test
    fun recalculateClampsNegativeDurationAndPlayhead() {
        val corrupt = Timeline(
            tracks = listOf(
                Track(
                    type = TrackType.VIDEO,
                    clips = listOf(Clip(startMs = -100L, durationMs = 50L))
                )
            ),
            durationMs = -50L,
            playheadMs = 100L
        )

        val fixed = TimelineEditor.recalculate(corrupt)

        assertEquals(0L, fixed.durationMs)
        assertEquals(0L, fixed.playheadMs)
    }

    @Test
    fun splitUsesSpeedAwareSourceRangesAndClearsEdgeTransitions() {
        val (timeline, videoId, _) = baseTimeline()
        val source = Clip(
            startMs = 0L,
            durationMs = 1_000L,
            sourceDurationMs = 2_000L,
            speed = SpeedConfig(speed = 2f),
            keyframes = listOf(
                Keyframe(200L, AnimatableProperty.OPACITY, 1f),
                Keyframe(800L, AnimatableProperty.OPACITY, 0f)
            ),
            transitionIn = TransitionConfig(TransitionType.FADE, 300L),
            transitionOut = TransitionConfig(TransitionType.WHIP, 300L),
            audio = AudioConfig(fadeInMs = 200L, fadeOutMs = 400L)
        )
        val withClip = TimelineEditor.addClip(timeline, videoId, source)
        val split = TimelineEditor.splitClip(withClip, source.id, 500L)
        val clips = split.tracks.first { it.id == videoId }.clips

        assertEquals(1_000L, clips[0].sourceDurationMs)
        assertEquals(1_000L, clips[1].sourceDurationMs)
        assertEquals(1_000L, clips[1].sourceStartMs)
        assertNull(clips[0].transitionOut)
        assertNull(clips[1].transitionIn)
        assertEquals(1, clips[0].keyframes.size)
        // The right half is re-seeded with the left half's value at the cut (t = 0)
        // so the animation is continuous, then keeps the later keyframe.
        assertEquals(2, clips[1].keyframes.size)
        assertEquals(0L, clips[1].keyframes.first().timeMs)
        assertEquals(300L, clips[1].keyframes.last().timeMs)
        assertEquals(0L, clips[0].audio.fadeOutMs)
        assertEquals(0L, clips[1].audio.fadeInMs)
    }

    @Test
    fun trimWithHugeOffsetDoesNotWrapSourceStart() {
        val (timeline, videoId, _) = baseTimeline()
        val source = Clip(
            startMs = 0L,
            durationMs = 1_000L,
            sourceStartMs = 1_000L,
            speed = SpeedConfig(speed = 8f)
        )
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val trimmed = TimelineEditor.trimClip(
            withClip,
            source.id,
            1_200_000_000_000_000_000L,
            1_200_000_000_000_001_000L
        )
        val result = trimmed.tracks.first { it.id == videoId }.clips.single()

        assertEquals(0L, result.startMs)
        assertEquals(1_000L, result.sourceStartMs)
    }

    @Test
    fun sanitizeDropsInvalidAndMismatchedClips() {
        val corrupt = Timeline(
            tracks = listOf(
                Track(
                    type = TrackType.VIDEO,
                    clips = listOf(
                        Clip(startMs = -100L, durationMs = 50L),
                        Clip(startMs = 0L, durationMs = 200L),
                        Clip(type = com.zaaam.zcut.core.model.ClipType.TEXT, startMs = 0L, durationMs = 100L)
                    )
                )
            )
        )

        val fixed = TimelineEditor.sanitize(corrupt)
        val clips = fixed.tracks.single().clips

        assertEquals(1, clips.size)
        assertEquals(200L, fixed.durationMs)
    }

    @Test
    fun trimClampsRightEdgeToNeighbour() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val withFirst = TimelineEditor.addClip(timeline, videoId, first)
        val withBoth = TimelineEditor.addClip(withFirst, videoId, second)

        val trimmed = TimelineEditor.trimClip(withBoth, first.id, 0L, 1_500L)
        val result = trimmed.tracks.first { it.id == videoId }.clips.first { it.id == first.id }

        assertEquals(1_000L, result.durationMs)
        assertEquals(1_000L, result.endMs)
    }

    @Test
    fun trimClampsRightEdgeToSourceDuration() {
        val (timeline, videoId, _) = baseTimeline()
        val source = clip().copy(sourceDurationMs = 500L)
        val withClip = TimelineEditor.addClip(timeline, videoId, source)

        val trimmed = TimelineEditor.trimClip(withClip, source.id, 0L, 900L)
        val result = trimmed.tracks.first { it.id == videoId }.clips.single()

        assertEquals(500L, result.durationMs)
        assertEquals(500L, result.sourceDurationMs)
    }

    @Test
    fun rippleDeleteClosesGapAndKeepsLaterClipsContiguous() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val third = clip().copy(startMs = 2_000L)
        val withClips = listOf(first, second, third).fold(timeline) { current, item ->
            TimelineEditor.addClip(current, videoId, item)
        }

        val deleted = TimelineEditor.rippleDelete(withClips, second.id)
        val clips = deleted.tracks.first { it.id == videoId }.clips

        assertEquals(listOf(0L, 1_000L), clips.map { it.startMs })
        assertEquals(2_000L, deleted.durationMs)
    }

    @Test
    fun deleteWithoutRippleLeavesGap() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val deleted = TimelineEditor.deleteClip(withClips, first.id, ripple = false)
        val remaining = deleted.tracks.first { it.id == videoId }.clips.single()

        assertEquals(1_000L, remaining.startMs)
        assertEquals(2_000L, deleted.durationMs)
    }

    @Test
    fun addClipRejectsOverlapOnSameTrack() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 500L)
        val withFirst = TimelineEditor.addClip(timeline, videoId, first)

        val withSecond = TimelineEditor.addClip(withFirst, videoId, second)

        assertEquals(1, withSecond.tracks.first { it.id == videoId }.clips.size)
    }

    @Test
    fun moveClipPushesToFirstFreeSlot() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 2_000L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val moved = TimelineEditor.moveClip(withClips, second.id, videoId, 500L)
        val result = moved.tracks.first { it.id == videoId }.clips.first { it.id == second.id }

        assertEquals(1_000L, result.startMs)
    }

    @Test
    fun snapPositionOnlyMovesInsideThreshold() {
        val candidates = listOf(0L, 1_000L, 2_000L)

        assertEquals(1_000L, TimelineEditor.snapPosition(1_050L, candidates, 120L))
        assertEquals(1_500L, TimelineEditor.snapPosition(1_500L, candidates, 120L))
        assertEquals(1_000L, TimelineEditor.snapPosition(880L, candidates, 120L))
    }

    @Test
    fun retimeFasterRipplesLaterClipsLeft() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val retimed = TimelineEditor.retimeClip(withClips, first.id, 2f)
        val clips = retimed.tracks.first { it.id == videoId }.clips

        assertEquals(500L, clips.first { it.id == first.id }.durationMs)
        assertEquals(500L, clips.first { it.id == second.id }.startMs)
        assertEquals(1_500L, retimed.durationMs)
    }

    @Test
    fun retimeSlowerPushesLaterClipsRight() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val retimed = TimelineEditor.retimeClip(withClips, first.id, 0.5f)
        val clips = retimed.tracks.first { it.id == videoId }.clips

        assertEquals(2_000L, clips.first { it.id == first.id }.durationMs)
        assertEquals(2_000L, clips.first { it.id == second.id }.startMs)
        assertEquals(3_000L, retimed.durationMs)
    }

    @Test
    fun transitionDurationIsClampedToBothClips() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip(durationMs = 400L)
        val second = clip(durationMs = 1_000L).copy(startMs = 400L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val withTransition = TimelineEditor.setTransition(
            withClips,
            second.id,
            TransitionSide.IN,
            TransitionConfig(TransitionType.DISSOLVE, 5_000L)
        )
        val result = withTransition.tracks.first { it.id == videoId }
            .clips.first { it.id == second.id }

        assertEquals(400L, result.transitionIn?.durationMs)
    }

    @Test
    fun sanitizeShiftsLegacyOverlaps() {
        val corrupt = Timeline(
            tracks = listOf(
                Track(
                    type = TrackType.VIDEO,
                    clips = listOf(
                        Clip(startMs = 0L, durationMs = 1_000L),
                        Clip(startMs = 500L, durationMs = 1_000L)
                    )
                )
            )
        )

        val fixed = TimelineEditor.sanitize(corrupt)
        val clips = fixed.tracks.single().clips

        assertEquals(listOf(0L, 1_000L), clips.map { it.startMs })
        assertEquals(2_000L, fixed.durationMs)
    }

    @Test
    fun duplicateClipSkipsOccupiedSlot() {
        val (timeline, videoId, _) = baseTimeline()
        val first = clip()
        val second = clip().copy(startMs = 1_000L)
        val withClips = TimelineEditor.addClip(
            TimelineEditor.addClip(timeline, videoId, first),
            videoId,
            second
        )

        val duplicated = TimelineEditor.duplicateClip(withClips, first.id)
        val copy = duplicated.tracks.first { it.id == videoId }
            .clips.first { it.id != first.id && it.id != second.id }

        assertEquals(2_000L, copy.startMs)
    }

    @Test
    fun trimUsesSpeedAwareSourceDuration() {
        val (timeline, videoId, _) = baseTimeline()
        val source = Clip(
            startMs = 0L,
            durationMs = 1_000L,
            sourceDurationMs = 2_000L,
            speed = SpeedConfig(speed = 2f)
        )
        val withClip = TimelineEditor.addClip(timeline, videoId, source)
        val trimmed = TimelineEditor.trimClip(withClip, source.id, 0L, 500L)
        val result = trimmed.tracks.first { it.id == videoId }.clips.single()

        assertEquals(500L, result.durationMs)
        assertEquals(1_000L, result.sourceDurationMs)
    }
}
