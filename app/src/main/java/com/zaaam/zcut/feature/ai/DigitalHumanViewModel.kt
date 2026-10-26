package com.zaaam.zcut.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.domain.DigitalHumanAvatar
import com.zaaam.zcut.core.domain.DigitalHumanCatalog
import com.zaaam.zcut.core.domain.DigitalHumanPlan
import com.zaaam.zcut.core.domain.DigitalHumanRequest
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TtsScript
import com.zaaam.zcut.core.media.AudioDuration
import com.zaaam.zcut.core.media.DigitalHumanBackgrounds
import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Transcript
import com.zaaam.zcut.core.model.TranscriptSource
import com.zaaam.zcut.core.render.SpeechEngine
import com.zaaam.zcut.core.render.SpeechVoice
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DigitalHumanUiState(
    val avatars: List<DigitalHumanAvatar> = DigitalHumanCatalog.avatars,
    val selectedAvatarId: String = DigitalHumanCatalog.default.id,
    /** The mockup's example script, kept as an editable starting point. */
    val script: String = SAMPLE_SCRIPT,
    val voices: List<SpeechVoice> = emptyList(),
    val loadingVoices: Boolean = true,
    val selectedVoiceId: String? = null,
    val subtitles: Boolean = true,
    val busy: Boolean = false,
    /** Set after the local preparation draft is written. */
    val createdDraftId: String? = null,
    val createdDraftName: String? = null,
    val notice: String? = null
) {
    val avatar: DigitalHumanAvatar get() = DigitalHumanCatalog.byId(selectedAvatarId)

    val selectedVoice: SpeechVoice?
        get() = voices.firstOrNull { it.id == selectedVoiceId } ?: voices.firstOrNull()

    val canGenerate: Boolean get() = script.isNotBlank() && !busy

    companion object {
        const val SAMPLE_SCRIPT =
            "Welcome back to the channel. Today we are cutting a night-city short " +
                "in under a minute."
    }
}

/**
 * Digital Human (F25). Rendering the avatar and the lip sync needs a model
 * backend, so this screen only prepares what really runs on device: a TTS voice
 * track, a placeholder background still, and estimated subtitle lines packed
 * into a new draft. The avatar render step is stated as missing, never faked.
 */
class DigitalHumanViewModel(
    private val repository: ProjectRepository,
    private val speechEngine: SpeechEngine,
    private val ttsCacheDir: File,
    private val backgroundDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalHumanUiState())
    val uiState: StateFlow<DigitalHumanUiState> = _uiState.asStateFlow()

    init {
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

    fun setAvatar(id: String) {
        _uiState.update { it.copy(selectedAvatarId = DigitalHumanCatalog.byId(id).id).reset() }
    }

    fun setScript(text: String) {
        _uiState.update {
            it.copy(script = text.take(DigitalHumanRequest.MAX_SCRIPT)).reset()
        }
    }

    fun setVoice(id: String) {
        _uiState.update { it.copy(selectedVoiceId = id).reset() }
    }

    fun setSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(subtitles = enabled).reset() }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Speaks the script with the device engine; no file is written. */
    fun preview() {
        val snapshot = _uiState.value
        val text = TtsScript.sanitize(snapshot.script)
        if (text.isBlank()) return showNotice("Tulis skrip dulu.")
        viewModelScope.launch {
            val spoken = runCatching {
                speechEngine.speak(text, snapshot.selectedVoice?.id, 1f, 1f)
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
     * Prepares the local half: synthesise the voice, render the background
     * still, and (optionally) add estimated subtitle lines, then save one draft.
     * The avatar/lip-sync render is explicitly out of scope here.
     */
    fun generate(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.busy) return onDone(null)
        val request = DigitalHumanRequest(
            avatarId = snapshot.selectedAvatarId,
            script = snapshot.script,
            voiceId = snapshot.selectedVoice?.id,
            subtitles = snapshot.subtitles
        ).sanitized()
        if (!request.isReady) {
            showNotice("Tulis skrip dulu.")
            return onDone(null)
        }

        _uiState.update { it.copy(busy = true, notice = null, createdDraftId = null) }
        viewModelScope.launch {
            try {
                ttsCacheDir.mkdirs()
                val voiceFile = File(
                    ttsCacheDir,
                    TtsScript.fileName(request.script, request.voiceId, request.rate, request.pitch)
                )
                val cached = voiceFile.isFile && voiceFile.length() > 0L
                val synthesised = cached || runCatching {
                    speechEngine.synthesize(
                        text = request.script,
                        voiceId = request.voiceId,
                        rate = request.rate,
                        pitch = request.pitch,
                        outputPath = voiceFile.absolutePath
                    )
                }.getOrDefault(false)
                if (!synthesised) {
                    _uiState.update {
                        it.copy(
                            busy = false,
                            notice = "Gagal membuat suara. Mesin TTS perangkat mungkin " +
                                "tidak tersedia."
                        )
                    }
                    return@launch onDone(null)
                }

                val durationMs = withContext(Dispatchers.IO) {
                    AudioDuration.of(voiceFile.absolutePath)
                } ?: TtsScript.estimateDurationMs(request.script)
                val avatar = DigitalHumanCatalog.byId(request.avatarId)
                val backgroundFile = File(
                    backgroundDir,
                    "dh-${avatar.id}-${durationMs}.png"
                )
                val backgroundReady = (backgroundFile.isFile && backgroundFile.length() > 0L) ||
                    DigitalHumanBackgrounds.render(avatar, backgroundFile)
                if (!backgroundReady) {
                    _uiState.update {
                        it.copy(
                            busy = false,
                            notice = "Gagal membuat background lokal."
                        )
                    }
                    return@launch onDone(null)
                }

                val now = System.currentTimeMillis()
                val background = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    displayName = "Digital Human · ${avatar.name}",
                    mimeType = "image/png",
                    kind = MediaKind.IMAGE,
                    durationMs = durationMs,
                    width = DigitalHumanBackgrounds.WIDTH,
                    height = DigitalHumanBackgrounds.HEIGHT,
                    sizeBytes = backgroundFile.length(),
                    createdAtMs = now,
                    localPath = backgroundFile.absolutePath
                )
                val voice = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    displayName = "Avatar voice · ${avatar.name}",
                    mimeType = "audio/wav",
                    kind = MediaKind.AUDIO,
                    durationMs = durationMs,
                    sizeBytes = voiceFile.length(),
                    createdAtMs = now,
                    localPath = voiceFile.absolutePath
                )

                val taken = repository.summaries().map { it.name }.toSet()
                val name = "Avatar · ${avatar.name}"
                    .let { if (it in taken) DraftSummaries.nextName(taken) else it }
                var project = ProjectFactory.fromAssets(name, listOf(background))
                project = AudioEditor.insertAudio(
                    project = project,
                    asset = voice,
                    atMs = 0L,
                    clipId = UUID.randomUUID().toString()
                )
                if (request.subtitles) {
                    project = project.copy(
                        transcript = buildTranscript(project.transcript, request, durationMs)
                    )
                }
                repository.upsert(project)
                _uiState.update {
                    it.copy(
                        busy = false,
                        createdDraftId = project.id,
                        createdDraftName = project.name,
                        notice = if (cached) {
                            "Draft persiapan dibuat dari suara cache. Render avatar tetap " +
                                "BUTUH BACKEND."
                        } else {
                            "Draft persiapan dibuat. Render avatar tetap BUTUH BACKEND."
                        }
                    )
                }
                onDone(project.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal menyiapkan draft: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    private fun buildTranscript(
        transcript: Transcript,
        request: DigitalHumanRequest,
        durationMs: Long
    ): Transcript {
        val lines = DigitalHumanPlan.captions(request.script, durationMs)
        if (lines.isEmpty()) return transcript
        val language = _uiState.value.selectedVoice?.locale
            ?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().language
        val style = CaptionEditor.defaultStyle()
        val cues = lines.map { line ->
            CaptionCue(
                id = UUID.randomUUID().toString(),
                startMs = line.startMs,
                endMs = line.endMs,
                text = line.text,
                language = language,
                style = style
            )
        }
        return CaptionEditor.replaceCues(
            transcript,
            cues,
            TranscriptSource.SYNTHESIZED,
            language = language
        )
    }

    private fun showNotice(text: String) {
        _uiState.update { it.copy(notice = text) }
    }

    /** Input changes invalidate the previous result card. */
    private fun DigitalHumanUiState.reset(): DigitalHumanUiState =
        copy(createdDraftId = null, createdDraftName = null, notice = null)
}