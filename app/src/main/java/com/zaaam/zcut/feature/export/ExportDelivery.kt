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

    private const val MIME_VIDEO = "video/mp4"

    /** Opens the system chooser for [file]; false when no app can handle it. */
    fun share(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_VIDEO
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan video"))
        true
    }.getOrDefault(false)

    /**
     * Copies the export into the gallery. Returns the location to show the user,
     * or null when the copy failed. On Android 10+ this needs no permission; on
     * older devices the file lands in the app's own Movies folder instead of the
     * shared gallery, and the returned string says so.
     */
    fun saveToGallery(context: Context, file: File): String? = runCatching {
        if (!file.isFile) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, file)
        } else {
            saveToAppMovies(context, file)
        }
    }.getOrNull()

    private fun saveViaMediaStore(context: Context, file: File): String? {
        val resolver = context.contentResolver
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_VIDEO)
            put(MediaStore.MediaColumns.RELATIVE_PATH, GALLERY_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            pending
        ) ?: return null

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
        return "$GALLERY_DIR/${file.name}"
    }

    private fun saveToAppMovies(context: Context, file: File): String? {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Zcut")
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val target = File(dir, file.name)
        file.inputStream().use { source ->
            target.outputStream().use { sink -> source.copyTo(sink) }
        }
        return target.absolutePath
    }
}
