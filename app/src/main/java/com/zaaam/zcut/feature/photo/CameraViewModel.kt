package com.zaaam.zcut.feature.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TrackType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Modes the mockup draws under the viewfinder. */
enum class CameraMode(val label: String) {
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    PORTRAIT("PORTRAIT"),
    SLOW_MO("SLOW-MO")
}

enum class CameraFacing { BACK, FRONT }

enum class FlashSetting { OFF, AUTO, ON }

data class CameraUiState(
    /** The mockup opens on VIDEO. */
    val mode: CameraMode = CameraMode.VIDEO,
    val facing: CameraFacing = CameraFacing.BACK,
    val flash: FlashSetting = FlashSetting.OFF,
    /** 0 = off, otherwise a countdown before capture. */
    val timerMs: Long = 0L,
    val aspect: AspectRatio = AspectRatio.PORTRAIT_9_16,
    /** Carried into the photo editor / draft as the starting filter. */
    val filterId: String? = null,
    val beautyOn: Boolean = false,
    val recording: Boolean = false,
    val busy: Boolean = false,
    val notice: String? = null
) {
    val isVideo: Boolean get() = mode == CameraMode.VIDEO || mode == CameraMode.SLOW_MO
    /** PORTRAIT turns retouch on by default, like the original's portrait mode. */
    val effectiveBeauty: Boolean get() = beautyOn || mode == CameraMode.PORTRAIT
}

/**
 * Camera state (F26). The realtime EffectSDK preview of the original is not
 * available, so beauty/filter here are *carried into* the captured result
 * instead of pretending to be a live GPU preview. Recording itself is CameraX.
 */
class CameraViewModel(
    private val repository: ProjectRepository,
    private val importer: MediaImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setMode(mode: CameraMode) {
        if (_uiState.value.recording) return
        _uiState.update {
            it.copy(mode = mode, timerMs = if (mode == CameraMode.PHOTO) it.timerMs else 0L)
        }
    }

    fun toggleFacing() {
        if (_uiState.value.recording) return
        _uiState.update {
            it.copy(
                facing = if (it.facing == CameraFacing.BACK) {
                    CameraFacing.FRONT
                } else {
                    CameraFacing.BACK
                }
            )
        }
    }

    fun cycleFlash() {
        _uiState.update {
            it.copy(
                flash = when (it.flash) {
                    FlashSetting.OFF -> FlashSetting.AUTO
                    FlashSetting.AUTO -> FlashSetting.ON
                    FlashSetting.ON -> FlashSetting.OFF
                }
            )
        }
    }

    /** Off → 3 s → 10 s → off, matching the mockup's "TIMER · OFF" row. */
    fun cycleTimer() {
        _uiState.update {
            it.copy(
                timerMs = when (it.timerMs) {
                    0L -> 3_000L
                    3_000L -> 10_000L
                    else -> 0L
                }
            )
        }
    }

    fun setAspect(aspect: AspectRatio) {
        _uiState.update { it.copy(aspect = aspect) }
    }

    fun setFilter(filterId: String?) {
        _uiState.update { it.copy(filterId = filterId) }
    }

    fun toggleBeauty() {
        _uiState.update { it.copy(beautyOn = !it.beautyOn) }
    }

    fun setRecording(recording: Boolean) {
        _uiState.update { it.copy(recording = recording, notice = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    /**
     * Imports a finished recording and builds a draft. SLOW-MO records at normal
     * speed (the device cannot capture high frame rates here) and applies a 0.5×
     * speed afterward, which the UI states plainly.
     */
    fun importRecording(uri: Uri, slowMotion: Boolean, onDone: (String?) -> Unit) {
        if (_uiState.value.busy) return onDone(null)
        _uiState.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = importer.import(uri)) {
                    is MediaImporter.Result.Success -> {
                        val asset = result.asset
                        val taken = repository.summaries().map { it.name }.toSet()
                        var project = ProjectFactory.fromAssets(
                            DraftSummaries.nextName(taken),
                            listOf(asset)
                        )
                        if (slowMotion) {
                            project = applySlowMotion(project)
                        }
                        repository.upsert(project)
                        _uiState.update {
                            it.copy(
                                busy = false,
                                notice = if (slowMotion) {
                                    "Rekaman masuk draft dengan speed 0.5×."
                                } else {
                                    "Rekaman masuk draft."
                                }
                            )
                        }
                        onDone(project.id)
                    }

                    is MediaImporter.Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                busy = false,
                                notice = "Gagal mengimpor rekaman: " +
                                    result.reason.name.lowercase() + "."
                            )
                        }
                        onDone(null)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        notice = "Gagal mengimpor rekaman: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
                onDone(null)
            }
        }
    }

    private fun applySlowMotion(project: Project): Project {
        val track = project.timeline.tracks.firstOrNull { it.type == TrackType.VIDEO }
            ?: return project
        val first = track.clips.minByOrNull { it.startMs } ?: return project
        val timeline = TimelineEditor.retimeClip(project.timeline, first.id, SLOW_MOTION_SPEED)
        return project.copy(timeline = timeline)
    }

    private companion object {
        const val SLOW_MOTION_SPEED = 0.5f
    }
}