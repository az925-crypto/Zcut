package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.domain.CaptionSegmenter
import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import com.zaaam.zcut.core.util.SrtFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CaptionUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val notFound: Boolean = false,
    val error: String? = null
) {
    val transcript: Transcript get() = project?.transcript ?: Transcript()
    val cues: List<CaptionCue> get() = transcript.cues
}

/**
 * Read/write layer for the caption panel.
 *
 * Captions live on the draft ([Project.transcript]) rather than on a clip: a cue
 * is a time window plus text, independent of the visual timeline. Every write is
 * a read-modify-write behind one mutex so a transcription stream and a manual
 * edit cannot clobber each other.
 */
class CaptionViewModel(
    private val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    private val editMutex = Mutex()

    val uiState: StateFlow<CaptionUiState> = repository.projects
        .map { projects ->
            val project = projects.firstOrNull { it.id == projectId }
            if (project == null) {
                CaptionUiState(isLoading = false, notFound = true)
            } else {
                CaptionUiState(isLoading = false, project = project)
            }
        }
        .catch { error ->
            emit(
                CaptionUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CaptionUiState()
        )

    /** Adds every line produced by one recognised phrase. */
    fun addLines(lines: List<CaptionSegmenter.Line>) {
        if (lines.isEmpty()) return
        mutate { transcript ->
            lines.fold(transcript) { acc, line ->
                CaptionEditor.addCue(acc, line.text, line.startMs, line.endMs)
            }
        }
    }

    /** One manual cue; times come from the playhead the panel was opened at. */
    fun addCue(text: String, startMs: Long, endMs: Long, onDone: () -> Unit = {}) =
        mutate(onDone) { CaptionEditor.addCue(it, text, startMs, endMs) }

    fun updateText(cueId: String, text: String) =
        mutate { CaptionEditor.updateCue(it, cueId, text = text) }

    fun removeCue(cueId: String) = mutate { CaptionEditor.removeCue(it, cueId) }

    fun setStyle(style: TextConfig) = mutate { CaptionEditor.setStyle(it, style) }

    fun clearAll() = mutate { CaptionEditor.clear(it) }

    /** Replaces the transcript with the cues read from an SRT file the user picked. */
    fun importSrt(srt: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                val parsed = if (project == null) null else SrtFormat.parse(srt)
                if (project == null || parsed == null || parsed.cues.isEmpty()) {
                    onDone(false)
                    return@withLock
                }
                val updated = CaptionEditor.replaceCues(
                    project.transcript,
                    parsed.cues,
                    TranscriptSource.IMPORTED_SRT
                )
                if (updated != project.transcript) {
                    runCatching {
                        repository.upsert(project.copy(transcript = updated))
                    }
                }
                onDone(true)
            }
        }
    }

    private fun mutate(
        onDone: () -> Unit = {},
        transform: (Transcript) -> Transcript
    ) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                if (project != null) {
                    val updated = transform(project.transcript)
                    if (updated != project.transcript) {
                        runCatching {
                            repository.upsert(project.copy(transcript = updated))
                        }
                    }
                }
                onDone()
            }
        }
    }
}