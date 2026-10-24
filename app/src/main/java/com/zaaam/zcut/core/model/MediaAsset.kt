package com.zaaam.zcut.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MediaKind {
    VIDEO,
    IMAGE,
    AUDIO,
    GIF,
    LIVE_PHOTO
}

@Serializable
data class MediaAsset(
    val id: String = UUID.randomUUID().toString(),
    /** Original picker URI. Kept for provenance; playback uses [localPath]. */
    val uri: String = "",
    val displayName: String,
    val mimeType: String = "application/octet-stream",
    val kind: MediaKind,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val hasAudio: Boolean = false,
    /** Copy inside the app's private workspace. Null only for legacy entries. */
    val localPath: String? = null,
    /** Relative path inside cacheDir; may disappear after "Bersihkan". */
    val thumbnailPath: String? = null,
    val rotationDegrees: Int = 0
) {
    val isVisual: Boolean
        get() = kind == MediaKind.VIDEO || kind == MediaKind.IMAGE ||
            kind == MediaKind.GIF || kind == MediaKind.LIVE_PHOTO

    /** File the editor should read, preferring the private copy. */
    val sourcePath: String?
        get() = localPath?.takeIf { it.isNotBlank() }
            ?: uri.takeIf { it.isNotBlank() }

    /** Usable entries must point somewhere and carry a positive identity. */
    val isUsable: Boolean
        get() = id.isNotBlank() && sourcePath != null
}
