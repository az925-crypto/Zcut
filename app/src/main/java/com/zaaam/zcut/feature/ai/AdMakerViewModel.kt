package com.zaaam.zcut.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AdBrief
import com.zaaam.zcut.core.domain.AdCta
import com.zaaam.zcut.core.domain.AdScene
import com.zaaam.zcut.core.domain.AdScript
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.MusicCatalog
import com.zaaam.zcut.core.domain.MusicGenerator
import com.zaaam.zcut.core.domain.MusicMood
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TextEditor
import com.zaaam.zcut.core.domain.TtsScript
import com.zaaam.zcut.core.media.AdBackdrop
import com.zaaam.zcut.core.media.AudioDuration
import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.TextAlignment
import com.zaaam.zcut.core.model.TextConfig
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

data class AdMakerUiState(
    val script: String = AdBrief.DEFAULT_SCRIPT,
    val product: String = AdBrief.DEFAULT_PRODUCT,
    val cta: AdCta = AdCta.SHOP_NOW,
    val voiceover: Boolean = true,
    val music: Boolean = false,
    val voices: List<SpeechVoice> = emptyList(),
    val selectedVoiceId: String? = null,
    val busy: Boolean = false,
    val notice: String? = null,
    val createdDraftId: String? = null,
    val createdDraftName: String? = null
) {
    val scenes: List<AdScene> get() = AdScript.scenes(script)
    val totalDurationMs: Long get() = AdScript.totalDurationMs(scenes)
    val ready: Boolean get() = !busy && scenes.isNotEmpty()
    val selectedVoice: SpeechVoice?
        get() = voices.firstOrNull { it.id == selectedVoiceId } ?: voices.firstOrNull()
}

/**
 * Ad Maker (F31), local half.
 *
 * Script → scenes → device TTS voiceover (or a local procedural music bed) →
 * captions → CTA overlay → one real draft. The generative "smart ad", product
 * album AI and pixel/report panels need a backend and are labelled as such; the
 * screen never pretends to call one.
 *
 * A draft has one audio track, so voiceover and music are mutually exclusive;
 * the switches explain the swap instead of silently dropping a bed.
 */
class AdMakerViewModel(
    private val repository: ProjectRepository,
    private val speechEngine: SpeechEngine,
    private val ttsCacheDir: File,
    private val backgroundDir: File,
    private val musicDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdMakerUiState())
    val uiState: StateFlow<AdMakerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val voices = runCatching { speechEngine.voices() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    voices = voices,
                    selectedVoiceId = it.selectedVoiceId ?: voices.firstOrNull()?.id
                )
            }
        }
    }

    fun setScript(text: String) {
        _uiState.update { it.copy(script = text).reset() }
    }

    fun setProduct(text: String) {
        _uiState.update {
            it.copy(product = text.take(AdScript.MAX_PRODUCT_CHARS)).reset()
        }
    }

    fun setCta(cta: AdCta) {
        _uiState.update { it.copy(cta = cta).reset() }
    }

    fun setVoice(id: String) {
        _uiState.update { it.copy(selectedVoiceId = id).reset() }
    }

    fun setVoiceover(enabled: Boolean) {
        _uiState.update {
            it.copy(
                voiceover = enabled,
                music = if (enabled) false else it.music,
                notice = if (enabled && it.music) {
                    "Voiceover menggantikan musik: satu track audio per draft."
                } else {
                    null
                }
            ).reset()
        }
    }

    fun setMusic(enabled: Boolean) {
        _uiState.update {
            it.copy(
                music = enabled,
                voiceover = if (enabled) false else it.voiceover,
                notice = if (enabled && it.voiceover) {
                    "Musik menggantikan voiceover: satu track audio per draft."
                } else {
                    null
                }
            ).reset()
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Speaks the whole script with the device engine; no file is written. */
    fun preview() {
        val snapshot = _uiState.value
        val text = AdScript.lines(snapshot.script).joinToString(" ")
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

    fun generate(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.busy) return onDone(null)
        val brief = AdBrief(
            script = snapshot.script,
            product = snapshot.product,
            cta = snapshot.cta,
            voiceover = snapshot.voiceover,
            music = snapshot.music
        ).sanitized()
        val baseScenes = AdScript.scenes(brief.script)
        if (baseScenes.isEmpty()) {
            showNotice("Tulis minimal satu baris skrip.")
            return onDone(null)
        }

        _uiState.update { it.copy(busy = true, notice = null, createdDraftId = null) }
        viewModelScope.launch {
            try {
                ttsCacheDir.mkdirs()
                val voiceFiles = mutableListOf<File>()
                val measured = mutableListOf<Long>()
                if (brief.voiceover) {
                    for (scene in baseScenes) {
                        val file = File(
                            ttsCacheDir,
                            TtsScript.fileName(scene.text, snapshot.selectedVoice?.id, 1f, 1f)
                        )
                        val ready = (file.isFile && file.length() > 0L) || runCatching {
                            speechEngine.synthesize(
                                text = scene.text,
                                voiceId = snapshot.selectedVoice?.id,
                                rate = 1f,
                                pitch = 1f,
                                outputPath = file.absolutePath
                            )
                        }.getOrDefault(false)
                        if (!ready) {
                            _uiState.update {
                                it.copy(
                                    busy = false,
                                    notice = "Gagal membuat voiceover. Mesin TTS perangkat " +
                                        "mungkin tidak tersedia; matikan voiceover untuk " +
                                        "memakai musik lokal."
                                )
                            }
                            return@launch onDone(null)
                        }
                        val durationMs = withContext(Dispatchers.IO) {
                            AudioDuration.of(file.absolutePath)
                        } ?: TtsScript.estimateDurationMs(scene.text)
                        voiceFiles += file
                        measured += durationMs
                    }
                }

                val scenes = AdScript.scenes(
                    brief.script,
                    if (brief.voiceover) measured else null
                )
                val speechMs = AdScript.totalDurationMs(scenes).coerceAtLeast(1_000L)

                var musicAsset: MediaAsset? = null
                var musicDurationMs = 0L
                if (brief.music) {
                    val style = MusicCatalog.style(MusicMood.CINEMATIC)
                    val loopMs = speechMs.coerceIn(
                        MusicGenerator.MIN_DURATION_MS,
                        MusicGenerator.MAX_DURATION_MS
                    )
                    val seed = (brief.product + brief.script).hashCode()
                    val file = withContext(Dispatchers.IO) {
                        MusicGenerator.writeUnique(
                            dir = musicDir,
                            base = MusicGenerator.fileName(style, loopMs, seed),
                            bytes = MusicGenerator.wav(style, loopMs, seed)
                        )
                    }
                    musicDurationMs = loopMs
                    musicAsset = MediaAsset(
                        id = UUID.randomUUID().toString(),
                        uri = "",
                        displayName = file.name,
                        mimeType = "audio/wav",
                        kind = MediaKind.AUDIO,
                        durationMs = loopMs,
                        sizeBytes = file.length(),
                        createdAtMs = System.currentTimeMillis(),
                        localPath = file.absolutePath
                    )
                }

                val projectDurationMs = maxOf(speechMs, musicDurationMs).coerceAtLeast(1_000L)
                val backdropFile = File(
                    backgroundDir,
                    "ad-${projectDurationMs}-${(brief.product + brief.cta.label).hashCode()}.png"
                )
                val backdropReady =
                    (backdropFile.isFile && backdropFile.length() > 0L) ||
                        AdBackdrop.render(
                            product = brief.product.ifBlank { "Product ad" },
                            cta = brief.cta.label,
                            output = backdropFile
                        )
                if (!backdropReady) {
                    _uiState.update {
                        it.copy(busy = false, notice = "Gagal merender kartu produk lokal.")
                    }
                    return@launch onDone(null)
                }

                val backdrop = MediaAsset(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    displayName = "Ad card · ${brief.product.ifBlank { "Product ad" }}",
                    mimeType = "image/png",
                    kind = MediaKind.IMAGE,
                    durationMs = projectDurationMs,
                    width = AdBackdrop.WIDTH,
                    height = AdBackdrop.HEIGHT,
                    sizeBytes = backdropFile.length(),
                    createdAtMs = System.currentTimeMillis(),
                    localPath = backdropFile.absolutePath
                )

                val taken = repository.summaries().map { it.name }.toSet()
                val preferred = "Ad · " + brief.product.ifBlank { "Script" }
                val name = if (preferred in taken) DraftSummaries.nextName(taken) else preferred
                var project = ProjectFactory.fromAssets(name, listOf(backdrop))

                if (brief.voiceover) {
                    scenes.forEachIndexed { index, scene ->
                        val file = voiceFiles.getOrNull(index) ?: return@forEachIndexed
                        val asset = MediaAsset(
                            id = UUID.randomUUID().toString(),
                            uri = "",
                            displayName = "Ad voice ${index + 1}",
                            mimeType = "audio/wav",
                            kind = MediaKind.AUDIO,
                            durationMs = scene.durationMs,
                            sizeBytes = file.length(),
                            createdAtMs = System.currentTimeMillis(),
                            localPath = file.absolutePath
                        )
                        project = AudioEditor.insertAudio(project, asset, scene.startMs)
                    }
                } else {
                    musicAsset?.let { asset ->
                        project = AudioEditor.insertAudio(project, asset, 0L)
                    }
                }

                project = project.copy(
                    transcript = captions(
                        transcript = project.transcript,
                        scenes = scenes,
                        locale = snapshot.selectedVoice?.locale
                    )
                )

                val last = scenes.last()
                val ctaConfig = TextConfig(
                    text = brief.cta.label,
                    fontSizeSp = 56f,
                    colorArgb = 0xFF0B1020,
                    alignment = TextAlignment.CENTER,
                    backgroundColorArgb = 0xFFC8F04A
                )
                project = project.copy(
                    timeline = TextEditor.addText(
                        timeline = project.timeline,
                        config = ctaConfig,
                        atMs = last.startMs,
                        durationMs = last.durationMs
                    )
                )

                repository.upsert(project)
                _uiState.update {
                    it.copy(
                        busy = false,
                        createdDraftId = project.id,
                        createdDraftName = project.name,
                        notice = when {
                            brief.voiceover -> "Draft ad dibuat: ${scenes.size} scene, " +
                                "voiceover lokal + caption + CTA."
                            brief.music -> "Draft ad dibuat: ${scenes.size} scene, musik " +
                                "lokal + caption + CTA."
                            else -> "Draft ad dibuat: ${scenes.size} scene, caption + CTA " +
                                "(tanpa audio)."
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
                        notice = "Gagal membuat draft ad: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    private fun captions(
        transcript: Transcript,
        scenes: List<AdScene>,
        locale: String?
    ): Transcript {
        if (scenes.isEmpty()) return transcript
        val language = locale?.takeIf { it.isNotBlank() } ?: Locale.getDefault().language
        val style = CaptionEditor.defaultStyle()
        val cues = scenes.map { scene ->
            CaptionCue(
                id = UUID.randomUUID().toString(),
                startMs = scene.startMs,
                endMs = scene.endMs,
                text = scene.text,
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
    private fun AdMakerUiState.reset(): AdMakerUiState =
        copy(createdDraftId = null, createdDraftName = null)
}
