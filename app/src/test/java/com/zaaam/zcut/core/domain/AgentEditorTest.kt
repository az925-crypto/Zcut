package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentEditorTest {

    private fun video(startMs: Long, durationMs: Long, id: String = "v$startMs"): Clip =
        Clip(
            id = id,
            type = ClipType.VIDEO,
            startMs = startMs,
            durationMs = durationMs,
            sourceStartMs = 0L,
            sourceDurationMs = durationMs
        )

    private fun text(startMs: Long, durationMs: Long, id: String = "t$startMs"): Clip =
        Clip(
            id = id,
            type = ClipType.TEXT,
            startMs = startMs,
            durationMs = durationMs,
            sourceStartMs = 0L,
            sourceDurationMs = durationMs,
            text = TextConfig(text = id)
        )

    private fun project(
        videoClips: List<Clip>,
        textClips: List<Clip> = emptyList()
    ): Project = Project(
        name = "Agent",
        timeline = Timeline(
            tracks = listOf(
                Track(type = TrackType.VIDEO, clips = videoClips),
                Track(type = TrackType.TEXT, clips = textClips)
            )
        )
    )

    private fun Project.videoClips(): List<Clip> =
        timeline.tracks.first { it.type == TrackType.VIDEO }.clips

    private fun Project.textClips(): List<Clip> =
        timeline.tracks.first { it.type == TrackType.TEXT }.clips

    @Test
    fun tightenTrimsTheHeadAndRipplesEveryTrackLeft() {
        val trimmed = AgentEditor.tightenIntro(
            project(
                videoClips = listOf(video(0L, 5_000L), video(5_000L, 5_000L)),
                textClips = listOf(text(5_000L, 3_000L))
            )
        )

        val video = trimmed.videoClips()
        // The first clip loses 800 ms of its head ...
        assertEquals(0L, video[0].startMs)
        assertEquals(4_200L, video[0].durationMs)
        // ... and the second follows, closing the gap the trim left behind.
        assertEquals(4_200L, video[1].startMs)
        assertEquals(9_200L, video[1].endMs)

        assertEquals(4_200L, trimmed.textClips().single().startMs)
        assertEquals(7_200L, trimmed.textClips().single().endMs)
        assertEquals(9_200L, trimmed.timeline.durationMs)
    }

    @Test
    fun theTrimmedClipKeepsItsSourceWindowShiftedByTheHead() {
        val trimmed = AgentEditor.tightenIntro(
            project(videoClips = listOf(video(0L, 5_000L)))
        )

        val clip = trimmed.videoClips().single()
        assertEquals(800L, clip.sourceStartMs)
        assertEquals(4_200L, clip.sourceDurationMs)
    }

    @Test
    fun aDraftWithoutVideoIsLeftAlone() {
        val original = project(videoClips = emptyList(), textClips = listOf(text(0L, 2_000L)))

        assertEquals(original, AgentEditor.tightenIntro(original))
    }

    @Test
    fun anEmptyVideoTrackIsLeftAlone() {
        val original = project(videoClips = emptyList())

        assertEquals(original, AgentEditor.tightenIntro(original))
    }

    @Test
    fun theHeadIsClampedSoAClipNeverDisappears() {
        val trimmed = AgentEditor.tightenIntro(
            project(videoClips = listOf(video(0L, 500L)))
        )

        val clip = trimmed.videoClips().single()
        assertEquals(1L, clip.durationMs)
        assertEquals(499L, clip.sourceStartMs)
    }

    @Test
    fun aHeadSmallerThanTheDefaultIsHonoured() {
        val trimmed = AgentEditor.tightenIntro(
            project(videoClips = listOf(video(0L, 5_000L))),
            headMs = 250L
        )

        val clip = trimmed.videoClips().single()
        assertEquals(4_750L, clip.durationMs)
        assertEquals(0L, clip.startMs)
    }

    @Test
    fun aClipStartingAfterTheCutIsNotDraggedPastZero() {
        // The earliest video clip starts at 1 s, so the ripple must not push a
        // later clip to a negative start.
        val trimmed = AgentEditor.tightenIntro(
            project(
                videoClips = listOf(video(1_000L, 5_000L), video(6_000L, 5_000L))
            )
        )

        val video = trimmed.videoClips()
        assertEquals(1_000L, video[0].startMs)
        assertEquals(4_200L, video[0].durationMs)
        assertEquals(5_200L, video[1].startMs)
        assertTrue(video.all { it.startMs >= 0L })
    }
}