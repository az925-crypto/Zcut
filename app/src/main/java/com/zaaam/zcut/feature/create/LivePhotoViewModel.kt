package com.zaaam.zcut.feature.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.LivePhotoConfig
import com.zaaam.zcut.core.domain.LivePhotoExport
import com.zaaam.zcut.core.media.CompositeRenderer
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.media.MotionPhoto
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LivePhotoUiState(
    val asset: MediaAsset? = null,
    val still: File? = null,
    val stillWidth: Int = 0,
    val stillHeight: Int = 0,
    val video: MediaAsset? = null,
    val durationMs: Long = LivePhotoConfig.DEFAULT_DURATION_MS,
    val motionStrengthPercent: Int = LivePhotoConfig.DEFAULT_MOTION_PERCENT,
    val export: LivePhotoExport = LivePhotoExport.VIDEO,
    val busy: Boolean = false,
    val notice: String? = null
) {
    val canSplit: Boolean get() = !busy && asset != null
    val canCombine: Boolean get() = !busy && still != null
    val hasMotionVideo: Boolean get() = video != null
}

/**
 * Live photo toolkit (F30). A motion photo is split into its still and its
 * appended video with [MotionPhoto]; combining writes a normal draft (still,
 * then the motion clip). GIF and native motion-photo export are not available
 * and the screen says so instead of faking them.
 */
class LivePhotoViewModel(
    private val repository: ProjectRepository,
    private val importer: MediaImporter,
    private val thumbnails: MediaThumbnails,
    private val compositeDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(LivePhotoUiState())
    val uiState: StateFlow<LivePhotoUiState> = _uiState.asStateFlow()

    fun pick(uri: Uri) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = importer.import(uri)) {
                    is MediaImporter.Result.Success -> {
                        val asset = result.asset
                        if (asset.kind == MediaKind.VIDEO) {
                            _uiState.update {
                                it.copy(
                                    asset = asset,
                                    still = null,
                                    stillWidth = 0,
                                    stillHeight = 0,
                                    video = asset,
                                    busy = false,
                                    notice = "Video dipilih. Tekan Split photo untuk mengambil still."
                                )
                            }
                        } else {
                            val path = asset.localPath
                            if (path == null) {
                                _uiState.update {
                                    it.copy(
                                        busy = false,
                                        notice = "Berkas gambar tidak punya salinan lokal."
                                    )
                                }
                                return@launch
                            }
                            val still = File(path)
                            val extracted = MotionPhoto.extractVideo(
                                source = still,
                                output = File(compositeDir, "live_${asset.id}.mp4")
                            )
                            _uiState.update {
                                it.copy(
                                    asset = asset,
                                    still = still,
                                    stillWidth = asset.width,
                                    stillHeight = asset.height,
                                    video = extracted,
                                    busy = false,
                                    notice = if (extracted != null) {
                                        "Motion photo dipecah: still + video gerak siap digabung."
                                    } else {
                                        "Still siap. Video gerak tidak ditemukan; combine " +
                                            "akan memakai still saja."
                                    }
                                )
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

    fun splitPhoto() {
        val snapshot = _uiState.value
        if (snapshot.busy) return
        val asset = snapshot.asset
        if (asset == null) {
            _uiState.update { it.copy(notice = "Pilih motion photo atau video dulu.") }
            return
        }
        if (asset.kind != MediaKind.VIDEO) {
            _uiState.update {
                it.copy(
                    notice = if (it.still != null) {
                        "Still sudah tersedia dari gambar asli."
                    } else {
                        "Gambar tidak bisa dibaca sebagai still."
                    }
                )
            }
            return
        }

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val frame = thumbnails.load(asset, CompositeRenderer.DEFAULT_LONG_EDGE)
                if (frame == null) {
                    _uiState.update {
                        it.copy(
                            busy = false,
                            notice = "Gagal mengambil frame pertama dari video."
                        )
                    }
                    return@launch
                }
                val file = File(
                    compositeDir,
                    "live_still_${asset.id}_${System.currentTimeMillis()}.jpg"
                )
                if (!CompositeRenderer.saveJpeg(frame, file)) {
                    _uiState.update {
                        it.copy(busy = false, notice = "Gagal menyimpan still ke workspace.")
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        still = file,
                        stillWidth = frame.width,
                        stillHeight = frame.height,
                        video = asset,
                        busy = false,
                        notice = "Still diekstrak dari frame pertama video."
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal mengekstrak still: " +
                            (error.message ?: "kesalahan tak dikenal") + "."
                    )
                }
            }
        }
    }

    fun setDurationMs(value: Float) {
        val clamped = LivePhotoConfig(durationMs = value.toLong()).sanitize().durationMs
        _uiState.update { it.copy(durationMs = clamped, notice = null) }
    }

    /** The play badge cannot play a motion photo in this screen; say so. */
    fun showPreviewNotice() {
        _uiState.update {
            it.copy(
                notice = "Pratinjau gerak belum tersedia di layar ini; still ditampilkan."
            )
        }
    }

    fun setExport(export: LivePhotoExport) {
        if (!export.supported) {
            _uiState.update {
                it.copy(notice = "${export.label} belum didukung lokal; pilih Video.")
            }
            return
        }
        _uiState.update { it.copy(export = export, notice = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun combine(onDone: (String?) -> Unit) {
        val snapshot = _uiState.value
        if (snapshot.busy) return onDone(null)
        val still = snapshot.still
        if (still == null) {
            _uiState.update { it.copy(notice = "Tekan Split photo dulu untuk mengambil still.") }
            return onDone(null)
        }
        val config = LivePhotoConfig(
            durationMs = snapshot.durationMs,
            motionStrengthPercent = snapshot.motionStrengthPercent,
            export = snapshot.export
        ).sanitize()
        if (!config.export.supported) {
            _uiState.update { it.copy(notice = "Ekspor ini belum didukung lokal.") }
            return onDone(null)
        }

        val width = snapshot.stillWidth.takeIf { it > 0 } ?: snapshot.asset?.width ?: 0
        val height = snapshot.stillHeight.takeIf { it > 0 } ?: snapshot.asset?.height ?: 0
        val aspect = closestAspect(width, height)

        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val project = CompositeDrafts.createDraft(
                    repository = repository,
                    still = still,
                    width = width,
                    height = height,
                    aspectRatio = aspect,
                    stillDurationMs = config.durationMs,
                    extraAssets = listOfNotNull(snapshot.video)
                )
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = if (project == null) {
                            "Gagal membuat draft dari still."
                        } else if (snapshot.video != null) {
                            "Draft ${project.name} dibuat: still + video gerak."
                        } else {
                            "Draft ${project.name} dibuat dari still saja."
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
                        notice = "Gagal membuat draft: " +
                            (error.message ?: "kesalahan tak dikenal") + "."
                    )
                }
                onDone(null)
            }
        }
    }

    private fun closestAspect(width: Int, height: Int): AspectRatio {
        if (width <= 0 || height <= 0) return AspectRatio.PORTRAIT_9_16
        val target = width.toFloat() / height.toFloat()
        return AspectRatio.entries.minByOrNull { abs(it.ratio - target) }
            ?: AspectRatio.PORTRAIT_9_16
    }
}
