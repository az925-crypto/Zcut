package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cut-same mapping, pure JVM. The cases pin the two rules the UI promises:
 * centre-crop a longer source, keep a shorter one whole, and never invent a clip
 * for a slot that got no media.
 */
class TemplateEditorTest {

    private fun asset(
        id: String,
        kind: MediaKind = MediaKind.VIDEO,
        durationMs: Long = 0L
    ): MediaAsset = MediaAsset(
        id = id,
        uri = "content://test/$id",
        displayName = "$id.mp4",
        mimeType = if (kind == MediaKind.AUDIO) "audio/mp4" else "video/mp4",
        kind = kind,
        durationMs = durationMs
    )

    private fun template(slots: List<Long>, bpm: Int? = 100) = VideoTemplate(
        id = "test-pack",
        name = "Test Pack",
        category = TemplateCategory.FOR_YOU,
        artKey = "night",
        ratio = AspectRatio.PORTRAIT_9_16,
        slots = slots.map(::TemplateSlot),
        bpm = bpm
    )

    @Test
    fun longerSourceIsCentreCropped() {
        val fit = TemplateEditor.fitToSlot(sourceDurationMs = 6_000L, slotDurationMs = 2_000L)

        assertEquals(2_000L, fit.sourceStartMs)
        assertEquals(2_000L, fit.durationMs)
    }

    @Test
    fun shorterSourceIsUsedWhole() {
        val fit = TemplateEditor.fitToSlot(sourceDurationMs = 1_500L, slotDurationMs = 2_000L)

        assertEquals(0L, fit.sourceStartMs)
        assertEquals(1_500L, fit.durationMs)
    }

    @Test
    fun unknownDurationFallsBackToTheSlotLength() {
        val fit = TemplateEditor.fitToSlot(sourceDurationMs = 0L, slotDurationMs = 2_000L)

        assertEquals(0L, fit.sourceStartMs)
        assertEquals(2_000L, fit.durationMs)
    }

    @Test
    fun aDegenerateSlotStillProducesAFittableClip() {
        val fit = TemplateEditor.fitToSlot(sourceDurationMs = 5_000L, slotDurationMs = 0L)

        assertEquals(1L, fit.durationMs)
        assertTrue(fit.sourceStartMs >= 0L)
    }

    @Test
    fun applyFillsSlotsAtCumulativeStartsWithCentreCrop() {
        val assets = listOf(
            asset("a", MediaKind.VIDEO, 6_000L),
            asset("b", MediaKind.IMAGE)
        )

        val project = TemplateEditor.apply(
            template = template(listOf(2_000L, 2_000L)),
            assets = assets,
            name = "From template"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertEquals(2, clips.size)

        assertEquals(0L, clips[0].startMs)
        assertEquals(2_000L, clips[0].durationMs)
        assertEquals(2_000L, clips[0].sourceStartMs)
        assertEquals(ClipType.VIDEO, clips[0].type)

        assertEquals(2_000L, clips[1].startMs)
        assertEquals(MediaAppender.DEFAULT_IMAGE_DURATION_MS, clips[1].durationMs)
        assertEquals(0L, clips[1].sourceStartMs)
        assertEquals(ClipType.IMAGE, clips[1].type)

        assertEquals(assets.map { it.id }, project.assets.map { it.id })
        assertEquals("From template", project.name)
    }

    @Test
    fun beatGridFollowsTheTemplateTempoAndEnablesSnap() {
        val project = TemplateEditor.apply(
            template = template(listOf(2_000L, 2_000L), bpm = 120),
            assets = listOf(asset("a", MediaKind.VIDEO, 2_000L)),
            name = "Paced"
        )

        assertEquals(0L, project.settings.beatGridMs.first())
        assertTrue(project.settings.beatGridMs.size > 1)
        assertTrue(project.settings.snapToBeat)
    }

    @Test
    fun aTemplateWithoutTempoLeavesSnapOff() {
        val project = TemplateEditor.apply(
            template = template(listOf(2_000L), bpm = null),
            assets = listOf(asset("a", MediaKind.VIDEO, 2_000L)),
            name = "Still"
        )

        assertTrue(project.settings.beatGridMs.isEmpty())
        assertTrue(!project.settings.snapToBeat)
    }

    @Test
    fun noUsableMediaLeavesTheDraftEmpty() {
        val project = TemplateEditor.apply(
            template = template(listOf(2_000L, 2_000L)),
            assets = listOf(asset("a", MediaKind.AUDIO, 5_000L)),
            name = "Empty"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertTrue("audio cannot fill a slot", clips.isEmpty())
        assertTrue(project.assets.isEmpty())
    }

    @Test
    fun extraMediaBeyondTheSlotCountIsIgnored() {
        val project = TemplateEditor.apply(
            template = template(listOf(2_000L)),
            assets = listOf(
                asset("a", MediaKind.VIDEO, 2_000L),
                asset("b", MediaKind.VIDEO, 2_000L),
                asset("c", MediaKind.VIDEO, 2_000L)
            ),
            name = "Trimmed"
        )

        val clips = project.timeline.tracks.first { it.type == TrackType.VIDEO }.clips
        assertEquals(1, clips.size)
        assertEquals(1, project.assets.size)
    }

    @Test
    fun blankNameFallsBackToTheTemplateName() {
        val project = TemplateEditor.apply(
            template = template(listOf(2_000L)),
            assets = listOf(asset("a", MediaKind.VIDEO, 2_000L)),
            name = "   "
        )

        assertEquals("Test Pack", project.name)
    }
}