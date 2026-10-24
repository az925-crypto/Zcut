package com.zaaam.zcut.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAssetTest {

    private fun asset(
        id: String = "asset-1",
        uri: String = "",
        localPath: String? = null,
        kind: MediaKind = MediaKind.VIDEO
    ): MediaAsset = MediaAsset(
        id = id,
        uri = uri,
        displayName = "clip.mp4",
        kind = kind,
        localPath = localPath
    )

    @Test
    fun usableRequiresAnIdAndSomeSource() {
        assertTrue(asset(uri = "content://media/1").isUsable)
        assertTrue(asset(localPath = "/data/files/media/1.mp4").isUsable)
        assertFalse(asset().isUsable)
        assertFalse(asset(id = "", uri = "content://media/1").isUsable)
        assertFalse(asset(id = "  ", uri = "content://media/1").isUsable)
        assertFalse(asset(localPath = "  ").isUsable)
    }

    @Test
    fun sourcePathPrefersThePrivateCopy() {
        assertEquals(
            "/data/files/media/1.mp4",
            asset(uri = "content://media/1", localPath = "/data/files/media/1.mp4").sourcePath
        )
        assertEquals(
            "content://media/1",
            asset(uri = "content://media/1", localPath = " ").sourcePath
        )
        assertNull(asset().sourcePath)
    }

    @Test
    fun visualKindsExcludeAudio() {
        assertTrue(asset(kind = MediaKind.VIDEO).isVisual)
        assertTrue(asset(kind = MediaKind.IMAGE).isVisual)
        assertTrue(asset(kind = MediaKind.GIF).isVisual)
        assertTrue(asset(kind = MediaKind.LIVE_PHOTO).isVisual)
        assertFalse(asset(kind = MediaKind.AUDIO).isVisual)
    }
}
