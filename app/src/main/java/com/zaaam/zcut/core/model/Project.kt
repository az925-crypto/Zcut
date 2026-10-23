package com.zaaam.zcut.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val frameRate: Int = 30,
    val timeline: Timeline = Timeline(),
    val settings: ProjectSettings = ProjectSettings(),
    val coverAssetId: String? = null,
    val schemaVersion: Int = 1
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class ProjectSettings(
    val exportPreset: ExportPreset = ExportPreset(),
    val backgroundColorArgb: Long = 0xFF08090D,
    val autoSave: Boolean = true,
    val snapToBeat: Boolean = false
)

@Serializable
data class ExportPreset(
    val resolution: Resolution = Resolution.FHD_1080,
    val frameRate: Int = 30,
    val videoBitrate: Int = 12_000_000,
    val audioBitrate: Int = 192_000,
    val format: ExportFormat = ExportFormat.MP4_H264_AAC,
    val quality: ExportQuality = ExportQuality.HIGH
)

@Serializable
enum class Resolution(val shortEdge: Int) {
    HD_720(720),
    FHD_1080(1080),
    QHD_1440(1440),
    UHD_2160(2160)
}

@Serializable
enum class ExportFormat {
    MP4_H264_AAC,
    WEBM_VP9_OPUS,
    GIF,
    PNG_SEQUENCE
}

@Serializable
enum class ExportQuality {
    DRAFT,
    STANDARD,
    HIGH,
    MAX
}

@Serializable
data class DraftSummary(
    val id: String,
    val name: String,
    val updatedAtMs: Long,
    val durationMs: Long,
    val aspectRatio: AspectRatio,
    val coverAssetId: String? = null,
    val hasExport: Boolean = false
)
