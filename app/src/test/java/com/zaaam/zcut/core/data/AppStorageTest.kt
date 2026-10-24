package com.zaaam.zcut.core.data

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStorageTest {

    private fun tempDir(): File = Files.createTempDirectory("zcut-storage").toFile()

    @Test
    fun clearCacheKeepsDraftsAndWipesCache() = runTest {
        val files = tempDir()
        val cache = tempDir()
        val draft = File(files, "datastore/zcut_projects.preferences_pb").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(64))
        }
        val cached = File(cache, "thumb.png").apply { writeBytes(ByteArray(32)) }

        val storage = AppStorage(filesDir = files, cacheDir = cache)

        assertEquals(96L, storage.usedBytes())
        assertEquals(64L, storage.clearCache())

        assertTrue("draft data must survive cache clearing", draft.exists())
        assertFalse("cache entries must be removed", cached.exists())
    }

    @Test
    fun missingDirectoriesDoNotThrow() = runTest {
        val root = tempDir()
        val storage = AppStorage(
            filesDir = File(root, "missing-files"),
            cacheDir = File(root, "missing-cache")
        )

        assertEquals(0L, storage.usedBytes())
        assertEquals(0L, storage.clearCache())
    }

    @Test
    fun clearCacheDeletesSymlinksWithoutFollowingThem() = runTest {
        val files = tempDir()
        val cache = tempDir()
        val draft = File(files, "draft.bin").apply { writeBytes(ByteArray(16)) }
        val link = File(cache, "escape").toPath()
        Files.createSymbolicLink(link, draft.toPath())

        val storage = AppStorage(filesDir = files, cacheDir = cache)

        assertEquals(16L, storage.usedBytes())
        assertEquals(16L, storage.clearCache())

        assertTrue("symlink target must survive", draft.exists())
        assertFalse("the link itself must be removed", Files.exists(link, LinkOption.NOFOLLOW_LINKS))
    }
}
