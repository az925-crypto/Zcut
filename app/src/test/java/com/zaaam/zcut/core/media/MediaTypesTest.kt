package com.zaaam.zcut.core.media

import com.zaaam.zcut.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTypesTest {

    @Test
    fun mimePrefixesMapToKinds() {
        assertEquals(MediaKind.VIDEO, MediaTypes.kindFor("video/mp4"))
        assertEquals(MediaKind.IMAGE, MediaTypes.kindFor("image/jpeg"))
        assertEquals(MediaKind.AUDIO, MediaTypes.kindFor("audio/mpeg"))
        assertEquals(MediaKind.GIF, MediaTypes.kindFor("image/gif"))
    }

    @Test
    fun mimeParametersAndCasingAreIgnored() {
        assertEquals(MediaKind.VIDEO, MediaTypes.kindFor("Video/MP4; charset=binary"))
        assertEquals(MediaKind.IMAGE, MediaTypes.kindFor("  IMAGE/PNG  "))
    }

    @Test
    fun octetStreamFallsBackToTheFileExtension() {
        assertEquals(
            MediaKind.VIDEO,
            MediaTypes.kindFor("application/octet-stream", "holiday.MOV")
        )
        assertEquals(
            MediaKind.IMAGE,
            MediaTypes.kindFor("application/octet-stream", "photo.HEIC")
        )
        assertEquals(
            MediaKind.AUDIO,
            MediaTypes.kindFor(null, "voice memo.m4a")
        )
        assertEquals(
            MediaKind.GIF,
            MediaTypes.kindFor(null, "loop.gif")
        )
    }

    @Test
    fun unsupportedMediaIsRejectedInsteadOfGuessed() {
        assertNull(MediaTypes.kindFor("application/pdf", "document.pdf"))
        assertNull(MediaTypes.kindFor(null, null))
        assertNull(MediaTypes.kindFor(null, "no-extension"))
        assertFalse(MediaTypes.isSupported("text/plain", "notes.txt"))
    }

    @Test
    fun extensionForPrefersMimeThenNameThenSafeFallback() {
        assertEquals("jpg", MediaTypes.extensionFor("image/jpeg", "anything"))
        assertEquals("mp4", MediaTypes.extensionFor(null, "folder/clip.MP4"))
        assertEquals("m4a", MediaTypes.extensionFor("application/octet-stream", "clip.m4a"))
        assertEquals("bin", MediaTypes.extensionFor(null, "no-extension"))
    }

    @Test
    fun sizeLimitAcceptsZeroAndCapsAtTwoGigabytes() {
        assertTrue(MediaTypes.isWithinSizeLimit(0L))
        assertTrue(MediaTypes.isWithinSizeLimit(MediaTypes.MAX_IMPORT_BYTES))
        assertFalse(MediaTypes.isWithinSizeLimit(MediaTypes.MAX_IMPORT_BYTES + 1L))
        assertFalse(MediaTypes.isWithinSizeLimit(-1L))
    }

    @Test
    fun spaceCheckReservesHeadroomEvenWhenSizeIsUnknown() {
        val headroom = MediaTypes.FREE_SPACE_HEADROOM_BYTES

        assertTrue(MediaTypes.hasRoomFor(headroom, null))
        assertFalse(MediaTypes.hasRoomFor(headroom - 1L, null))
        assertTrue(MediaTypes.hasRoomFor(headroom + 100L, 100L))
        assertFalse(MediaTypes.hasRoomFor(headroom + 99L, 100L))
        // A provider that reports a negative size must not bypass the headroom.
        assertTrue(MediaTypes.hasRoomFor(headroom, -100L))
    }
}
