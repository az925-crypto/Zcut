package com.zaaam.zcut.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Thumbnail decoding for the timeline and the media picker.
 *
 * Bounded [LruCache] plus sampled decodes keep a grid of large phone photos from
 * exhausting the heap. Thumbnails live in `cacheDir`, so a "Bersihkan cache"
 * can remove them; callers regenerate from the private copy when that happens.
 */
class MediaThumbnails(
    private val context: Context,
    private val cacheDir: File
) {

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(asset: MediaAsset, sizePx: Int = 320): Bitmap? =
        withContext(Dispatchers.IO) {
            val source = asset.thumbnailPath ?: asset.localPath ?: asset.uri
            if (source.isBlank()) return@withContext null
            // Size is part of the key: a 160dp timeline thumb must not be served
            // to the 720dp preview (and vice versa).
            val key = "$source@$sizePx"
            cache.get(key)?.let { return@withContext it }

            val bitmap = safely {
                val cached = if (sizePx <= THUMBNAIL_EDGE_PX || asset.localPath == null) {
                    asset.thumbnailPath?.let { relative ->
                        decodeFile(File(cacheDir, relative), sizePx)
                    }
                } else {
                    null
                }
                if (sizePx <= THUMBNAIL_EDGE_PX) {
                    cached ?: asset.localPath?.let { path ->
                        ThumbnailFactory.fromFile(context, File(path), asset.kind, sizePx)
                    } ?: loadUriInternal(asset.uri, sizePx)
                } else {
                    // Prefer the private copy for a large preview; the cached
                    // JPEG is only 320px and would look soft when upscaled.
                    asset.localPath?.let { path ->
                        ThumbnailFactory.fromFile(context, File(path), asset.kind, sizePx)
                    } ?: cached ?: loadUriInternal(asset.uri, sizePx)
                }
            }

            if (bitmap != null) cache.put(key, bitmap)
            bitmap
        }

    suspend fun loadUri(uri: String, sizePx: Int = 320): Bitmap? =
        withContext(Dispatchers.IO) {
            if (uri.isBlank()) return@withContext null
            val key = "$uri@$sizePx"
            cache.get(key)?.let { return@withContext it }
            val bitmap = safely { loadUriInternal(uri, sizePx) }
            if (bitmap != null) cache.put(key, bitmap)
            bitmap
        }

    /** A broken thumbnail must never crash the screen; the next load retries. */
    private inline fun <T> safely(block: () -> T?): T? = try {
        block()
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (error: Exception) {
        null
    }

    private fun loadUriInternal(uri: String, sizePx: Int): Bitmap? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(parsed, Size(sizePx, sizePx), null)
            }.getOrNull()?.let { return it }
        }
        return ThumbnailFactory.fromUri(context, parsed, sizePx)
    }

    private fun decodeFile(file: File, sizePx: Int): Bitmap? =
        if (file.isFile) ThumbnailFactory.decodeSampled(file, sizePx) else null

    companion object {
        /** Must match `MediaImporter`'s saved thumbnail size. */
        const val THUMBNAIL_EDGE_PX = 320
    }
}

/**
 * Stateless bitmap helpers shared by the importer and the picker.
 * Every retriever is released in a `finally`; every decode is sampled.
 */
object ThumbnailFactory {

    fun fromFile(context: Context, file: File, kind: MediaKind, sizePx: Int): Bitmap? {
        if (!file.isFile) return null
        return when (kind) {
            MediaKind.VIDEO -> videoFrame(context, Uri.fromFile(file), sizePx)
                ?: decodeSampled(file, sizePx)
            MediaKind.AUDIO -> null
            else -> decodeSampled(file, sizePx)
        }
    }

    fun fromUri(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        if (mime == null || mime.startsWith("video/")) {
            videoFrame(context, uri, sizePx)?.let { return it }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.scaled(sizePx)
            }
        }.getOrNull()
    }

    fun decodeSampled(file: File, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)?.scaled(sizePx)
    }

    fun saveJpeg(bitmap: Bitmap, target: File, quality: Int = 82): Boolean = runCatching {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        // Rename keeps a half-written thumbnail from being read on next launch.
        temporary.renameTo(target) || run {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
            true
        }
    }.getOrDefault(false)

    private fun videoFrame(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            frame?.scaled(sizePx)
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun Bitmap.scaled(maxEdge: Int): Bitmap {
        val longest = max(width, height)
        if (longest <= maxEdge || longest <= 0) return this
        val ratio = maxEdge.toFloat() / longest.toFloat()
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }
}
