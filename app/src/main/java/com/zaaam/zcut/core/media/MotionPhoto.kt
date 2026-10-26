package com.zaaam.zcut.core.media

import android.media.MediaMetadataRetriever
import com.zaaam.zcut.core.domain.MotionPhotoIndex
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Best-effort split of a motion photo into its still and its appended video.
 *
 * The still is the original image; [extractVideo] copies the MP4 tail into the
 * app workspace and only returns an asset when Android can actually read a
 * video track from it. A plain still therefore reports "no motion video" instead
 * of producing a broken draft.
 */
object MotionPhoto {

    /** Motion photos are small; refuse to buffer something pathological. */
    private const val MAX_SOURCE_BYTES = 64L * 1024 * 1024

    suspend fun extractVideo(source: File, output: File): MediaAsset? =
        withContext(Dispatchers.IO) {
            try {
                if (!source.isFile) return@withContext null
                if (source.length() <= 0L || source.length() > MAX_SOURCE_BYTES) {
                    return@withContext null
                }
                val bytes = source.readBytes()
                val start = MotionPhotoIndex.videoStart(bytes) ?: return@withContext null
                output.parentFile?.mkdirs()
                output.outputStream().use { sink ->
                    sink.write(bytes, start, bytes.size - start)
                }
                probe(output) ?: run {
                    output.delete()
                    null
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                output.delete()
                null
            }
        }

    private fun probe(file: File): MediaAsset? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0
            if (durationMs <= 0L || width <= 0 || height <= 0) return null
            MediaAsset(
                displayName = file.name,
                mimeType = "video/mp4",
                kind = MediaKind.VIDEO,
                durationMs = durationMs,
                width = width,
                height = height,
                sizeBytes = file.length(),
                hasAudio = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes",
                localPath = file.absolutePath,
                rotationDegrees = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0
            )
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
