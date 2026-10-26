package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.StickerConfig
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import com.zaaam.zcut.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerEditorTest {

    private fun emptyTimeline(): Timeline = Timeline(
        tracks = listOf(
            Track(type = TrackType.VIDEO, clips = emptyList()),
            Track(type = TrackType.STICKER, clips = emptyList())
        )
    )

    @Test
    fun addStickerPlacesAClipOnTheStickerTrack() {
        val timeline = StickerEditor.addSticker(emptyTimeline(), "emoji-heart", atMs = 0L)

        val clip = timeline.tracks.first { it.type == TrackType.STICKER }.clips.single()
        assertEquals(ClipType.STICKER, clip.type)
        assertEquals("emoji-heart", clip.sticker?.stickerId)
        assertTrue(StickerEditor.hasStickers(timeline))
    }

    @Test
    fun addStickerPushesRightWhenTheSlotIsTaken() {
        val once = StickerEditor.addSticker(emptyTimeline(), "trend-lol", atMs = 0L, durationMs = 2_000L)
        val twice = StickerEditor.addSticker(once, "emoji-star", atMs = 0L, durationMs = 2_000L)

        val starts = twice.tracks.first { it.type == TrackType.STICKER }
            .clips.map { it.startMs }
            .sorted()

        assertEquals(listOf(0L, 2_000L), starts)
    }

    @Test
    fun addStickerResolvesAnUnknownGlyphKey() {
        val timeline = StickerEditor.addSticker(emptyTimeline(), "not-a-sticker", atMs = 0L)

        val clip = timeline.tracks.first { it.type == TrackType.STICKER }.clips.single()
        assertNotNull(StickerCatalog.find(clip.sticker!!.stickerId))
    }

    @Test
    fun setStickerClampsTransformAndRejectsOtherClipTypes() {
        val added = StickerEditor.addSticker(emptyTimeline(), "shape-star", atMs = 0L)
        val sticker = added.tracks.first { it.type == TrackType.STICKER }.clips.single()

        val hostile = Transform(
            positionX = 9_000f,
            positionY = Float.NaN,
            scale = 0f,
            rotationDegrees = -720f
        )
        val updated = StickerEditor.setSticker(
            timeline = added,
            clipId = sticker.id,
            stickerId = "shape-star",
            transform = hostile,
            opacity = 4f
        )
        val result = updated.clip(sticker.id)!!

        assertEquals(100f, result.transform.positionX, 0.0001f)
        assertEquals(0f, result.transform.positionY, 0.0001f)
        assertEquals(0.1f, result.transform.scale, 0.0001f)
        assertEquals(-180f, result.transform.rotationDegrees, 0.0001f)
        assertEquals(1f, result.opacity, 0.0001f)

        // A video clip id must not be rewritten by a sticker edit.
        val other = Clip(type = ClipType.VIDEO, startMs = 0L, durationMs = 1_000L)
        val mixed = Timeline(
            tracks = listOf(Track(type = TrackType.VIDEO, clips = listOf(other)))
        )
        val untouched = StickerEditor.setSticker(mixed, other.id, "emoji-smile", Transform(), 1f)
        assertEquals(mixed, untouched)
    }

    @Test
    fun removeStickerDeletesOnlyStickerClips() {
        val added = StickerEditor.addSticker(emptyTimeline(), "trend-fire", atMs = 0L)
        val sticker = added.tracks.first { it.type == TrackType.STICKER }.clips.single()

        val removed = StickerEditor.removeSticker(added, sticker.id)

        assertTrue(removed.clip(sticker.id) == null)
        assertFalse(StickerEditor.hasStickers(removed))
    }

    @Test
    fun sanitizeClampsADecodedTransformAndKeepsTheClip() {
        val hostile = Clip(
            type = ClipType.STICKER,
            startMs = 0L,
            durationMs = 500L,
            transform = Transform(positionX = 1e9f, scale = -3f),
            sticker = StickerConfig("unknown-key")
        )
        val timeline = Timeline(
            tracks = listOf(Track(type = TrackType.STICKER, clips = listOf(hostile)))
        )

        val clip = StickerEditor.sanitize(timeline).tracks.single().clips.single()

        assertEquals(100f, clip.transform.positionX, 0.0001f)
        assertEquals(0.1f, clip.transform.scale, 0.0001f)
        assertNotNull(StickerCatalog.find(clip.sticker!!.stickerId))
    }

    @Test
    fun activeAtOnlyReturnsStickersCoveringThePlayhead() {
        val timeline = StickerEditor.addSticker(
            emptyTimeline(),
            "text-wow",
            atMs = 1_000L,
            durationMs = 1_000L
        )

        assertEquals(1, StickerEditor.activeAt(timeline, 1_500L).size)
        assertTrue(StickerEditor.activeAt(timeline, 2_000L).isEmpty())
        assertEquals("WOW", StickerEditor.label(StickerEditor.stickerClips(timeline).single()))
    }

    @Test
    fun catalogueCoversEveryCategory() {
        StickerCategory.entries.forEach { category ->
            assertTrue(
                "no sticker for $category",
                StickerCatalog.byCategory(category).isNotEmpty()
            )
        }
    }
}