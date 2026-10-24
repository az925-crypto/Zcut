package com.zaaam.zcut.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * M2 editor is read-only: it observes the draft instead of holding a private
 * copy, so media imported by the picker appears without a manual refresh and a
 * stale editor can never overwrite newer edits.
 *
 * Playhead and selection are UI state only; they are not persisted yet.
 */
class EditorViewModel(
    private val projectId: String,
    repository: ProjectRepository
) : ViewModel() {

    private val selectedClipId = MutableStateFlow<String?>(null)
    private val playheadMs = MutableStateFlow(0L)

    val uiState: StateFlow<EditorUiState> = combine(
        repository.projects,
        selectedClipId,
        playheadMs
    ) { projects, selected, position ->
        val project = projects.firstOrNull { it.id == projectId }
        if (project == null) {
            EditorUiState(isLoading = false, notFound = true)
        } else {
            val duration = project.timeline.durationMs
            EditorUiState(
                isLoading = false,
                project = project,
                selectedClipId = selected?.takeIf { project.timeline.clip(it) != null },
                playheadMs = position.coerceIn(0L, duration)
            )
        }
    }
        .catch { error ->
            emit(
                EditorUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditorUiState()
        )

    fun selectClip(clipId: String?) {
        selectedClipId.value = if (selectedClipId.value == clipId) null else clipId
    }

    fun seekTo(positionMs: Long) {
        playheadMs.value = positionMs.coerceAtLeast(0L)
    }
}
