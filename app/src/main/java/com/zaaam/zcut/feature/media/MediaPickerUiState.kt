package com.zaaam.zcut.feature.media

enum class MediaFilter(val label: String, val mimeFilter: String) {
    PHOTOS("Foto", "image/*"),
    VIDEOS("Video", "video/*"),
    AUDIO("Audio", "audio/*")
}

data class ImportProgress(val done: Int, val total: Int)

data class MediaPickerUiState(
    val projectId: String? = null,
    val projectName: String? = null,
    /** Set when the picker is filling a template's slots (cut-same flow). */
    val templateName: String? = null,
    val requiredSlots: Int? = null,
    val filter: MediaFilter = MediaFilter.PHOTOS,
    val selected: List<String> = emptyList(),
    val isImporting: Boolean = false,
    val progress: ImportProgress? = null,
    val notice: String? = null,
    /** Set only when a partial import still needs an acknowledgement. */
    val pendingProjectId: String? = null
)
