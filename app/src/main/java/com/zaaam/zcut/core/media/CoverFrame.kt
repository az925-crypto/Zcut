package com.zaaam.zcut.core.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

/**
 * Writes one frame of an exported video as a JPEG, used by the export screen's
 * "smart cover" toggle. Reading a frame is local and cheap; nothing is uploaded.
 */
object CoverFrame {

    private const val JPEG_QUALITY = 90

    fun write(videoPath: String, atMs: Long, outputPath: String): Boolean = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val frame = retriever.getFrameAtTime(
                atMs.coerceAtLeast(0L) * 1_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (frame == null) {
                false
            } else {
                File(outputPath).outputStream().use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                frame.recycle()
                true
            }
        } finally {
            retriever.release()
        }
    }.getOrDefault(false)
}
