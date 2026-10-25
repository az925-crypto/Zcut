package com.zaaam.zcut.feature.editor

import com.zaaam.zcut.core.model.Project

data class EditorUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val selectedClipId: String? = null,
    val notFound: Boolean = false,
    val error: String? = null
) {
    val durationMs: Long
        get() = project?.timeline?.durationMs ?: 0L

    val isEmpty: Boolean
        get() = project != null && project.timeline.tracks.all { it.clips.isEmpty() }
}
