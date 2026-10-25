package com.zaaam.zcut.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.BeatDetector
import com.zaaam.zcut.core.domain.Loudness
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.media.PcmDecoder
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AudioUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val clips: List<Clip> = emptyList(),
    val selectedClipId: String? = null,
    val notFound: Boolean = false,
    val busy: Boolean = false,
    val notice: String? = null,
    val error: String? = null
) {
    val selectedClip: Clip? get() = clips.firstOrNull { it.id == selectedClipId }
}

/**
 * Audio tool read/write layer. Every mutation is a read-modify-write under one
 * mutex so a fast slider drag cannot clobber a newer draft. Loudness and beat
 * analysis decode locally; nothing is uploaded.
 */
class AudioViewModel(
    private val projectId: String,
    initialClipId: String?,
    private val repository: ProjectRepository
) : ViewModel() {

    private val editMutex = Mutex()
    private val selected = MutableStateFlow(initialClipId?.takeIf { it.isNotBlank() })
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AudioUiState> = combine(
        repository.projects,
        selected,
        busy,
        message
    ) { projects, selectedId, isBusy, notice ->
        val project = projects.firstOrNull { it.id == projectId }
        if (project == null) {
            AudioUiState(isLoading = false, notFound = true, busy = isBusy, notice = notice)
        } else {
            val clips = AudioEditor.audioClips(project.timeline)
            val resolved = selectedId?.takeIf { id -> clips.any { it.id == id } }
                ?: clips.firstOrNull()?.id
            AudioUiState(
                isLoading = false,
                project = project,
                clips = clips,
                selectedClipId = resolved,
                busy = isBusy,
                notice = notice
            )
        }
    }
        .catch { error ->
            emit(
                AudioUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat draft"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AudioUiState()
        )

    fun selectClip(clipId: String) {
        selected.value = clipId
        message.value = null
    }

    fun setVolume(volume: Float) = edit { AudioEditor.setVolume(it, id(), volume) }

    fun setFades(fadeInMs: Long, fadeOutMs: Long) =
        edit { AudioEditor.setFades(it, id(), fadeInMs, fadeOutMs) }

    fun setMuted(muted: Boolean) = edit { AudioEditor.setMuted(it, id(), muted) }

    fun setLooping(looping: Boolean) = edit { AudioEditor.setLooping(it, id(), looping) }

    fun setPitch(pitch: Float) = edit { AudioEditor.setPitch(it, id(), pitch) }

    fun deleteSelected() {
        val clipId = id()
        if (clipId.isBlank()) return
        selected.value = null
        edit { TimelineEditor.removeClip(it, clipId) }
    }

    /** Snapping is only meaningful with a detected grid. */
    fun setSnapToBeat(enabled: Boolean) {
        editProject { project ->
            if (enabled && project.settings.beatGridMs.isEmpty()) {
                message.value = "Deteksi beat dulu sebelum mengaktifkan snap."
                project
            } else {
                project.copy(settings = project.settings.copy(snapToBeat = enabled))
            }
        }
    }

    /** Measures the selected clip's level and boosts/trims it to ≈ -16 dBFS. */
    fun normalizeLoudness() {
        val clip = uiState.value.selectedClip ?: return
        val path = assetPath(clip) ?: return showMessage("File audio tidak ditemukan di perangkat.")
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) { PcmDecoder.decodeMono(path) }
                if (decoded == null) {
                    showMessage("Audio tidak bisa didekode di perangkat ini.")
                    return@launch
                }
                val gain = Loudness.normalizeGain(decoded.samples)
                val target = (clip.audio.volume * gain).coerceIn(0f, AudioEditor.MAX_VOLUME)
                if (applyEdit { AudioEditor.setVolume(it, clip.id, target) }) {
                    showMessage("Volume dinormalisasi ke ≈ ${Loudness.TARGET_DBFS.toInt()} dBFS.")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                showMessage("Normalisasi gagal: " + (error.message ?: "kesalahan tak dikenal"))
            } finally {
                busy.value = false
            }
        }
    }

    /** Detects beat markers on the selected clip and stores them on the draft. */
    fun detectBeats() {
        val clip = uiState.value.selectedClip ?: return
        val path = assetPath(clip) ?: return showMessage("File audio tidak ditemukan di perangkat.")
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) { PcmDecoder.decodeMono(path) }
                if (decoded == null) {
                    showMessage("Audio tidak bisa didekode di perangkat ini.")
                    return@launch
                }
                val beats = BeatDetector.detect(decoded.samples, decoded.sampleRate)
                if (beats.isEmpty()) {
                    showMessage("Tidak ada beat yang terdeteksi pada klip ini.")
                    return@launch
                }
                val bpm = BeatDetector.bpm(beats)
                val stored = applyEdit { project ->
                    project.copy(
                        settings = project.settings.copy(
                            beatGridMs = AudioEditor.sanitizeBeats(beats),
                            snapToBeat = true
                        )
                    )
                }
                if (stored) {
                    showMessage(
                        buildString {
                            append(beats.size)
                            append(" beat terdeteksi")
                            if (bpm != null) append(" (≈ $bpm BPM)")
                            append(". Snap ke beat aktif.")
                        }
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                showMessage("Deteksi beat gagal: " + (error.message ?: "kesalahan tak dikenal"))
            } finally {
                busy.value = false
            }
        }
    }

    /** Adds a finished voiceover recording to the timeline at [atMs]. */
    fun insertRecording(file: File, durationMs: Long, atMs: Long) {
        val asset = MediaAsset(
            id = UUID.randomUUID().toString(),
            uri = "",
            displayName = "Voiceover",
            mimeType = "audio/mp4",
            kind = MediaKind.AUDIO,
            durationMs = durationMs,
            sizeBytes = file.length(),
            localPath = file.absolutePath
        )
        val clipId = UUID.randomUUID().toString()
        viewModelScope.launch {
            if (applyEdit { AudioEditor.insertAudio(it, asset, atMs, clipId) }) {
                selected.value = clipId
                showMessage("Voiceover ditambahkan ke timeline.")
            } else {
                showMessage("Voiceover gagal ditambahkan.")
            }
        }
    }

    fun clearNotice() {
        message.value = null
    }

    private fun id(): String = uiState.value.selectedClipId.orEmpty()

    private fun assetPath(clip: Clip): String? {
        val assetId = clip.assetId ?: return null
        val path = uiState.value.project?.assets
            ?.firstOrNull { it.id == assetId }
            ?.sourcePath
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return path.takeIf { File(it).isFile }
    }

    private fun edit(transform: (Timeline) -> Timeline) {
        if (id().isBlank()) return
        viewModelScope.launch {
            applyEdit { project -> project.copy(timeline = transform(project.timeline)) }
        }
    }

    private fun editProject(transform: (Project) -> Project) {
        viewModelScope.launch { applyEdit(transform) }
    }

    private fun showMessage(text: String) {
        message.value = text
    }

    /** One serialised read-modify-write; returns true when something was stored. */
    private suspend fun applyEdit(transform: (Project) -> Project): Boolean =
        editMutex.withLock {
            val current = repository.get(projectId) ?: return@withLock false
            val updated = transform(current)
            if (updated == current) return@withLock false
            runCatching { repository.upsert(updated) }.isSuccess
        }
}
