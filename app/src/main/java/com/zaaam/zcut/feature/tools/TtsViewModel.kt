package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.TtsScript
import com.zaaam.zcut.core.media.AudioDuration
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.render.SpeechEngine
import com.zaaam.zcut.core.render.SpeechVoice
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TtsUiState(
    val project: Project? = null,
    val notFound: Boolean = false,
    val loadingVoices: Boolean = true,
    val voices: List<SpeechVoice> = emptyList(),
    val selectedVoiceId: String? = null,
    val script: String = "",
    val rate: Float = 1f,
    val pitch: Float = 1f,
    val isBusy: Boolean = false,
    val generated: Boolean = false,
    val notice: String? = null,
    val error: String? = null
) {
    val selectedVoice: SpeechVoice?
        get() = voices.firstOrNull { it.id == selectedVoiceId } ?: voices.firstOrNull()

    val canGenerate: Boolean get() = script.isNotBlank() && !isBusy
}

/**
 * System TextToSpeech screen. Speech is synthesised on device and cached by a
 * content key, so the same line/voice/rate/pitch is never rendered twice.
 * Voice cloning needs a server model, so it is marked as such instead of faked.
 */
class TtsViewModel(
    private val projectId: String,
    private val atMs: Long,
    private val repository: ProjectRepository,
    private val speechEngine: SpeechEngine,
    private val cacheDir: File
) : ViewModel() {

    private val writeMutex = Mutex()
    private val _uiState = MutableStateFlow(TtsUiState())
    val uiState: StateFlow<TtsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.projects
                .catch { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Gagal memuat draft")
                    }
                }
                .collect { projects ->
                    val project = projects.firstOrNull { it.id == projectId }
                    _uiState.update { it.copy(project = project, notFound = project == null) }
                }
        }
        viewModelScope.launch {
            val voices = runCatching { speechEngine.voices() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    loadingVoices = false,
                    voices = voices,
                    selectedVoiceId = it.selectedVoiceId ?: voices.firstOrNull()?.id
                )
            }
        }
    }

    fun setScript(text: String) {
        _uiState.update { it.copy(script = text.take(TtsScript.MAX_LENGTH), generated = false) }
    }

    fun setVoice(voiceId: String) {
        _uiState.update { it.copy(selectedVoiceId = voiceId, generated = false) }
    }

    fun setRate(rate: Float) {
        _uiState.update { it.copy(rate = rate.coerceIn(MIN_RATE, MAX_RATE), generated = false) }
    }

    fun setPitch(pitch: Float) {
        _uiState.update {
            it.copy(pitch = pitch.coerceIn(MIN_PITCH, MAX_PITCH), generated = false)
        }
    }

    fun preview() {
        val snapshot = _uiState.value
        val text = TtsScript.sanitize(snapshot.script)
        if (text.isBlank()) return showNotice("Tulis skrip dulu.")
        viewModelScope.launch {
            val spoken = runCatching {
                speechEngine.speak(text, snapshot.selectedVoice?.id, snapshot.rate, snapshot.pitch)
            }.getOrDefault(false)
            showNotice(
                if (spoken) "Memutar pratinjau suara." else "Mesin TTS perangkat tidak tersedia."
            )
        }
    }

    fun stopPreview() {
        runCatching { speechEngine.stop() }
    }

    /**
     * Synthesises (or reuses a cached file for) the current script and inserts
     * it as an audio clip at the playhead the screen was opened with.
     */
    fun generate() {
        val snapshot = _uiState.value
        if (snapshot.isBusy) return
        val text = TtsScript.sanitize(snapshot.script)
        if (text.isBlank()) return showNotice("Tulis skrip dulu.")
        val voiceId = snapshot.selectedVoice?.id

        _uiState.update { it.copy(isBusy = true, notice = null) }
        viewModelScope.launch {
            try {
                cacheDir.mkdirs()
                val file = File(cacheDir, TtsScript.fileName(text, voiceId, snapshot.rate, snapshot.pitch))
                val cached = file.isFile && file.length() > 0L
                val ready = cached || runCatching {
                    speechEngine.synthesize(
                        text = text,
                        voiceId = voiceId,
                        rate = snapshot.rate,
                        pitch = snapshot.pitch,
                        outputPath = file.absolutePath
                    )
                }.getOrDefault(false)
                if (!ready) {
                    // Reset the busy flag here too: the success path resets it
                    // later, so returning early without it would leave the button
                    // disabled and labelled "Membuat suara..." forever.
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            notice = "Gagal membuat suara. Mesin TTS perangkat mungkin " +
                                "tidak tersedia."
                        )
                    }
                    return@launch
                }

                val duration = AudioDuration.of(file.absolutePath)
                    ?: TtsScript.estimateDurationMs(text)
                val asset = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    displayName = "TTS · " + text.take(24),
                    mimeType = "audio/wav",
                    kind = MediaKind.AUDIO,
                    durationMs = duration,
                    sizeBytes = file.length(),
                    localPath = file.absolutePath
                )
                val stored = writeMutex.withLock {
                    val project = repository.get(projectId) ?: return@withLock false
                    val updated = AudioEditor.insertAudio(
                        project = project,
                        asset = asset,
                        atMs = atMs,
                        clipId = UUID.randomUUID().toString()
                    )
                    updated != project && runCatching { repository.upsert(updated) }.isSuccess
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        generated = stored,
                        notice = if (stored) {
                            if (cached) "Suara dari cache ditambahkan ke timeline."
                            else "Suara dibuat dan ditambahkan ke timeline."
                        } else {
                            "Suara dibuat, tapi gagal masuk ke timeline."
                        }
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        notice = "Gagal membuat suara: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun showNotice(text: String) {
        _uiState.update { it.copy(notice = text) }
    }

    private companion object {
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2f
    }
}
