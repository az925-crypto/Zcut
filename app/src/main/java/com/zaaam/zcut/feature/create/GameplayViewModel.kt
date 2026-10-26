package com.zaaam.zcut.feature.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.FacecamPosition
import com.zaaam.zcut.core.domain.FacecamShape
import com.zaaam.zcut.core.domain.GameplayConfig
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

data class GameplayUiState(
    val gameplay: MediaAsset? = null,
    val facecam: MediaAsset? = null,
    val position: FacecamPosition = FacecamPosition.BOTTOM_RIGHT,
    val sizePercent: Int = GameplayConfig.DEFAULT_SIZE_PERCENT,
    val borderPercent: Int = GameplayConfig.DEFAULT_BORDER_PERCENT,
    val shape: FacecamShape = FacecamShape.ROUNDED,
    val busy: Boolean = false,
    val notice: String? = null
) {
    val ready: Boolean get() = !busy && gameplay != null && facecam != null
}

/**
 * Gameplay overlay (F30). Composites a gameplay frame with a facecam bubble into
 * one still and turns that into a draft. Screen recording via MediaProjection
 * and chroma key are not implemented and are labelled as such in the UI.
 */
class GameplayViewModel(
    private val repository: ProjectRepository,
    private val importer: MediaImporter,
    private val thumbnails: MediaThumbnails,
    private val compositeDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameplayUiState())
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    fun setPosition(position: FacecamPosition) {
        _uiState.update { it.copy(position = position, notice = null) }
    }

    fun setSizePercent(value: Int) {
        _uiState.update {
            it.copy(
                sizePercent = value.coerceIn(
                    GameplayConfig.MIN_SIZE_PERCENT,
                    GameplayConfig.MAX_SIZE_PERCENT
                ),
                notice = null
            )
        }
    }

    fun setBorderPercent(value: Int) {
        _uiState.update {
            it.copy(
                borderPercent = value.coerceIn(0, GameplayConfig.MAX_BORDER_PERCENT),
                notice = null
            )
        }
    }

    fun setShape(shape: FacecamShape) {
        _uiState.update { it.copy(shape = shape, notice = null) }
    }

    fun pickGameplay(uri: Uri) = importInto(uri, gameplay = true)

    fun pickFacecam(uri: Uri) = importInto(uri, gameplay = false)

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun importInto(uri: Uri, gameplay: Boolean) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = importer.import(uri)) {
                    is MediaImporter.Result.Success -> {
                        if (result.asset.kind != MediaKind.VIDEO) {
                            _uiState.update {
                                it.copy(
                                    busy = false,
                                    notice = "Gameplay dan facecam harus berupa video."
                                )
                            }
                        } else {
                            _uiState.update {
                                if (gameplay) {
                                    it.copy(gameplay = result.asset, busy = false)
                                } else {
                                    it.copy(facecam = result.asset, busy = false)
                                }
                            }
                        }
                    }

                    is MediaImporter.Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                busy = false,
                                notice = "Gagal mengimpor media: " +
                                    result.reason.name.lowercase() + "."
                            )
                        }
                    }
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

    fun apply(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.busy) return onDone(null)
        val gameplay = snapshot.gameplay
        val facecam = snapshot.facecam
        if (gameplay == null || facecam == null) {
            _uiState.update {
                it.copy(notice = "Pilih video gameplay dan facecam dulu.")
            }
            return onDone(null)
        }

        val config = GameplayConfig(
            position = snapshot.position,
            sizePercent = snapshot.sizePercent,
            borderPercent = snapshot.borderPercent,
            shape = snapshot.shape
        ).sanitize()

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val project = CompositeDrafts.renderDraft(
                    repository = repository,
                    thumbnails = thumbnails,
                    directory = compositeDir,
                    assets = listOf(gameplay, facecam),
                    aspectRatio = AspectRatio.PORTRAIT_9_16,
                    prefix = "gameplay"
                ) { frames, width, height ->
                    CompositeRenderer.renderGameplay(
                        gameplay = frames.getOrNull(0),
                        facecam = frames.getOrNull(1),
                        config = config,
                        width = width,
                        height = height
                    )
                }
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = if (project == null) {
                            "Gagal merender still komposit. Pastikan kedua video bisa dibaca."
                        } else {
                            "Draft ${project.name} dibuat dari still gameplay."
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
