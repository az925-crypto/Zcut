package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ClipToolUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val clip: Clip? = null,
    val missingSelection: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null
)

/**
 * Shared read/write layer for the editor's clip panels (trim, speed, keyframes,
 * transitions, filters, effects, mask). Every write is a read-modify-write under
 * one mutex so a fast gesture cannot clobber a newer draft.
 */
class ClipToolViewModel(
    private val projectId: String,
    clipId: String?,
    private val repository: ProjectRepository
) : ViewModel() {

    private val clipId: String? = clipId?.takeIf { it.isNotBlank() }
    private val editMutex = Mutex()

    val uiState: StateFlow<ClipToolUiState> = repository.projects
        .map { projects ->
            val selectedId = clipId
            val project = projects.firstOrNull { it.id == projectId }
            when {
                project == null -> ClipToolUiState(
                    isLoading = false,
                    notFound = true
                )
                selectedId == null -> ClipToolUiState(
                    isLoading = false,
                    project = project,
                    missingSelection = true
                )
                else -> {
                    val clip = project.timeline.clip(selectedId)
                    ClipToolUiState(
                        isLoading = false,
                        project = project,
                        clip = clip,
                        notFound = clip == null
                    )
                }
            }
        }
        .catch { error ->
            emit(
                ClipToolUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ClipToolUiState()
        )

    fun trim(newStartMs: Long, newEndMs: Long) {
        val id = clipId ?: return
        edit { TimelineEditor.trimClip(it, id, newStartMs, newEndMs) }
    }

    fun splitAt(positionMs: Long) {
        val id = clipId ?: return
        edit { TimelineEditor.splitClip(it, id, positionMs) }
    }

    fun duplicate() {
        val id = clipId ?: return
        edit { TimelineEditor.duplicateClip(it, id) }
    }

    fun delete(ripple: Boolean, onDone: () -> Unit = {}) {
        val id = clipId ?: return
        edit({ TimelineEditor.deleteClip(it, id, ripple) }, onDone)
    }

    fun editClip(transform: (Clip) -> Clip) {
        val id = clipId ?: return
        edit { TimelineEditor.editClip(it, id, transform) }
    }

    fun editTimeline(transform: (Timeline) -> Timeline) {
        edit(transform)
    }

    private fun edit(
        transform: (Timeline) -> Timeline,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                if (project == null) {
                    onDone?.invoke()
                    return@withLock
                }
                val updated = transform(project.timeline)
                if (updated != project.timeline) {
                    runCatching { repository.upsert(project.copy(timeline = updated)) }
                }
                onDone?.invoke()
            }
        }
    }
}
