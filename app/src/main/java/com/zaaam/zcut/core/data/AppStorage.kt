package com.zaaam.zcut.core.data

import com.zaaam.zcut.core.util.DirectorySize
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device storage accounting for the Profile/Settings screens.
 *
 * "Workspace" covers the app's private files (drafts live there); clearing the
 * cache only empties [cacheDir], so drafts and preferences always survive.
 */
class AppStorage(
    private val filesDir: File,
    private val cacheDir: File
) {

    suspend fun usedBytes(): Long = withContext(Dispatchers.IO) { currentSize() }

    /**
     * Empties the cache and returns the size afterwards.
     *
     * Symlinks are deleted, never followed: a link inside the cache cannot reach
     * the draft store or any other file outside [cacheDir]. Entries that cannot
     * be removed are left in place and simply still count towards the size.
     */
    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val root = cacheDir.toPath()
        if (Files.isDirectory(root)) {
            try {
                Files.walkFileTree(
                    root,
                    object : SimpleFileVisitor<Path>() {
                        override fun visitFile(
                            file: Path,
                            attrs: BasicFileAttributes
                        ): FileVisitResult {
                            runCatching { Files.deleteIfExists(file) }
                            return FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(
                            dir: Path,
                            exc: IOException?
                        ): FileVisitResult {
                            // Only remove a directory that was fully visited.
                            if (exc == null && dir != root) {
                                runCatching { Files.deleteIfExists(dir) }
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(
                            file: Path,
                            exc: IOException
                        ): FileVisitResult = FileVisitResult.CONTINUE
                    }
                )
            } catch (error: IOException) {
                // Partial clear is fine; the reported size tells the truth.
            }
        }
        currentSize()
    }

    private fun currentSize(): Long = DirectorySize.of(filesDir) + DirectorySize.of(cacheDir)
}
