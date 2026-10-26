package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.MusicCatalog
import com.zaaam.zcut.core.domain.MusicGenerator
import com.zaaam.zcut.core.domain.MusicMood
import com.zaaam.zcut.core.domain.MusicStyle
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A loop that has been rendered to disk and can be dropped on the timeline. */
data class AiMusicResult(
    val fileName: String,
    val path: String,
    val durationMs: Long,
    val bpm: Int,
    val keyLabel: String,
    val sizeBytes: Long
)

data class AiMusicUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val notFound: Boolean = false,
    /** True when the hub opened the panel with no draft to insert into. */
    val detached: Boolean = false,
    val error: String? = null,
    val prompt: String = DEFAULT_PROMPT,
    val mood: MusicMood = MusicMood.LOFI,
    val durationMs: Long = DEFAULT_DURATION_MS,
    val busy: Boolean = false,
    val result: AiMusicResult? = null,
    val notice: String? = null
) {
    /** The readout in the mockup's panel mirrors the selected mood. */
    val style: MusicStyle get() = MusicCatalog.style(mood)

    companion object {
        const val DEFAULT_PROMPT = "warm lo-fi beat for a night drive, soft keys, vinyl texture"
        const val DEFAULT_DURATION_MS = 24_000L
        const val MAX_PROMPT = 240
    }
}

/**
 * Local, procedural music generator. The prompt steers nothing but the
 * deterministic seed (there is no model on device — a real generative model is
 * marked BUTUH BACKEND), so the same prompt and mood always render the same
 * loop. Nothing is uploaded and nothing is licensed: the file is written to the
 * app's private workspace and inserted as a normal audio clip.
 */
class AiMusicViewModel(
    private val projectId: String?,
    private val atMs: Long,
    private val repository: ProjectRepository,
    private val musicDir: File
) : ViewModel() {

    private val editMutex = Mutex()
    private val _uiState = MutableStateFlow(AiMusicUiState())
    val uiState: StateFlow<AiMusicUiState> = _uiState.asStateFlow()

    init {
        if (projectId == null) {
            // Opened from the AI Studio hub: the generator is fully usable, only
            // the timeline insert has nowhere to go.
            _uiState.update { it.copy(isLoading = false, detached = true) }
        } else {
            viewModelScope.launch {
                repository.projects
                    .catch { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Gagal memuat draft"
                            )
                        }
                    }
                    .collect { projects ->
                        val project = projects.firstOrNull { it.id == projectId }
                        _uiState.update {
                            it.copy(isLoading = false, project = project, notFound = project == null)
                        }
                    }
            }
        }
    }

    fun setPrompt(text: String) {
        _uiState.update { it.copy(prompt = text.take(AiMusicUiState.MAX_PROMPT), result = null) }
    }

    fun setMood(mood: MusicMood) {
        _uiState.update { it.copy(mood = mood, result = null) }
    }

    fun setDurationMs(durationMs: Long) {
        _uiState.update {
            it.copy(
                durationMs = durationMs.coerceIn(
                    MusicGenerator.MIN_DURATION_MS,
                    MusicGenerator.MAX_DURATION_MS
                ),
                result = null
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Renders the loop on a background dispatcher; the file never leaves the app. */
    fun generate() {
        val snapshot = _uiState.value
        if (snapshot.busy) return
        val style = MusicCatalog.style(snapshot.mood)
        val duration = snapshot.durationMs.coerceIn(
            MusicGenerator.MIN_DURATION_MS,
            MusicGenerator.MAX_DURATION_MS
        )
        val seed = seedFor(snapshot.prompt, snapshot.mood)

        _uiState.update { it.copy(busy = true, result = null, notice = null) }
        viewModelScope.launch {
            try {
                val wav = withContext(Dispatchers.Default) {
                    MusicGenerator.wav(style, duration, seed)
                }
                val base = MusicGenerator.fileName(style, duration, seed)
                val file = withContext(Dispatchers.IO) {
                    runCatching {
                        MusicGenerator.writeUnique(musicDir, base, wav)
                    }.getOrNull()
                }
                if (file == null) {
                    _uiState.update {
                        it.copy(busy = false, notice = "Gagal menulis berkas loop.")
                    }
                    return@launch
                }
                val result = AiMusicResult(
                    fileName = file.name,
                    path = file.absolutePath,
                    durationMs = duration,
                    bpm = style.bpm,
                    keyLabel = style.keyLabel,
                    sizeBytes = file.length()
                )
                _uiState.update {
                    it.copy(
                        busy = false,
                        result = result,
                        notice = "Loop ${result.durationMs / 1_000}s dibuat lokal (${result.bpm} BPM)."
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal membuat loop: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    /**
     * Adds the rendered loop to the timeline at the playhead the panel opened
     * with, and stores the tempo as the draft's snap grid so clip edges can
     * follow the beat. [onDone] receives whether something was stored.
     */
    fun insert(onDone: (Boolean) -> Unit = {}) {
        val snapshot = _uiState.value
        val result = snapshot.result ?: return onDone(false)
        val draftId = projectId ?: return onDone(false)
        if (snapshot.busy) return
        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val asset = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    displayName = result.fileName,
                    mimeType = "audio/wav",
                    kind = MediaKind.AUDIO,
                    durationMs = result.durationMs,
                    sizeBytes = result.sizeBytes,
                    localPath = result.path
                )
                val stored = editMutex.withLock {
                    val project = repository.get(draftId) ?: return@withLock false
                    val inserted = AudioEditor.insertAudio(
                        project = project,
                        asset = asset,
                        atMs = atMs,
                        clipId = UUID.randomUUID().toString()
                    )
                    // A grid the draft already carries (beat detection, a template)
                    // outranks the loop's tempo, so it is never overwritten.
                    val hadGrid = inserted.settings.beatGridMs.isNotEmpty()
                    val withGrid = if (hadGrid) {
                        inserted
                    } else {
                        inserted.copy(
                            settings = inserted.settings.copy(
                                beatGridMs = AudioEditor.sanitizeBeats(
                                    MusicGenerator.beatGrid(result.bpm, result.durationMs)
                                ),
                                snapToBeat = true
                            )
                        )
                    }
                    withGrid != project && runCatching { repository.upsert(withGrid) }.isSuccess
                }
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = if (stored) {
                            "Loop masuk ke timeline."
                        } else {
                            "Loop siap, tapi gagal masuk ke timeline."
                        }
                    )
                }
                onDone(stored)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal menambah loop: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(false)
            }
        }
    }

    /**
     * A stable seed per (prompt, mood): `String.hashCode` is specified by the
     * language, so the same input renders the same audio on any JVM.
     */
    private fun seedFor(prompt: String, mood: MusicMood): Int =
        prompt.hashCode() * 31 + mood.ordinal
}