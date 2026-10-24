package com.zaaam.zcut.core.media

import com.zaaam.zcut.core.model.MediaKind

/**
 * MIME/extension policy for imports. Pure JVM code: no `android.*` here, so the
 * allowlist can be unit-tested without a device.
 */
object MediaTypes {

    const val MAX_IMPORT_BYTES: Long = 2L * 1024L * 1024L * 1024L
    const val FREE_SPACE_HEADROOM_BYTES: Long = 256L * 1024L * 1024L

    private val videoExtensions = setOf("mp4", "m4v", "mov", "webm", "mkv", "3gp", "ts")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "amr")

    private val extensionByMime = mapOf(
        "video/mp4" to "mp4",
        "video/quicktime" to "mov",
        "video/webm" to "webm",
        "video/x-matroska" to "mkv",
        "video/3gpp" to "3gp",
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/heic" to "heic",
        "image/heif" to "heif",
        "image/gif" to "gif",
        "image/bmp" to "bmp",
        "audio/mpeg" to "mp3",
        "audio/mp4" to "m4a",
        "audio/aac" to "aac",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/ogg" to "ogg",
        "audio/opus" to "opus",
        "audio/flac" to "flac",
        "audio/amr" to "amr"
    )

    /** Null means "not importable"; callers must reject it instead of guessing. */
    fun kindFor(mimeType: String?, displayName: String? = null): MediaKind? {
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        when {
            mime == "image/gif" -> return MediaKind.GIF
            mime.startsWith("video/") -> return MediaKind.VIDEO
            mime.startsWith("image/") -> return MediaKind.IMAGE
            mime.startsWith("audio/") -> return MediaKind.AUDIO
        }
        // Some document providers report application/octet-stream.
        val extension = extensionOf(displayName)
        return when {
            extension == "gif" -> MediaKind.GIF
            extension in videoExtensions -> MediaKind.VIDEO
            extension in imageExtensions -> MediaKind.IMAGE
            extension in audioExtensions -> MediaKind.AUDIO
            else -> null
        }
    }

    fun isSupported(mimeType: String?, displayName: String? = null): Boolean =
        kindFor(mimeType, displayName) != null

    /** File extension for the private copy, never blank. */
    fun extensionFor(mimeType: String?, displayName: String?): String {
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        extensionByMime[mime]?.let { return it }
        val fromName = extensionOf(displayName)
        if (fromName.isNotBlank()) return fromName
        return when {
            mime.startsWith("video/") -> "mp4"
            mime.startsWith("image/") -> "jpg"
            mime.startsWith("audio/") -> "m4a"
            else -> "bin"
        }
    }

    fun isWithinSizeLimit(sizeBytes: Long): Boolean =
        sizeBytes in 0..MAX_IMPORT_BYTES

    /** `null` size means the provider did not say; the copy loop enforces the cap. */
    fun hasRoomFor(freeBytes: Long, sizeBytes: Long?): Boolean {
        val needed = (sizeBytes ?: 0L).coerceAtLeast(0L) + FREE_SPACE_HEADROOM_BYTES
        return freeBytes >= needed
    }

    private fun extensionOf(displayName: String?): String {
        val name = displayName?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.lastIndex) return ""
        val extension = name.substring(dot + 1).lowercase()
        return extension.filter { it.isLetterOrDigit() }.take(5)
    }
}
