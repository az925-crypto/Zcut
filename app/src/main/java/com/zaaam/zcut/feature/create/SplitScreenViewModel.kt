package com.zaaam.zcut.feature.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.SplitLayout
import com.zaaam.zcut.core.domain.SplitScreenConfig
import com.zaaam.zcut.core.media.CompositeRenderer
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitScreenUiState(
    val layout: SplitLayout = SplitLayout.default,
    val aspectRatio: AspectRatio = AspectRatio.SQUARE_1_1,
    val borderPercent: Int = SplitScreenConfig.DEFAULT_BORDER_PERCENT,
    val cornerPercent: Int = SplitScreenConfig.DEFAULT_CORNER_PERCENT,
    val assets: List<MediaAsset> = emptyList(),
    val busy: Boolean = false,
    val notice: String? = null
) {
    val requiredVideos: Int get() = layout.cells
    val ready: Boolean get() = !busy && assets.size >= requiredVideos
    val visibleAssets: List<MediaAsset> get() = assets.take(requiredVideos)
}

/**
 * Split screen (F30). Picks up to nine videos and renders one high-resolution
 * still grid from their first frames; the resulting draft exports through the
 * normal image pipeline. Simultaneous multi-video playback is not claimed.
 */
class SplitScreenViewModel(
    private val repository: ProjectRepository,
    private val importer: MediaImporter,
    private val thumbnails: MediaThumbnails,
    private val compositeDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitScreenUiState())
    val uiState: StateFlow<SplitScreenUiState> = _uiState.asStateFlow()

    fun setLayout(layout: SplitLayout) {
        _uiState.update { it.copy(layout = layout, notice = null) }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        _uiState.update { it.copy(aspectRatio = aspectRatio, notice = null) }
    }

    fun setBorderPercent(value: Int) {
        _uiState.update {
            it.copy(
                borderPercent = value.coerceIn(0, SplitScreenConfig.MAX_BORDER_PERCENT),
                notice = null
            )
        }
    }

    fun setCornerPercent(value: Int) {
        _uiState.update {
            it.copy(
                cornerPercent = value.coerceIn(0, SplitScreenConfig.MAX_CORNER_PERCENT),
                notice = null
            )
        }
    }

    fun addMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val snapshot = _uiState.value
        if (snapshot.busy) return
        val room = SplitLayout.MAX_CELLS - snapshot.assets.size
        if (room <= 0) {
            _uiState.update { it.copy(notice = "Maksimal 9 video untuk split screen.") }
            return
        }

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val added = mutableListOf<MediaAsset>()
            var failure: String? = null
            try {
                for (uri in uris.take(room)) {
                    when (val result = importer.import(uri)) {
                        is MediaImporter.Result.Success -> {
                            if (result.asset.kind == MediaKind.VIDEO) {
                                added += result.asset
                            } else {
                                failure = "Split screen hanya memakai video."
                            }
                        }

                        is MediaImporter.Result.Failure -> {
                            failure = "Gagal mengimpor media: " +
                                result.reason.name.lowercase() + "."
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        assets = (it.assets + added)
                            .distinctBy { asset -> asset.id }
                            .take(SplitLayout.MAX_CELLS),
                        busy = false,
                        notice = failure ?: if (added.isEmpty()) {
                            "Tidak ada video yang ditambahkan."
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
                        busy = false,
                        notice = "Gagal mengimpor: " +
                            (error.message ?: "kesalahan tak dikenal") + "."
                    )
                }
            }
        }
    }

    fun removeAsset(assetId: String) {
        _uiState.update { state ->
            state.copy(
                assets = state.assets.filterNot { it.id == assetId },
                notice = null
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun apply(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.busy) return onDone(null)
        if (snapshot.assets.size < snapshot.requiredVideos) {
            _uiState.update {
                it.copy(
                    notice = "${snapshot.layout.label} butuh ${snapshot.requiredVideos} video; " +
                        "baru ${snapshot.assets.size} dipilih."
                )
            }
            return onDone(null)
        }

        val config = SplitScreenConfig(
            layout = snapshot.layout,
            aspectRatio = snapshot.aspectRatio,
            borderPercent = snapshot.borderPercent,
            cornerPercent = snapshot.cornerPercent
        ).sanitize()

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val project = CompositeDrafts.renderDraft(
                    repository = repository,
                    thumbnails = thumbnails,
                    directory = compositeDir,
                    assets = snapshot.assets.take(config.layout.cells),
                    aspectRatio = config.aspectRatio,
                    prefix = "split"
                ) { frames, width, height ->
                    CompositeRenderer.renderSplit(frames, config, width, height)
                }
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = if (project == null) {
                            "Gagal merender still komposit. Pastikan semua video bisa dibaca."
                        } else {
                            "Draft ${project.name} dibuat dari still komposit."
                        }
                    )
                }
                onDone(project?.id)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal merender: " +
                            (error.message ?: "kesalahan tak dikenal") + "."
                    )
                }
                onDone(null)
            }
        }
    }
}
