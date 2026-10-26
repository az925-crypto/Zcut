package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.model.Track
import com.zaaam.zcut.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftRemapTest {

    private fun asset(id: String, path: String) = MediaAsset(
        id = id,
        displayName = "clip.mp4",
        mimeType = "video/mp4",
        kind = MediaKind.VIDEO,
        durationMs = 1_000L,
        localPath = path
    )

    @Test
    fun importedDraftGetsFreshIdsAndRewrittenPaths() {
        val source = Project(
            id = "old-project",
            name = "Holiday",
            assets = listOf(asset("old-asset", "/old/clip.mp4")),
            coverAssetId = "old-asset",
            timeline = Timeline(
                tracks = listOf(
                    Track(
                        type = TrackType.VIDEO,
                        clips = listOf(
                            Clip(
                                assetId = "old-asset",
                                type = ClipType.VIDEO,
                                startMs = 0L,
                                durationMs = 1_000L
                            )
                        )
                    )
                )
            )
        )

        val imported = DraftRemap.imported(
            source = source,
            newPaths = mapOf("old-asset" to "/new/clip.mp4"),
            idFor = { "new-$it" },
            nowMs = 42L,
            name = "Holiday (impor)"
        )

        assertEquals("new-old-project", imported.id)
        assertEquals("Holiday (impor)", imported.name)
        assertEquals(42L, imported.updatedAtMs)
        assertNull(imported.trashedAtMs)
        assertEquals("/new/clip.mp4", imported.assets.first().localPath)
        assertEquals("new-old-asset", imported.assets.first().id)
        assertEquals("new-old-asset", imported.coverAssetId)
        assertEquals("new-old-asset", imported.timeline.tracks.first().clips.first().assetId)
    }

    @Test
    fun anAssetWithoutAnUnpackedFileKeepsItsOldPath() {
        val source = Project(assets = listOf(asset("a1", "/old/clip.mp4")))

        val imported = DraftRemap.imported(source, emptyMap(), idFor = { "new-$it" })

        assertEquals("/old/clip.mp4", imported.assets.first().localPath)
    }

    @Test
    fun textClipsKeepTheirNullAssetId() {
        val source = Project(
            timeline = Timeline(
                tracks = listOf(
                    Track(
                        type = TrackType.TEXT,
                        clips = listOf(
                            Clip(type = ClipType.TEXT, startMs = 0L, durationMs = 500L)
                        )
                    )
                )
            )
        )

        val imported = DraftRemap.imported(source, emptyMap(), idFor = { "new-$it" })

        assertNull(imported.timeline.tracks.first().clips.first().assetId)
    }

    @Test
    fun importedNamesStayWithinTheLimit() {
        val longName = "x".repeat(200)

        val imported = DraftRemap.importedName(longName)

        assertEquals(DraftRemap.MAX_NAME_LENGTH, imported.length)
        assertEquals(DraftRemap.IMPORT_SUFFIX, imported.takeLast(DraftRemap.IMPORT_SUFFIX.length))
        assertEquals("Draft (impor)", DraftRemap.importedName("   "))
    }
}
