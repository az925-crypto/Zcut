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
    val uri: String,
    val displayName: String,
    val mimeType: String = "application/octet-stream",
    val kind: MediaKind,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val hasAudio: Boolean = false,
    val thumbnailPath: String? = null
) {
    val isVisual: Boolean
        get() = kind == MediaKind.VIDEO || kind == MediaKind.IMAGE ||
            kind == MediaKind.GIF || kind == MediaKind.LIVE_PHOTO
}
