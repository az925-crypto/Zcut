package com.zaaam.zcut.feature.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * What happens to a finished export: hand it to the system share sheet, or copy
 * it into the device gallery. Both paths stay on the device — this app has no
 * upload step, no account and no server.
 */
object ExportDelivery {

    /** Public gallery album for exports; the mockup calls it Movies/Zcut. */
    private const val GALLERY_DIR = "Movies/Zcut"

    /** Photo exports land next to the movies, in Pictures/Zcut. */
    private const val GALLERY_DIR_IMAGES = "Pictures/Zcut"

    private const val MIME_VIDEO = "video/mp4"
    private const val MIME_IMAGE = "image/jpeg"

    /**
     * Opens the system chooser for [file]; false when no app can handle it.
     *
     * [targetPackage] tries one app first (the share screen's channel tiles).
     * If that app is not installed the chooser still opens, so a channel tile
     * can never dead-end. Zcut only hands the file to Android — it never
     * uploads anything itself.
     */
    fun share(
        context: Context,
        file: File,
        mime: String = MIME_VIDEO,
        targetPackage: String? = null
    ): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val base = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (targetPackage != null) {
            val direct = Intent(base).setPackage(targetPackage)
            val opened = runCatching { context.startActivity(direct) }.isSuccess
            if (opened) return@runCatching true
        }
        context.startActivity(Intent.createChooser(base, "Bagikan"))
        true
    }.getOrDefault(false)

    /**
     * Copies the export into the gallery. Returns the location to show the user,
     * or null when the copy failed. On Android 10+ this needs no permission; on
     * older devices the file lands in the app's own Movies folder instead of the
     * shared gallery, and the returned string says so.
     */
    fun saveToGallery(context: Context, file: File): String? =
        saveMediaToGallery(context, file, isImage = false, mime = MIME_VIDEO)

    /**
     * Same contract as [saveToGallery] for a finished photo: the image
     * collection and Pictures/Zcut on Android 10+, the app's own Pictures folder
     * below that.
     */
    fun savePhotoToGallery(context: Context, file: File, mime: String = MIME_IMAGE): String? =
        saveMediaToGallery(context, file, isImage = true, mime = mime)

    private fun saveMediaToGallery(
        context: Context,
        file: File,
        isImage: Boolean,
        mime: String
    ): String? = runCatching {
        if (!file.isFile) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, file, isImage, mime)
        } else {
            saveToAppMedia(context, file, isImage)
        }
    }.getOrNull()

    private fun saveViaMediaStore(
        context: Context,
        file: File,
        isImage: Boolean,
        mime: String
    ): String? {
        val resolver = context.contentResolver
        val collection = if (isImage) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val album = if (isImage) GALLERY_DIR_IMAGES else GALLERY_DIR
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, album)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, pending) ?: return null

        val out = resolver.openOutputStream(uri)
        if (out == null) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        try {
            out.use { target ->
                file.inputStream().use { source -> source.copyTo(target) }
            }
        } catch (error: Exception) {
            // Do not leave a half-written row in the user's gallery.
            runCatching { resolver.delete(uri, null, null) }
            return null
        }

        val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        resolver.update(uri, ready, null, null)
        return "$album/${file.name}"
    }

    private fun saveToAppMedia(context: Context, file: File, isImage: Boolean): String? {
        val type = if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
        val dir = File(context.getExternalFilesDir(type), "Zcut")
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val target = File(dir, file.name)
        file.inputStream().use { source ->
            target.outputStream().use { sink -> source.copyTo(sink) }
        }
        return target.absolutePath
    }
}
