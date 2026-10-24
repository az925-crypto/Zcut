package com.zaaam.zcut.feature.editor

import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTimelineTest {

    @Test
    fun offsetsUseSixtyFourDpPerSecond() {
        assertEquals(0f, EditorTimeline.offsetDp(0L))
        assertEquals(64f, EditorTimeline.offsetDp(1_000L))
        assertEquals(32f, EditorTimeline.offsetDp(500L))
        assertEquals(0f, EditorTimeline.offsetDp(-500L))
    }

    @Test
    fun shortClipsKeepATappableMinimumWidth() {
        assertEquals(EditorTimeline.MIN_CLIP_WIDTH_DP, EditorTimeline.widthDp(0L))
        assertEquals(EditorTimeline.MIN_CLIP_WIDTH_DP, EditorTimeline.widthDp(100L))
        assertEquals(64f, EditorTimeline.widthDp(1_000L))
        assertEquals(EditorTimeline.MIN_CLIP_WIDTH_DP, EditorTimeline.widthDp(-1L))
    }

    @Test
    fun millisecondsRoundTripThroughDp() {
        assertEquals(0L, EditorTimeline.msAt(0f))
        assertEquals(1_000L, EditorTimeline.msAt(64f))
        assertEquals(2_500L, EditorTimeline.msAt(160f))
        assertEquals(0L, EditorTimeline.msAt(-10f))
    }

    @Test
    fun contentWidthNeverShrinksBelowTheViewport() {
        assertEquals(300f, EditorTimeline.contentWidthDp(1_000L, 300f))
        assertEquals(128f, EditorTimeline.contentWidthDp(2_000L, 100f))
    }

    @Test
    fun rulerLabelsAdaptToClipLength() {
        assertEquals(1, EditorTimeline.labelIntervalSeconds(0L))
        assertEquals(1, EditorTimeline.labelIntervalSeconds(30_000L))
        assertEquals(2, EditorTimeline.labelIntervalSeconds(30_001L))
        assertEquals(2, EditorTimeline.labelIntervalSeconds(90_000L))
        assertEquals(5, EditorTimeline.labelIntervalSeconds(90_001L))
    }

    @Test
    fun longTimelinesCapTheNumberOfRulerLabels() {
        val hour = 60L * 60L * 1_000L

        assertEquals(30, EditorTimeline.labelIntervalSeconds(hour))
        assertEquals(
            120,
            EditorTimeline.labelIntervalSeconds(4L * hour)
        )
        // Even an absurd duration cannot explode the label count.
        assertTrue(
            EditorTimeline.labelIntervalSeconds(24L * hour) >= 120
        )
    }

    @Test
    fun tracksAreOrderedVideoFirstAudioAfterText() {
        val tracks = listOf(
            Track(type = TrackType.STICKER),
            Track(type = TrackType.AUDIO),
            Track(type = TrackType.TEXT),
            Track(type = TrackType.VIDEO),
            Track(type = TrackType.EFFECT),
            Track(type = TrackType.CAPTION),
            Track(type = TrackType.OVERLAY)
        )

        assertEquals(
            listOf(
                TrackType.VIDEO,
                TrackType.OVERLAY,
                TrackType.TEXT,
                TrackType.CAPTION,
                TrackType.AUDIO,
                TrackType.EFFECT,
                TrackType.STICKER
            ),
            EditorTimeline.orderedTracks(tracks).map { it.type }
        )
    }

    @Test
    fun everyTrackTypeHasAShortLabel() {
        TrackType.entries.forEach { type ->
            assertEquals(true, EditorTimeline.label(type).isNotBlank())
        }
        assertEquals("V1", EditorTimeline.label(TrackType.VIDEO))
        assertEquals("A1", EditorTimeline.label(TrackType.AUDIO))
        assertEquals("CC", EditorTimeline.label(TrackType.CAPTION))
    }

    @Test
    fun canvasHeightGrowsWithTheLaneCount() {
        assertEquals(EditorTimeline.RULER_HEIGHT_DP, EditorTimeline.canvasHeightDp(0))
        assertEquals(
            EditorTimeline.RULER_HEIGHT_DP +
                EditorTimeline.LANE_HEIGHT_DP + EditorTimeline.LANE_GAP_DP,
            EditorTimeline.canvasHeightDp(1)
        )
    }
}
