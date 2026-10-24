package com.zaaam.zcut.core.util

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectorySizeTest {

    private fun tempDir(): File = Files.createTempDirectory("zcut-size").toFile()

    @Test
    fun missingDirectoryCountsAsZero() {
        assertEquals(0L, DirectorySize.of(File(tempDir(), "does-not-exist")))
    }

    @Test
    fun sumsNestedFiles() {
        val root = tempDir()
        File(root, "a").apply { parentFile?.mkdirs() }.writeBytes(ByteArray(10))
        File(root, "nested").mkdirs()
        File(root, "nested/b.bin").writeBytes(ByteArray(25))
        File(root, "nested/deeper").mkdirs()
        File(root, "nested/deeper/c.bin").writeBytes(ByteArray(5))

        assertEquals(40L, DirectorySize.of(root))
    }

    @Test
    fun singleFileReturnsItsLength() {
        val file = File(tempDir(), "one.bin")
        file.writeBytes(ByteArray(7))

        assertEquals(7L, DirectorySize.of(file))
    }

    @Test
    fun doesNotFollowSymlinkedDirectories() {
        val root = tempDir()
        val outside = tempDir()
        File(outside, "big.bin").writeBytes(ByteArray(100))
        File(root, "real.bin").writeBytes(ByteArray(5))
        Files.createSymbolicLink(File(root, "link").toPath(), outside.toPath())

        assertEquals(5L, DirectorySize.of(root))
    }
}
