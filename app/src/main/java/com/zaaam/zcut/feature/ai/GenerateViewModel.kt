package com.zaaam.zcut.feature.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.GenerationHistoryRepository
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.GenerationEntry
import com.zaaam.zcut.core.domain.GenerationRequest
import com.zaaam.zcut.core.domain.GenerationStyle
import com.zaaam.zcut.core.domain.GenerationType
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerateUiState(
    /** The mockup's example brief, kept as an editable starting point. */
    val prompt: String = SAMPLE_PROMPT,
    val type: GenerationType = GenerationType.IMAGE,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val style: GenerationStyle = GenerationStyle.CINEMATIC,
    /** Revealed by "Generate"; explains that a model backend is missing. */
    val showBackendPanel: Boolean = false,
    val importing: Boolean = false,
    val creatingDraft: Boolean = false,
    val history: List<GenerationEntry> = emptyList(),
    val notice: String? = null
) {
    val request: GenerationRequest
        get() = GenerationRequest(
            type = type,
            prompt = prompt,
            aspectRatio = aspectRatio,
            style = style
        ).sanitized()

    val canGenerate: Boolean get() = request.isReady && !importing
    val canImport: Boolean get() = !importing && !creatingDraft

    companion object {
        const val SAMPLE_PROMPT =
            "a rain-soaked Tokyo alley at midnight, neon reflections, cinematic 35mm"
    }
}

/**
 * Generate (F24). Every generative model in the artifact lives behind
 * `AigcGenerateClient`/`AigcWorkflowClient` and a remote task, so this screen
 * never pretends to render: "Generate" reveals the honest backend panel, and the
 * only real work is importing a result from the gallery into a local history that
 * can become a draft.
 */
class GenerateViewModel(
    private val repository: ProjectRepository,
    private val history: GenerationHistoryRepository,
    private val importer: MediaImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.entries
                .catch { error ->
                    _uiState.update {
                        it.copy(notice = error.message ?: "Gagal memuat history")
                    }
                }
                .collect { entries -> _uiState.update { it.copy(history = entries) } }
        }
    }

    fun setPrompt(text: String) {
        _uiState.update {
            it.copy(prompt = text.take(GenerationRequest.MAX_PROMPT), notice = null)
        }
    }

    fun setType(type: GenerationType) {
        _uiState.update { it.copy(type = type, showBackendPanel = false, notice = null) }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        _uiState.update { it.copy(aspectRatio = aspectRatio, notice = null) }
    }

    fun setStyle(style: GenerationStyle) {
        _uiState.update { it.copy(style = style, notice = null) }
    }

    /** No network call: reveals the panel that says a backend is required. */
    fun generate() {
        val snapshot = _uiState.value
        if (!snapshot.canGenerate) return
        _uiState.update {
            it.copy(
                showBackendPanel = true,
                notice = "Model generatif butuh backend sendiri. Tidak ada permintaan " +
                    "jaringan yang dikirim dari perangkat ini."
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /** Removes one imported result from History. The media file itself is untouched. */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            try {
                history.delete(id)
                _uiState.update { it.copy(notice = "Hasil dihapus dari History.") }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(notice = "Gagal menghapus hasil: " +
                        (error.message ?: "kesalahan tak dikenal"))
                }
            }
        }
    }

    /**
     * Imports a real result from the gallery, records the brief it belongs to,
     * and shows it in History. Nothing is uploaded.
     */
    fun importResult(uri: Uri) {
        val snapshot = _uiState.value
        if (!snapshot.canImport) return
        _uiState.update { it.copy(importing = true, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = importer.import(uri)) {
                    is MediaImporter.Result.Success -> {
                        val asset = result.asset
                        val wanted = snapshot.type
                        val matches = when (wanted) {
                            GenerationType.IMAGE -> asset.kind == MediaKind.IMAGE
                            GenerationType.VIDEO -> asset.kind == MediaKind.VIDEO
                        }
                        if (!matches) {
                            _uiState.update {
                                it.copy(
                                    importing = false,
                                    notice = "Pilih berkas ${wanted.label.lowercase()} " +
                                        "untuk tipe ini."
                                )
                            }
                            return@launch
                        }
                        val request = snapshot.request
                        history.add(
                            GenerationEntry(
                                id = asset.id,
                                prompt = request.prompt.ifBlank { asset.displayName },
                                type = wanted,
                                style = snapshot.style,
                                aspectRatio = snapshot.aspectRatio,
                                asset = asset,
                                createdAtMs = System.currentTimeMillis(),
                                seed = request.derivedSeed()
                            )
                        )
                        _uiState.update {
                            it.copy(
                                importing = false,
                                notice = "Hasil diimpor ke History. Tidak ada unggahan."
                            )
                        }
                    }

                    is MediaImporter.Result.Failure -> _uiState.update {
                        it.copy(
                            importing = false,
                            notice = "Gagal mengimpor hasil: " +
                                result.reason.name.lowercase() + "."
                        )
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        importing = false,
                        notice = "Gagal mengimpor hasil: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    /**
     * Turns one history result into a real draft and hands its id back. The file
     * is checked first, because a history entry outlives the media it points at.
     */
    fun createDraft(entry: GenerationEntry, onDone: (String?) -> Unit) {
        if (_uiState.value.creatingDraft) return onDone(null)
        _uiState.update { it.copy(creatingDraft = true, notice = null) }
        viewModelScope.launch {
            try {
                val path = entry.asset.sourcePath
                if (path == null || !File(path).isFile) {
                    _uiState.update {
                        it.copy(
                            creatingDraft = false,
                            notice = "Berkas hasil ini sudah tidak ada di perangkat."
                        )
                    }
                    return@launch onDone(null)
                }
                val taken = repository.summaries().map { it.name }.toSet()
                val project = ProjectFactory.fromAssets(draftName(entry, taken), listOf(entry.asset))
                if (project.timeline.tracks.none { it.clips.isNotEmpty() }) {
                    _uiState.update {
                        it.copy(
                            creatingDraft = false,
                            notice = "Hasil ini tidak bisa ditempatkan di timeline."
                        )
                    }
                    return@launch onDone(null)
                }
                repository.upsert(project)
                _uiState.update { it.copy(creatingDraft = false) }
                onDone(project.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        creatingDraft = false,
                        notice = "Gagal membuat draft: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    private fun draftName(entry: GenerationEntry, taken: Set<String>): String {
        val base = entry.prompt.substringBefore('\n').trim().take(28)
        if (base.isEmpty()) return DraftSummaries.nextName(taken)
        val candidate = "Generate · $base"
        return if (candidate in taken) DraftSummaries.nextName(taken) else candidate
    }
}
