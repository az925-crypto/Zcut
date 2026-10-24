package com.zaaam.zcut.core.util

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Recursive size of a directory tree. Missing roots count as empty.
 *
 * Uses NIO without FOLLOW_LINKS: symlinks are not traversed, so a link planted
 * in a cache directory can neither inflate the count nor create a cycle.
 * Unreadable subtrees are skipped instead of failing the whole measurement.
 */
object DirectorySize {

    fun of(root: File): Long {
        if (!root.exists()) return 0L
        if (root.isFile) return root.length()

        var total = 0L
        try {
            Files.walkFileTree(
                root.toPath(),
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(
                        file: Path,
                        attrs: BasicFileAttributes
                    ): FileVisitResult {
                        if (attrs.isRegularFile) total += attrs.size()
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                }
            )
        } catch (error: IOException) {
            // Report what could be measured rather than crashing the screen.
        }
        return total
    }
}
