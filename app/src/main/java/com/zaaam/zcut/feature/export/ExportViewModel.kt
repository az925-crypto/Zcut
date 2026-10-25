package com.zaaam.zcut.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.ExportPlan
import com.zaaam.zcut.core.media.CoverFrame
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.render.RenderProgress
import com.zaaam.zcut.core.render.RenderRequest
import com.zaaam.zcut.core.render.VideoRenderEngine
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the done screen needs; only the file name crosses the navigation. */
data class ExportResultUi(
    val fileName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val coverFileName: String? = null
)

enum class ExportPhase { IDLE, PREPARING, RENDERING, FINALIZING, DONE, FAILED }

data class ExportUiState(
    val project: Project? = null,
    val notFound: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val smartCover: Boolean = false,
    val phase: ExportPhase = ExportPhase.IDLE,
    val percent: Float = 0f,
    val message: String? = null,
    val result: ExportResultUi? = null
) {
    val isRunning: Boolean
        get() = phase == ExportPhase.PREPARING ||
            phase == ExportPhase.RENDERING ||
            phase == ExportPhase.FINALIZING
}

/**
 * One draft, one local render. The engine is created per screen, like the
 * preview player: two drafts must never share a Transformer.
 */
class ExportViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val renderEngine: VideoRenderEngine,
    private val exportDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var exportJob: Job? = null

    init {
        viewModelScope.launch {
            repository.projects
                .catch { error ->
                    _uiState.update {
                        it.copy(loading = false, error = error.message ?: "Gagal memuat draft")
                    }
                }
                .collect { projects ->
                    val project = projects.firstOrNull { it.id == projectId }
                    _uiState.update {
                        it.copy(project = project, notFound = project == null, loading = false)
                    }
                }
        }
    }

    fun setSmartCover(enabled: Boolean) {
        _uiState.update { it.copy(smartCover = enabled) }
    }

    fun startExport() {
        val snapshot = _uiState.value
        val project = snapshot.project ?: return
        if (snapshot.isRunning) return

        _uiState.update {
            it.copy(phase = ExportPhase.PREPARING, percent = 0f, message = null, result = null)
        }

        exportJob = viewModelScope.launch {
            val stamp = System.currentTimeMillis()
            val fileName = ExportPlan.fileName(project.name, stamp)
            val coverName = if (snapshot.smartCover) {
                ExportPlan.coverFileName(project.name, stamp)
            } else {
                null
            }
            exportDir.mkdirs()
            val request = RenderRequest(
                project = project,
                outputPath = File(exportDir, fileName).absolutePath
            )

            try {
                renderEngine.render(request)
                    .collect { progress ->
                        when (progress) {
                            RenderProgress.Preparing -> update(ExportPhase.PREPARING, 0f)

                            is RenderProgress.Rendering ->
                                update(ExportPhase.RENDERING, progress.percent)

                            RenderProgress.Finalizing -> update(ExportPhase.FINALIZING, 1f)

                            is RenderProgress.Completed -> {
                                val cover = coverName?.takeIf { name ->
                                    writeCover(project, progress.outputPath, name)
                                }
                                _uiState.update {
                                    it.copy(
                                        phase = ExportPhase.DONE,
                                        percent = 1f,
                                        result = ExportResultUi(
                                            fileName = fileName,
                                            sizeBytes = progress.sizeBytes,
                                            durationMs = progress.durationMs,
                                            coverFileName = cover
                                        )
                                    )
                                }
                            }

                            is RenderProgress.Failed -> {
                                _uiState.update {
                                    it.copy(
                                        phase = ExportPhase.FAILED,
                                        percent = 0f,
                                        message = progress.message
                                    )
                                }
                            }
                        }
                    }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        phase = ExportPhase.FAILED,
                        percent = 0f,
                        message = "Render gagal: " + (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _uiState.update { it.copy(phase = ExportPhase.IDLE, percent = 0f) }
        viewModelScope.launch { runCatching { renderEngine.cancel() } }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun update(phase: ExportPhase, percent: Float) {
        _uiState.update { it.copy(phase = phase, percent = percent.coerceIn(0f, 1f)) }
    }

    /**
     * "Smart cover" writes one frame as a JPEG next to the video. A third of the
     * way in is the usual heuristic for a representative frame; the toggle is
     * off by default so nothing is written unless asked for.
     */
    private suspend fun writeCover(project: Project, videoPath: String, name: String): Boolean =
        withContext(Dispatchers.IO) {
            val atMs = project.timeline.durationMs / 3
            CoverFrame.write(videoPath, atMs, File(exportDir, name).absolutePath)
        }
}
