package com.zaaam.zcut.core.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies picked media into the app's private workspace before it is referenced
 * by a draft, so a moved/deleted gallery item or an expired URI grant cannot
 * break an old project.
 *
 * Every failure path removes the partial copy and thumbnail it created.
 */
class MediaImporter(
    private val context: Context,
    private val mediaDir: File,
    private val thumbnailDir: File
) {

    sealed interface Result {
        data class Success(val asset: MediaAsset) : Result
        data class Failure(val reason: Reason, val displayName: String? = null) : Result
    }

    enum class Reason {
        UNSUPPORTED,
        TOO_LARGE,
        NO_SPACE,
        UNREADABLE,
        COPY_FAILED
    }

    suspend fun import(uri: Uri): Result = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri)
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val kind = MediaTypes.kindFor(mimeType, displayName)
            ?: return@withContext Result.Failure(Reason.UNSUPPORTED, displayName)

        val declaredSize = querySize(uri)
        if (declaredSize != null && !MediaTypes.isWithinSizeLimit(declaredSize)) {
            return@withContext Result.Failure(Reason.TOO_LARGE, displayName)
        }

        mediaDir.mkdirs()
        thumbnailDir.mkdirs()
        if (!MediaTypes.hasRoomFor(mediaDir.usableSpace, declaredSize)) {
            return@withContext Result.Failure(Reason.NO_SPACE, displayName)
        }

        val assetId = UUID.randomUUID().toString()
        val target = File(mediaDir, "$assetId.${MediaTypes.extensionFor(mimeType, displayName)}")
        var thumbnailFile: File? = null
        try {
            val copy = copyToWorkspace(uri, target)
            when (copy) {
                CopyOutcome.Unreadable ->
                    return@withContext Result.Failure(Reason.UNREADABLE, displayName)
                CopyOutcome.TooLarge ->
                    return@withContext Result.Failure(Reason.TOO_LARGE, displayName)
                CopyOutcome.NoSpace ->
                    return@withContext Result.Failure(Reason.NO_SPACE, displayName)
                CopyOutcome.Copied -> Unit
            }
            if (target.length() == 0L) {
                target.delete()
                return@withContext Result.Failure(Reason.UNREADABLE, displayName)
            }

            val probe = probe(target, kind)
            // A missing thumbnail is cosmetic: the timeline regenerates it from
            // the private copy later, so it must not fail the whole import.
            val thumbnail = runCatching {
                ThumbnailFactory.fromFile(
                    context,
                    target,
                    kind,
                    MediaThumbnails.THUMBNAIL_EDGE_PX
                )
            }.getOrNull()
            if (thumbnail != null) {
                try {
                    val candidate = File(thumbnailDir, "$assetId.jpg")
                    if (ThumbnailFactory.saveJpeg(thumbnail, candidate)) {
                        thumbnailFile = candidate
                    }
                } finally {
                    thumbnail.recycle()
                }
            }

            Result.Success(
                MediaAsset(
                    id = assetId,
                    uri = uri.toString(),
                    displayName = displayName ?: "Media",
                    mimeType = mimeType ?: "application/octet-stream",
                    kind = kind,
                    durationMs = probe.durationMs,
                    width = probe.width,
                    height = probe.height,
                    sizeBytes = target.length(),
                    hasAudio = probe.hasAudio,
                    localPath = target.absolutePath,
                    thumbnailPath = thumbnailFile?.let(::relativeToCache),
                    rotationDegrees = probe.rotationDegrees
                )
            )
        } catch (cancel: CancellationException) {
            target.delete()
            thumbnailFile?.delete()
            throw cancel
        } catch (error: Exception) {
            target.delete()
            thumbnailFile?.delete()
            Result.Failure(Reason.COPY_FAILED, displayName)
        }
    }

    private fun copyToWorkspace(uri: Uri, target: File): CopyOutcome {
        val input = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            ?: return CopyOutcome.Unreadable
        var total = 0L
        var nextSpaceCheck = SPACE_CHECK_BYTES
        try {
            input.use { source ->
                target.outputStream().use { sink ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        total += read
                        if (total > MediaTypes.MAX_IMPORT_BYTES) {
                            throw TooLargeException()
                        }
                        // A provider can under-report SIZE; check the real free
                        // space as the copy grows so it cannot fill the volume.
                        if (total >= nextSpaceCheck) {
                            if (target.usableSpace < MediaTypes.FREE_SPACE_HEADROOM_BYTES) {
                                throw NoSpaceException()
                            }
                            nextSpaceCheck += SPACE_CHECK_BYTES
                        }
                        sink.write(buffer, 0, read)
                    }
                    sink.flush()
                }
            }
        } catch (error: TooLargeException) {
            target.delete()
            return CopyOutcome.TooLarge
        } catch (error: NoSpaceException) {
            target.delete()
            return CopyOutcome.NoSpace
        }
        return CopyOutcome.Copied
    }

    private fun queryDisplayName(uri: Uri): String? {
        val fromProvider = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return fromProvider?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.takeIf { it.isNotBlank() }
    }

    private fun querySize(uri: Uri): Long? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            // Some providers use -1/0 for "unknown"; treat those as unknown
            // instead of rejecting a perfectly valid file as oversized.
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                cursor.getLong(0).takeIf { it > 0L }
            } else {
                null
            }
        }
    }.getOrNull()

    private fun relativeToCache(file: File): String? =
        runCatching { file.relativeTo(context.cacheDir).path }.getOrNull()

    private fun probe(file: File, kind: MediaKind): Probe {
        if (kind != MediaKind.VIDEO && kind != MediaKind.AUDIO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            return Probe(
                durationMs = 0L,
                width = options.outWidth.coerceAtLeast(0),
                height = options.outHeight.coerceAtLeast(0),
                rotationDegrees = 0,
                hasAudio = false
            )
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            Probe(
                durationMs = retriever.metadataLong(MediaMetadataRetriever.METADATA_KEY_DURATION),
                width = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
                height = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),
                rotationDegrees = retriever.metadataInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION),
                hasAudio = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
                ) == "yes"
            )
        } catch (error: Exception) {
            // Unreadable metadata is not fatal: the clip is still usable and the
            // timeline falls back to a safe duration.
            Probe(0L, 0, 0, 0, false)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaMetadataRetriever.metadataLong(key: Int): Long =
        extractMetadata(key)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

    private fun MediaMetadataRetriever.metadataInt(key: Int): Int =
        extractMetadata(key)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

    private data class Probe(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val hasAudio: Boolean
    )

    private sealed interface CopyOutcome {
        data object Copied : CopyOutcome
        data object Unreadable : CopyOutcome
        data object TooLarge : CopyOutcome
        data object NoSpace : CopyOutcome
    }

    private class TooLargeException : Exception()

    private class NoSpaceException : Exception()

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1024

        /** 16 MB between free-space syscalls: frequent enough, not per buffer. */
        const val SPACE_CHECK_BYTES = 16L * 1024L * 1024L
    }
}
