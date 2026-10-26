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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    /**
     * Typed-but-not-yet-written cue text, keyed by cue id. Held as state (not a
     * plain map) so the rows read the authoritative value and a recycled row can
     * neither show nor overwrite stale text.
     */
    private val _drafts = MutableStateFlow<Map<String, String>>(emptyMap())
    val drafts: StateFlow<Map<String, String>> = _drafts.asStateFlow()

    private var draftJob: Job? = null

    /**
     * ponytail: a single-threaded scope that outlives this ViewModel, so the last
     * debounce flush still lands when the user leaves the panel mid-typing, and two
     * flushes can never reorder each other. The write is one short transaction.
     */
    private val persistScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )

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

    /**
     * Live edits from the manual tab. Writing the whole draft on every keystroke
     * is wasteful, so the text is only remembered here and persisted once typing
     * pauses — or immediately when the panel/tab goes away.
     *
     * The text lives in the ViewModel rather than in the row so a row recycled out
     * of the lazy list cannot show (or later overwrite) a stale value. Emptying a
     * row is a real edit: on flush it removes the cue, exactly like [removeCue].
     */
    fun markDraft(cueId: String, text: String) {
        _drafts.update { current ->
            current + (cueId to text.take(CaptionEditor.MAX_LINE_LENGTH))
        }
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(DRAFT_DEBOUNCE_MS)
            flushDrafts()
        }
    }

    /**
     * Persists every pending draft now. Safe to call from a `DisposableEffect`:
     * the write runs on a scope that does not die with the screen, so leaving the
     * panel within the debounce window cannot silently drop a typed edit.
     */
    fun flushDrafts() {
        val pending = _drafts.value
        if (pending.isEmpty()) return
        persistScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId) ?: return@withLock
                var transcript = project.transcript
                pending.forEach { (id, text) ->
                    transcript = CaptionEditor.updateCue(transcript, id, text = text)
                }
                if (transcript == project.transcript) {
                    // Nothing to write (the cue is gone, or the text already
                    // matches): these entries are settled.
                    settle(pending)
                    return@withLock
                }
                val saved = runCatching {
                    repository.upsert(project.copy(transcript = transcript))
                }.isSuccess
                // Kept in the map until the write lands, so a row never falls back
                // to its old text while the upsert is still in flight; a failed
                // write leaves the text pending for the next flush.
                if (saved) settle(pending)
            }
        }
    }

    /**
     * Drops exactly the entries that typing has not already replaced, so a newer
     * draft can never be clobbered by a snapshot an older flush was holding.
     */
    private fun settle(pending: Map<String, String>) {
        _drafts.update { current ->
            pending.entries.fold(current) { acc, (id, text) ->
                if (acc[id] == text) acc - id else acc
            }
        }
    }

    override fun onCleared() {
        flushDrafts()
    }

    fun removeCue(cueId: String) = mutate { CaptionEditor.removeCue(it, cueId) }

    fun setStyle(style: TextConfig) = mutate { CaptionEditor.setStyle(it, style) }

    fun clearAll() = mutate { CaptionEditor.clear(it) }

    /** Replaces the transcript with the cues read from an SRT file the user picked. */
    fun importSrt(srt: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            editMutex.withLock {
                val project = repository.get(projectId)
                // Parsing is pure JVM work: keep it off the caller's (main) thread.
                val parsed = if (project == null) {
                    null
                } else {
                    withContext(Dispatchers.Default) { SrtFormat.parse(srt) }
                }
                if (project == null || parsed == null || parsed.cues.isEmpty()) {
                    onDone(false)
                    return@withLock
                }
                val updated = CaptionEditor.replaceCues(
                    project.transcript,
                    parsed.cues,
                    TranscriptSource.IMPORTED_SRT
                )
                if (updated == project.transcript) {
                    // Parsed fine but the transcript is unchanged: still a no-op.
                    onDone(false)
                    return@withLock
                }
                val saved = runCatching {
                    repository.upsert(project.copy(transcript = updated))
                }.isSuccess
                onDone(saved)
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

    private companion object {
        /** Idle time before typed cue text is written back to the draft. */
        const val DRAFT_DEBOUNCE_MS = 400L
    }
}