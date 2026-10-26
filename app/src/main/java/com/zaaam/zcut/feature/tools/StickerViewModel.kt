package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.StickerEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Transform
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StickerUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val clip: Clip? = null,
    val notFound: Boolean = false,
    val error: String? = null
)

/**
 * Read/write layer for the sticker panel. Like the text panel it is reachable
 * with no selection so it can create a clip; [selectedId] starts from the route
 * argument and switches to the new clip after the first add.
 */
class StickerViewModel(
    private val projectId: String,
    initialClipId: String?,
    private val repository: ProjectRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow(initialClipId?.takeIf { it.isNotBlank() })
    private val editMutex = Mutex()

    val uiState: StateFlow<StickerUiState> = combine(
        repository.projects,
        selectedId
    ) { projects, id ->
        val project = projects.firstOrNull { it.id == projectId }
        if (project == null) {
            StickerUiState(isLoading = false, notFound = true)
        } else {
            val clip = id
                ?.let(project.timeline::clip)
                ?.takeIf { it.type == ClipType.STICKER }
            StickerUiState(isLoading = false, project = project, clip = clip)
        }
    }
        .catch { error ->
            emit(
                StickerUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StickerUiState()
        )

    /**
     * Creates a sticker clip when nothing is selected, otherwise updates the
     * selected one. One mutex protects the read-modify-write against a second
     * gesture from this screen.
     */
    fun save(
        stickerId: String,
        transform: Transform,
        opacity: Float,
        atMs: Long = 0L,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                if (project == null) {
                    onDone()
                    return@withLock
                }
                val currentId = selectedId.value
                val existing = currentId
                    ?.let(project.timeline::clip)
                    ?.takeIf { it.type == ClipType.STICKER }
                val updated = if (existing == null) {
                    val newId = UUID.randomUUID().toString()
                    val result = StickerEditor.addSticker(
                        timeline = project.timeline,
                        stickerId = stickerId,
                        atMs = atMs.coerceAtLeast(0L),
                        transform = transform,
                        opacity = opacity,
                        clipId = newId
                    )
                    if (result.clip(newId) != null) {
                        selectedId.value = newId
                        result
                    } else {
                        project.timeline
                    }
                } else {
                    StickerEditor.setSticker(
                        timeline = project.timeline,
                        clipId = existing.id,
                        stickerId = stickerId,
                        transform = transform,
                        opacity = opacity
                    )
                }
                if (updated != project.timeline) {
                    runCatching {
                        repository.upsert(project.copy(timeline = updated))
                    }
                }
                onDone()
            }
        }
    }

    fun delete(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                val id = selectedId.value
                if (project == null || id == null) {
                    onDone()
                    return@withLock
                }
                val updated = StickerEditor.removeSticker(project.timeline, id)
                if (updated != project.timeline) {
                    runCatching {
                        repository.upsert(project.copy(timeline = updated))
                    }
                }
                selectedId.value = null
                onDone()
            }
        }
    }
}