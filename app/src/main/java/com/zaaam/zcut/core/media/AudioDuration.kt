package com.zaaam.zcut.core.media

import android.media.MediaMetadataRetriever

/**
 * Reads the duration of a local audio file. Used for recordings and TTS output,
 * where the duration is not known until the file exists.
 */
object AudioDuration {

    fun of(path: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
