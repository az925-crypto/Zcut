package com.zaaam.zcut.core.domain

/**
 * Finds the embedded MP4 inside a motion photo.
 *
 * A motion photo is a still (JPEG/HEIC) with an MP4 appended after the image
 * data. The shipped artifact splits it natively (`SplitLivePhoto`); Zcut scans
 * for the `ftyp` box instead of relying on a private API. Pure byte work so the
 * index can be unit-tested on the JVM; the extractor verifies the result with
 * `MediaMetadataRetriever` before anything is used.
 */
object MotionPhotoIndex {

    /** Offset of the MP4's first box (`ftyp`), or null when there is none. */
    fun videoStart(bytes: ByteArray): Int? {
        if (bytes.size < 12) return null
        var index = bytes.size - 4
        while (index >= 4) {
            if (bytes[index] == 'f'.code.toByte() &&
                bytes[index + 1] == 't'.code.toByte() &&
                bytes[index + 2] == 'y'.code.toByte() &&
                bytes[index + 3] == 'p'.code.toByte()
            ) {
                val start = index - 4
                val boxSize = readInt(bytes, start)
                // A plausible first box: at least its own header and fully
                // present. This rejects the literal "ftyp" inside metadata.
                if (boxSize >= MIN_BOX_BYTES && start + boxSize <= bytes.size) {
                    return start
                }
            }
            index--
        }
        return null
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private const val MIN_BOX_BYTES = 8
}
