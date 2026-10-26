package com.zaaam.zcut.feature.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.AutoCut
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.media.PcmDecoder
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The mockup's style chips. They have no documented semantics in the artifact,
 * so each one only biases which [AutoCut.Kind] the picker prefers — that mapping
 * is Zcut's own and is stated in the UI as a local heuristic.
 */
enum class ClipperStyle(val label: String, val prefer: AutoCut.Kind?) {
    VLOG("Vlog", null),
    FUNNY("Funny", AutoCut.Kind.LOUD),
    CINEMATIC("Cinematic", AutoCut.Kind.STEADY),
    PRODUCT("Product", AutoCut.Kind.QUIET)
}

data class AiClipperUiState(
    val source: MediaAsset? = null,
    val importing: Boolean = false,
    val analyzing: Boolean = false,
    val creating: Boolean = false,
    val targetMs: Long = AutoCut.TARGETS_MS.first(),
    val targetIsCustom: Boolean = false,
    val style: ClipperStyle = ClipperStyle.VLOG,
    val windows: List<AutoCut.Window> = emptyList(),
    /** How much of the source the analysis actually listened to. */
    val analysedMs: Long = 0L,
    /** True when the source is longer than the local analysis ceiling. */
    val truncated: Boolean = false,
    val notice: String? = null
) {
    val hasSource: Boolean get() = source != null
    val hasResults: Boolean get() = windows.isNotEmpty()

    companion object {
        const val MIN_TARGET_MS = 5_000L
        const val MAX_TARGET_MS = 60_000L
    }
}

/**
 * AI Clipper (F22). Picks one long video, finds its best windows with a local
 * energy heuristic, and turns them into a real draft. Speech recognition and the
 * trained highlight scorer belong to the original pipeline's backend nodes
 * (`ASRNode`, `AIRecognitionHighlightNode`), so they are not faked here.
 *
 * ponytail: the analysis decodes at most [PcmDecoder.MAX_DURATION_MS] of audio
 * (60 s). A longer source is reported as truncated instead of pretending the
 * whole file was inspected. Upgrade path: seek-and-decode in chunks once the
 * decoder grows a range API.
 */
class AiClipperViewModel(
    private val repository: ProjectRepository,
    private val importer: MediaImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiClipperUiState())
    val uiState: StateFlow<AiClipperUiState> = _uiState.asStateFlow()

    /** Copies the picked video into the private workspace and resets results. */
    fun selectSource(uri: Uri) {
        if (_uiState.value.importing) return
        _uiState.update { it.copy(importing = true, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = importer.import(uri)) {
                    is MediaImporter.Result.Success -> {
                        val usable = result.asset.kind == MediaKind.VIDEO
                        _uiState.update {
                            it.copy(
                                importing = false,
                                source = if (usable) result.asset else it.source,
                                windows = emptyList(),
                                analysedMs = 0L,
                                truncated = false,
                                notice = if (usable) {
                                    null
                                } else {
                                    "Pilih berkas video, bukan ${result.asset.displayName}."
                                }
                            )
                        }
                    }

                    is MediaImporter.Result.Failure -> _uiState.update {
                        it.copy(
                            importing = false,
                            notice = "Gagal mengimpor video: ${result.reason.name.lowercase()}."
                        )
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        importing = false,
                        notice = "Gagal mengimpor video: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    fun setTargetMs(targetMs: Long) {
        _uiState.update {
            it.copy(
                targetMs = targetMs.coerceIn(
                    AiClipperUiState.MIN_TARGET_MS,
                    AiClipperUiState.MAX_TARGET_MS
                ),
                targetIsCustom = false,
                windows = emptyList(),
                notice = null
            )
        }
    }

    fun setCustomTarget(enabled: Boolean) {
        _uiState.update { it.copy(targetIsCustom = enabled, windows = emptyList()) }
    }

    fun setStyle(style: ClipperStyle) {
        _uiState.update { it.copy(style = style, windows = emptyList(), notice = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Decodes the source audio and scores windows of the requested length. */
    fun findHighlights() {
        val snapshot = _uiState.value
        val source = snapshot.source ?: return
        if (snapshot.analyzing || snapshot.importing) return
        val path = source.sourcePath ?: return

        _uiState.update { it.copy(analyzing = true, notice = null, windows = emptyList()) }
        viewModelScope.launch {
            try {
                val outcome = withContext(Dispatchers.Default) {
                    val pcm = PcmDecoder.decodeMono(path, PcmDecoder.MAX_DURATION_MS)
                    if (pcm == null || pcm.samples.isEmpty()) return@withContext null
                    val envelope = AutoCut.energyEnvelope(pcm.samples, pcm.sampleRate)
                    Triple(
                        AutoCut.select(
                            AutoCut.candidates(envelope, snapshot.targetMs),
                            prefer = snapshot.style.prefer
                        ),
                        envelope.size * AutoCut.HOP_MS,
                        source.durationMs
                    )
                }
                if (outcome == null) {
                    _uiState.update {
                        it.copy(
                            analyzing = false,
                            notice = "Jalur audio video ini tidak bisa dibaca di perangkat."
                        )
                    }
                    return@launch
                }
                val (windows, analysedMs, sourceMs) = outcome
                _uiState.update {
                    it.copy(
                        analyzing = false,
                        windows = windows,
                        analysedMs = analysedMs,
                        truncated = sourceMs > analysedMs,
                        notice = if (windows.isEmpty()) {
                            "Tidak ada jendela ${snapshot.targetMs / 1_000}s yang cukup panjang " +
                                "di audio yang terbaca."
                        } else {
                            null
                        }
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        analyzing = false,
                        notice = "Analisis gagal: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    /**
     * Builds a draft holding [windows] as clips of the source, stored before the
     * editor opens. [onDone] receives the new draft id, or null on failure.
     */
    fun createDraft(windows: List<AutoCut.Window>, onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        val source = snapshot.source ?: return onDone(null)
        if (windows.isEmpty() || snapshot.creating) return onDone(null)
        _uiState.update { it.copy(creating = true, notice = null) }
        viewModelScope.launch {
            try {
                val taken = repository.summaries().map { it.name }.toSet()
                val name = draftName(source, taken)
                val project = AutoCut.draft(source, windows, name)
                if (project.timeline.tracks.none { it.clips.isNotEmpty() }) {
                    _uiState.update {
                        it.copy(
                            creating = false,
                            notice = "Jendela terpilih tidak muat di durasi video ini."
                        )
                    }
                    return@launch onDone(null)
                }
                repository.upsert(project)
                _uiState.update { it.copy(creating = false) }
                onDone(project.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        creating = false,
                        notice = "Gagal membuat draft: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    /** "long_vlog_0842 · cut", or the next free "Draft N" for a nameless file. */
    private fun draftName(source: MediaAsset, taken: Set<String>): String {
        val base = source.displayName.substringBeforeLast('.').take(32).trim()
        if (base.isEmpty()) return DraftSummaries.nextName(taken)
        val candidate = "$base · cut"
        return if (candidate in taken) DraftSummaries.nextName(taken) else candidate
    }
}