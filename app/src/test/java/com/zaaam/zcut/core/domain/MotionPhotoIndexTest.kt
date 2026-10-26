package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPhotoIndexTest {

    @Test
    fun findsAnAppendedMp4Box() {
        val image = ByteArray(20) { 0x11 }
        val box = mp4Header(totalSize = 24)
        val bytes = image + box

        assertEquals(image.size, MotionPhotoIndex.videoStart(bytes))
    }

    @Test
    fun ignoresTheLiteralFtypWithoutAPlausibleBox() {
        val bytes = "not a motion photo ftyp".toByteArray()

        assertNull(MotionPhotoIndex.videoStart(bytes))
    }

    @Test
    fun rejectsABoxThatClaimsMoreBytesThanExist() {
        val bytes = ByteArray(12)
        writeInt(bytes, 0, 100)
        writeFtyp(bytes, 4)

        assertNull(MotionPhotoIndex.videoStart(bytes))
    }

    @Test
    fun findsTheLastValidBox() {
        val first = ByteArray(16) { 0x22 }
        val bytes = first + mp4Header(totalSize = 16) + mp4Header(totalSize = 16)

        assertEquals(first.size + 16, MotionPhotoIndex.videoStart(bytes))
    }

    private fun mp4Header(totalSize: Int): ByteArray {
        val box = ByteArray(totalSize)
        writeInt(box, 0, totalSize)
        writeFtyp(box, 4)
        return box
    }

    private fun writeFtyp(bytes: ByteArray, offset: Int) {
        bytes[offset] = 'f'.code.toByte()
        bytes[offset + 1] = 't'.code.toByte()
        bytes[offset + 2] = 'y'.code.toByte()
        bytes[offset + 3] = 'p'.code.toByte()
    }

    private fun writeInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }
}
