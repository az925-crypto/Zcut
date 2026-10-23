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
