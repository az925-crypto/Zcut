package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftBackupTest {

    private fun tempDir(): File =
        Files.createTempDirectory("zcut-backup").toFile().apply { deleteOnExit() }

    private fun projectWithMedia(root: File): Pair<com.zaaam.zcut.core.model.Project, File> {
        val source = File(root, "clip.mp4").apply {
            writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
        }
        val asset = MediaAsset(
            displayName = "clip.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            durationMs = 2_000L,
            width = 1_080,
            height = 1_920,
            sizeBytes = source.length(),
            localPath = source.absolutePath
        )
        return ProjectFactory.fromAssets("Ad test", listOf(asset)) to source
    }

    @Test
    fun exportThenImportCopiesMediaAndGivesTheDraftNewIds() = runBlocking {
        val root = tempDir()
        val (project, source) = projectWithMedia(root)
        val archive = File(root, "backup.zcut.zip")

        assertTrue(DraftBackup.export(project, FileOutputStream(archive)))
        assertTrue(archive.length() > 0L)

        val imported = DraftBackup.import(
            FileInputStream(archive),
            File(root, "restore").apply { mkdirs() }
        )

        assertNotNull(imported)
        imported!!
        assertEquals("Ad test (impor)", imported.name)
        assertNotEquals(project.id, imported.id)
        assertNull(imported.trashedAtMs)
        assertEquals(1, imported.assets.size)

        val restored = imported.assets.first()
        assertNotEquals(project.assets.first().id, restored.id)
        val restoredFile = File(restored.localPath!!)
        assertTrue(restoredFile.isFile)
        assertNotEquals(source.absolutePath, restoredFile.absolutePath)
        assertArrayEquals(source.readBytes(), restoredFile.readBytes())

        val clip = imported.timeline.tracks
            .flatMap { it.clips }
            .first { it.assetId != null }
        assertEquals(restored.id, clip.assetId)
        assertFalse(imported.assets.any { it.id == project.assets.first().id })
    }

    @Test
    fun aCorruptArchiveIsRejectedWithoutThrowing() = runBlocking {
        val root = tempDir()
        val broken = File(root, "broken.zcut.zip").apply { writeText("not a zip") }

        val imported = DraftBackup.import(
            FileInputStream(broken),
            File(root, "restore").apply { mkdirs() }
        )

        assertNull(imported)
    }

    @Test
    fun aZipWithoutTheProjectEntryIsRejected() = runBlocking {
        val root = tempDir()
        val archive = File(root, "empty.zcut.zip")
        java.util.zip.ZipOutputStream(FileOutputStream(archive)).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("media/orphan.bin"))
            zip.write(byteArrayOf(9, 9, 9))
            zip.closeEntry()
        }

        val imported = DraftBackup.import(
            FileInputStream(archive),
            File(root, "restore").apply { mkdirs() }
        )

        assertNull(imported)
    }

    @Test
    fun archiveNamesNeverCarryPathSeparators() {
        val root = tempDir()
        val (project, _) = projectWithMedia(root)
        val hostile = project.copy(name = "../../etc/passwd")

        val name = DraftBackup.fileNameFor(hostile)

        assertFalse(name.contains("/"))
        assertFalse(name.contains(".."))
        assertTrue(name.endsWith(DraftBackup.EXTENSION))
    }
}
