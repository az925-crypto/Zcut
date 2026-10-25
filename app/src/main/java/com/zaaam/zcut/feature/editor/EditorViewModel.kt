package com.zaaam.zcut.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import com.zaaam.zcut.core.render.PreviewEngine
import com.zaaam.zcut.core.render.PreviewSeek
import com.zaaam.zcut.core.render.PreviewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The editor observes the draft instead of holding a private copy, so media
 * imported by the picker appears without a manual refresh and a stale editor
 * can never overwrite newer edits.
 *
 * Playback position lives in its own flow: the timeline reads it during layout
 * and only the small clock composable reads it during composition, so a moving
 * playhead does not recompose every clip at 30 fps.
 */
class EditorViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val previewEngine: PreviewEngine
) : ViewModel() {

    private val selectedClipId = MutableStateFlow<String?>(null)
    private val playheadMs = MutableStateFlow(0L)

    /** Serialises read-modify-write edits so two fast gestures cannot drop one. */
    private val editMutex = Mutex()

    val playhead: StateFlow<Long> = playheadMs.asStateFlow()
    val previewState: StateFlow<PreviewState> = previewEngine.state
    val previewPlayer: StateFlow<Player?> = previewEngine.playerFlow

    private var preparedTimeline: Timeline? = null
    private var preparedAssets: List<MediaAsset>? = null

    /**
     * The ViewModel survives on the back stack. While another screen (picker,
     * export) is on top, rebuilding a CompositionPlayer would tear down a GL
     * pipeline for an invisible editor, so changes wait until ON_START.
     */
    private var screenActive = true
    private var pendingPrepare: Project? = null

    val uiState: StateFlow<EditorUiState> = combine(
        repository.projects,
        selectedClipId
    ) { projects, selected ->
        val project = projects.firstOrNull { it.id == projectId }
        if (project == null) {
            EditorUiState(isLoading = false, notFound = true)
        } else {
            EditorUiState(
                isLoading = false,
                project = project,
                selectedClipId = selected?.takeIf { project.timeline.clip(it) != null }
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

    init {
        viewModelScope.launch {
            previewEngine.positionMs.collect { position ->
                playheadMs.value = position.coerceAtLeast(0L)
            }
        }
        viewModelScope.launch {
            repository.projects.collect { projects ->
                val project = projects.firstOrNull { it.id == projectId }
                if (project == null) {
                    pendingPrepare = null
                    previewEngine.pause()
                    return@collect
                }
                if (project.timeline != preparedTimeline || project.assets != preparedAssets) {
                    if (screenActive) {
                        prepare(project)
                    } else {
                        pendingPrepare = project
                    }
                }
            }
        }
    }

    private fun prepare(project: Project) {
        preparedTimeline = project.timeline
        preparedAssets = project.assets
        pendingPrepare = null
        val start = PreviewSeek.clamp(playheadMs.value, project.timeline.durationMs)
        previewEngine.prepare(project, start)
    }

    /** Called when the editor becomes visible again; flushes deferred rebuilds. */
    fun onScreenStarted() {
        screenActive = true
        pendingPrepare?.let(::prepare)
    }

    /** Stops playback and defers preview rebuilds while the editor is hidden. */
    fun onScreenStopped() {
        screenActive = false
        previewEngine.pause()
    }

    override fun onCleared() {
        previewEngine.release()
        super.onCleared()
    }

    fun selectClip(clipId: String?) {
        selectedClipId.value = if (selectedClipId.value == clipId) null else clipId
    }

    fun selectClipOnly(clipId: String) {
        selectedClipId.value = clipId
    }

    fun seekTo(positionMs: Long) {
        val target = PreviewSeek.clamp(positionMs, uiState.value.durationMs)
        playheadMs.value = target
        previewEngine.seekTo(target)
    }

    /**
     * Playing with a selected clip previews that clip only. Without a
     * selection the whole timeline plays from the playhead.
     */
    fun togglePlayback() {
        val preview = previewState.value
        if (!preview.isPlayable || preview.error != null) return
        if (preview.isPlaying) {
            previewEngine.pause()
            return
        }
        val project = uiState.value.project ?: return
        val selected = uiState.value.selectedClipId?.let { project.timeline.clip(it) }
        previewEngine.setRange(selected?.startMs, selected?.endMs)
        previewEngine.play()
    }

    fun toggleMute() {
        previewEngine.setMuted(!previewEngine.state.value.isMuted)
    }

    // ---- M4 timeline commands ------------------------------------------------

    fun splitAtPlayhead() {
        val atMs = playheadMs.value
        editTimeline { timeline ->
            val selected = uiState.value.selectedClipId
            if (selected != null) {
                TimelineEditor.splitClip(timeline, selected, atMs)
            } else {
                // No selection: split every clip crossing the playhead, which is
                // the least destructive interpretation of the footer button.
                timeline.tracks
                    .flatMap { track -> track.clips }
                    .filter { atMs > it.startMs && atMs < it.endMs }
                    .fold(timeline) { current, clip ->
                        TimelineEditor.splitClip(current, clip.id, atMs)
                    }
            }
        }
    }

    fun deleteSelected(ripple: Boolean = false) {
        val clipId = uiState.value.selectedClipId ?: return
        editTimeline { timeline -> TimelineEditor.deleteClip(timeline, clipId, ripple) }
    }

    fun duplicateSelected() {
        val clipId = uiState.value.selectedClipId ?: return
        editTimeline { timeline -> TimelineEditor.duplicateClip(timeline, clipId) }
    }

    fun moveClip(clipId: String, targetTrackId: String, newStartMs: Long) {
        editTimelineWithSettings { project ->
            val snapped = TimelineEditor.snapPosition(
                newStartMs,
                snapCandidates(project, clipId)
            )
            TimelineEditor.moveClip(project.timeline, clipId, targetTrackId, snapped)
        }
    }

    fun trimClip(clipId: String, newStartMs: Long, newEndMs: Long) {
        editTimelineWithSettings { project ->
            val clip = project.timeline.clip(clipId) ?: return@editTimelineWithSettings project.timeline
            val candidates = snapCandidates(project, clipId)
            // Only snap the edge the gesture actually changed; snapping the
            // untouched edge lets a trim pull its twin, or snap it back to the
            // same position and swallow the whole gesture.
            val start = if (newStartMs != clip.startMs) {
                TimelineEditor.snapPosition(newStartMs, candidates)
            } else {
                newStartMs
            }
            val end = if (newEndMs != clip.endMs) {
                TimelineEditor.snapPosition(newEndMs, candidates)
            } else {
                newEndMs
            }
            TimelineEditor.trimClip(project.timeline, clipId, start, end)
        }
    }

    /**
     * Clip boundaries plus, when the draft enables it, the locally detected beat
     * grid. Beats only participate while "snap ke beat" is on.
     */
    private fun snapCandidates(project: Project, clipId: String): List<Long> {
        val base = TimelineEditor.snapCandidates(project.timeline, clipId)
        val settings = project.settings
        if (!settings.snapToBeat || settings.beatGridMs.isEmpty()) return base
        return (base + settings.beatGridMs).distinct().sorted()
    }

    private fun editTimeline(transform: (Timeline) -> Timeline) {
        editTimelineWithSettings { project -> transform(project.timeline) }
    }

    /** Same as [editTimeline] but the transform also sees the draft's settings. */
    private fun editTimelineWithSettings(transform: (Project) -> Timeline) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId) ?: return@withLock
                val updated = transform(project)
                if (updated == project.timeline) return@withLock
                runCatching {
                    repository.upsert(project.copy(timeline = updated))
                }
                // The preview rebuild is driven by the repository flow, but the
                // local playhead must not point past a now-shorter timeline.
                playheadMs.value = PreviewSeek.clamp(playheadMs.value, updated.durationMs)
            }
        }
    }
}
