package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.domain.DraftRemap
import com.zaaam.zcut.core.model.Project
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local draft backup (F20 P1): one `.zcut.zip` holding `project.json` plus the
 * media files the draft references.
 *
 * No server is involved; the archive is written to a stream the caller picked
 * (SAF) and read back the same way. Entries are bounded so a hostile or damaged
 * archive cannot exhaust memory or storage.
 */
object DraftBackup {

    const val MIME_TYPE = "application/zip"
    const val EXTENSION = ".zcut.zip"

    const val MAX_PROJECT_BYTES = 8L * 1024 * 1024
    const val MAX_MEDIA_ENTRY_BYTES = 512L * 1024 * 1024
    const val MAX_MEDIA_TOTAL_BYTES = 2L * 1024 * 1024 * 1024

    private const val PROJECT_ENTRY = "project.json"
    private const val MEDIA_PREFIX = "media/"
    private const val BUFFER_BYTES = 64 * 1024

    /** Safe, human-readable archive name derived only from the draft name. */
    fun fileNameFor(project: Project): String = fileNameFor(project.name)

    fun fileNameFor(draftName: String): String {
        val base = draftName.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_', '.')
            .take(40)
            .ifBlank { "draft" }
        return base + EXTENSION
    }

    suspend fun export(project: Project, output: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            try {
                ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                    zip.putNextEntry(ZipEntry(PROJECT_ENTRY))
                    zip.write(
                        ProjectCodec.encode(ProjectDocument(projects = listOf(project)))
                            .toByteArray(Charsets.UTF_8)
                    )
                    zip.closeEntry()
                    project.assets.forEach { asset ->
                        val file = asset.localPath?.let(::File) ?: return@forEach
                        if (!file.isFile || file.length() <= 0L) return@forEach
                        if (file.length() > MAX_MEDIA_ENTRY_BYTES) return@forEach
                        zip.putNextEntry(
                            ZipEntry(MEDIA_PREFIX + asset.id + "_" + safeName(file.name))
                        )
                        file.inputStream().use { it.copyTo(zip, BUFFER_BYTES) }
                        zip.closeEntry()
                    }
                }
                true
            } catch (error: Exception) {
                false
            }
        }

    /**
     * Reads an archive, unpacks its media next to the app's own media, and
     * returns a fresh draft (new ids, no trash flag). Null on any malformed or
     * oversized entry, and every partially written file is removed.
     */
    suspend fun import(input: InputStream, mediaDir: File): Project? =
        withContext(Dispatchers.IO) {
            var source: Project? = null
            val copied = mutableMapOf<String, String>()
            val written = mutableListOf<File>()
            var total = 0L

            fun abort(): Project? {
                written.forEach { runCatching { it.delete() } }
                return null
            }

            try {
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory) {
                            when {
                                entry.name == PROJECT_ENTRY -> {
                                    val bytes = readBounded(zip, MAX_PROJECT_BYTES)
                                        ?: return@withContext abort()
                                    source = ProjectCodec
                                        .decode(String(bytes, Charsets.UTF_8))
                                        .projects
                                        .firstOrNull()
                                }

                                entry.name.startsWith(MEDIA_PREFIX) -> {
                                    val rest = entry.name.removePrefix(MEDIA_PREFIX)
                                    val assetId = rest.substringBefore('_')
                                    if (assetId.isNotBlank()) {
                                        val target = uniqueFile(
                                            mediaDir,
                                            assetId + "_" + safeName(
                                                rest.substringAfter('_', "media")
                                            )
                                        )
                                        val size = writeBounded(
                                            zip,
                                            target,
                                            MAX_MEDIA_ENTRY_BYTES
                                        )
                                        if (size < 0L) {
                                            target.delete()
                                            return@withContext abort()
                                        }
                                        written += target
                                        total += size
                                        if (total > MAX_MEDIA_TOTAL_BYTES) {
                                            return@withContext abort()
                                        }
                                        copied[assetId] = target.absolutePath
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } catch (error: Exception) {
                return@withContext abort()
            }

            val project = source ?: return@withContext abort()
            DraftRemap.imported(project, copied)
        }

    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** Bytes written, or -1 when the entry grows past [maxBytes]. */
    private fun writeBounded(input: InputStream, target: File, maxBytes: Long): Long {
        target.parentFile?.mkdirs()
        var total = 0L
        val buffer = ByteArray(BUFFER_BYTES)
        target.outputStream().use { sink ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return -1L
                sink.write(buffer, 0, read)
            }
        }
        return total
    }

    private fun uniqueFile(dir: File, name: String): File {
        dir.mkdirs()
        var candidate = File(dir, name)
        var index = 2
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isBlank()) "$stem-$index" else "$stem-$index.$ext")
            index++
        }
        return candidate
    }

    private fun safeName(raw: String): String = raw
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_', '.')
        .take(60)
        .ifBlank { "media" }
}
