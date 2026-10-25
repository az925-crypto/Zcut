package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.KeyframeEditor
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Easing
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.SpeedPoint
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
        edit(onDone = onDone) { TimelineEditor.deleteClip(it, id, ripple) }
    }

    fun setSpeed(speed: Float) {
        val id = clipId ?: return
        edit { TimelineEditor.retimeClip(it, id, speed, emptyList()) }
    }

    fun setCurve(points: List<SpeedPoint>, fallbackSpeed: Float) {
        val id = clipId ?: return
        edit { TimelineEditor.retimeClip(it, id, fallbackSpeed, points) }
    }

    fun setPreservePitch(preserve: Boolean) {
        editClip { clip -> clip.copy(speed = clip.speed.copy(preservePitch = preserve)) }
    }

    fun setReversed(reversed: Boolean) {
        val id = clipId ?: return
        edit { TimelineEditor.setReversed(it, id, reversed) }
    }

    fun addKeyframe(property: AnimatableProperty, timeMs: Long) {
        editClip { clip ->
            KeyframeEditor.upsert(
                clip = clip,
                property = property,
                timeMs = timeMs,
                value = KeyframeEditor.sample(clip, property, timeMs)
            )
        }
    }

    fun removeKeyframe(property: AnimatableProperty, timeMs: Long) {
        editClip { clip -> KeyframeEditor.remove(clip, property, timeMs) }
    }

    fun clearKeyframes(property: AnimatableProperty) {
        editClip { clip ->
            val remaining = clip.keyframes.filterNot { it.property == property }
            if (remaining.size == clip.keyframes.size) clip else {
                clip.copy(keyframes = remaining)
            }
        }
    }

    /** Slider edits: static property, auto-keyed once the property is animated. */    fun setPropertyValue(property: AnimatableProperty, timeMs: Long, value: Float) {
        editClip { clip ->
            KeyframeEditor.setPropertyAndAutoKey(clip, property, value, timeMs)
        }
    }

    fun setKeyframeEasing(property: AnimatableProperty, timeMs: Long, easing: Easing) {
        editClip { clip -> KeyframeEditor.setEasing(clip, property, timeMs, easing) }
    }

    fun editClip(transform: (Clip) -> Clip) {
        val id = clipId ?: return
        edit { TimelineEditor.editClip(it, id, transform) }
    }

    fun editTimeline(transform: (Timeline) -> Timeline) {
        edit(transform = transform)
    }

    private fun edit(
        onDone: (() -> Unit)? = null,
        transform: (Timeline) -> Timeline
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
