package com.zaaam.zcut.feature.photo

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.zcut.core.domain.BeautyEditor
import com.zaaam.zcut.core.domain.BrushStroke
import com.zaaam.zcut.core.domain.MattingEditor
import com.zaaam.zcut.core.domain.PhotoAdjustTab
import com.zaaam.zcut.core.domain.PhotoAspect
import com.zaaam.zcut.core.domain.PhotoEditState
import com.zaaam.zcut.core.domain.PhotoExportFormat
import com.zaaam.zcut.core.domain.PhotoPoint
import com.zaaam.zcut.core.domain.withAspect
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.media.PhotoRenderer
import com.zaaam.zcut.core.model.BeautyConfig
import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.render.FaceDetector
import com.zaaam.zcut.core.render.FaceGeometry
import com.zaaam.zcut.core.render.SubjectMask
import com.zaaam.zcut.core.render.SubjectSegmenter
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The mockup's tool row. */
enum class PhotoTool(val label: String) {
    CROP("Crop"),
    ADJUST("Adjust"),
    FILTER("Filter"),
    RETOUCH("Retouch"),
    TEXT("Text"),
    STICKER("Sticker"),
    CUTOUT("Cutout"),
    BRUSH("Brush")
}

data class PhotoEditorUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val error: String? = null,
    /** False until a photo has been decoded; the screen asks for one. */
    val hasPhoto: Boolean = false,
    /** Longest side of the decoded source, for the info row. */
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val tool: PhotoTool = PhotoTool.ADJUST,
    val tab: PhotoAdjustTab = PhotoAdjustTab.LIGHT,
    val edit: PhotoEditState = PhotoEditState(),
    val exportFormat: PhotoExportFormat = PhotoExportFormat.JPG,
    val exporting: Boolean = false,
    val rendering: Boolean = false,
    /** Faces found in the working frame; null = not analysed yet. */
    val faceCount: Int? = null,
    /** Subject confidence 0–100 from the local segmenter; null = not analysed. */
    val subjectPercent: Int? = null,
    val exportPath: String? = null,
    val notice: String? = null
) {
    val canExport: Boolean get() = !loading && error == null && !exporting
    val canUndo: Boolean get() = edit.brush.isNotEmpty()
    val needsPhoto: Boolean get() = !loading && !hasPhoto && error == null
}

/**
 * Photo editor (F26). Non-destructive: the state holds the recipe, the renderer
 * draws it. Retouch reuses the ML Kit face detector + [BeautyProcessor] and
 * cutout reuses the segmentation model + [MattingComposer], both already bundled
 * for the video tools, so the photo path adds no new model.
 */
class PhotoEditorViewModel(
    sourcePath: String?,
    private val importer: MediaImporter,
    private val faceDetector: FaceDetector,
    private val subjectSegmenter: SubjectSegmenter,
    /** Finished photos land here so the share sheet can reach them. */
    private val exportDir: File
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoEditorUiState())
    val uiState: StateFlow<PhotoEditorUiState> = _uiState.asStateFlow()

    /** Full-resolution working copy (bounded by [PhotoRenderer.load]). */
    private var source: Bitmap? = null

    /** Smaller copy used for the live preview, so sliders stay responsive. */
    private var previewSource: Bitmap? = null

    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()

    private var face: FaceGeometry? = null
    private var mask: SubjectMask? = null
    private var renderJob: Job? = null

    init {
        if (sourcePath == null) {
            // Opened from Home with no photo yet: the screen asks for one.
            _uiState.update { it.copy(loading = false) }
        } else {
            load(sourcePath)
        }
    }

    /** Imports a picked photo into the app workspace, then edits that copy. */
    fun pickPhoto(uri: Uri, onDone: (Boolean) -> Unit = {}) {
        if (_uiState.value.importing) return onDone(false)
        _uiState.update { it.copy(importing = true, error = null, notice = null) }
        viewModelScope.launch {
            when (val result = importer.import(uri)) {
                is MediaImporter.Result.Success -> {
                    _uiState.update { it.copy(importing = false) }
                    load(result.asset.localPath ?: return@launch onDone(false))
                    onDone(true)
                }

                is MediaImporter.Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            importing = false,
                            error = "Foto ini tidak bisa dibaca (" +
                                result.reason.name.lowercase() + ")."
                        )
                    }
                    onDone(false)
                }
            }
        }
    }

    private fun load(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val loaded = withContext(Dispatchers.IO) { PhotoRenderer.load(path) }
            if (loaded == null) {
                _uiState.update {
                    it.copy(loading = false, error = "Foto ini tidak bisa dibaca.")
                }
                return@launch
            }
            source = loaded
            previewSource = withContext(Dispatchers.Default) {
                scaleForPreview(loaded)
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    sourceWidth = loaded.width,
                    sourceHeight = loaded.height,
                    hasPhoto = true
                )
            }
            scheduleRender(immediate = true)
        }
    }

    fun setTool(tool: PhotoTool) {
        _uiState.update { it.copy(tool = tool, notice = null) }
        if (tool == PhotoTool.RETOUCH || tool == PhotoTool.CUTOUT) scheduleRender()
    }

    fun setTab(tab: PhotoAdjustTab) {
        _uiState.update { it.copy(tab = tab, notice = null) }
    }

    fun setAspect(aspect: PhotoAspect) {
        val current = source ?: return
        update(current, _uiState.value.edit.withAspect(aspect, aspectRatio(current)))
    }

    fun update(source: Bitmap, edit: PhotoEditState) {
        _uiState.update { it.copy(edit = edit.sanitized(), notice = null) }
        scheduleRender()
    }

    fun rotate() {
        val edit = _uiState.value.edit.copy(quarterTurns = _uiState.value.edit.quarterTurns + 1)
        applyEdit(edit)
    }

    fun flipHorizontal() = applyEdit(_uiState.value.edit.copy(flipHorizontal = !_uiState.value.edit.flipHorizontal))

    fun flipVertical() = applyEdit(_uiState.value.edit.copy(flipVertical = !_uiState.value.edit.flipVertical))

    fun setFilter(filterId: String?) = applyEdit(
        _uiState.value.edit.copy(
            filterId = filterId,
            filterIntensity = if (filterId == null) 1f else _uiState.value.edit.filterIntensity
        )
    )

    fun setFilterIntensity(intensity: Float) = applyEdit(
        _uiState.value.edit.copy(filterIntensity = intensity)
    )

    fun setAdjust(transform: (ColorAdjustments) -> ColorAdjustments) = applyEdit(
        _uiState.value.edit.copy(color = transform(_uiState.value.edit.color))
    )

    fun setBeauty(transform: (BeautyConfig) -> BeautyConfig) =
        applyEdit(_uiState.value.edit.copy(beauty = transform(_uiState.value.edit.beauty)))

    fun setMattingBackground(background: MattingBackground) = applyEdit(
        _uiState.value.edit.copy(matting = _uiState.value.edit.matting.copy(background = background))
    )

    fun setMattingBlur(blur: Float) = applyEdit(
        _uiState.value.edit.copy(matting = _uiState.value.edit.matting.copy(blur = blur))
    )

    fun setText(text: String) {
        val trimmed = text.take(TEXT_LIMIT)
        val next = if (trimmed.isBlank()) {
            _uiState.value.edit.copy(text = null)
        } else {
            _uiState.value.edit.copy(
                text = (_uiState.value.edit.text ?: TextConfig(text = "")).copy(text = trimmed)
            )
        }
        applyEdit(next)
    }

    /**
     * Size/colour of the single text layer. The layer is created on demand (when
     * the text is non-blank) and dropped when the text is emptied.
     */
    fun setTextStyle(transform: (TextConfig) -> TextConfig) {
        val current = _uiState.value.edit.text ?: TextConfig(text = "")
        val next = transform(current)
        applyEdit(_uiState.value.edit.copy(text = next.takeIf { it.text.isNotBlank() }))
    }

    fun setSticker(stickerId: String?) = applyEdit(_uiState.value.edit.copy(stickerId = stickerId))
    fun addStroke(points: List<PhotoPoint>, colorArgb: Long, widthFraction: Float) {
        if (points.size < 2) return
        applyEdit(
            _uiState.value.edit.copy(
                brush = _uiState.value.edit.brush + BrushStroke(
                    points = points,
                    colorArgb = colorArgb,
                    widthFraction = widthFraction
                )
            )
        )
    }

    fun undoStroke() = applyEdit(
        _uiState.value.edit.copy(brush = _uiState.value.edit.brush.dropLast(1))
    )

    fun clearBrush() = applyEdit(_uiState.value.edit.copy(brush = emptyList()))

    fun reset() = applyEdit(PhotoEditState())

    fun setExportFormat(format: PhotoExportFormat) {
        _uiState.update { it.copy(exportFormat = format, exportPath = null, notice = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun dismissExport() {
        _uiState.update { it.copy(exportPath = null) }
    }

    /**
     * Renders the recipe at the working resolution and writes one file. Face and
     * subject analysis run here at the export size when the recipe needs them,
     * so a retouch or cutout is not limited by the preview resolution.
     */
    fun export() {
        val loaded = source ?: return
        if (_uiState.value.exporting) return
        val edit = _uiState.value.edit
        val format = _uiState.value.exportFormat
        _uiState.update { it.copy(exporting = true, notice = null, exportPath = null) }
        viewModelScope.launch {
            try {
                val exported = withContext(Dispatchers.Default) {
                    val exportFace = if (BeautyEditor.isActive(edit.beauty)) {
                        face ?: runCatching { faceDetector.detect(loaded) }.getOrNull()?.firstOrNull()
                    } else {
                        null
                    }
                    val exportMask = if (needsMatting(edit)) {
                        mask ?: runCatching { subjectSegmenter.segment(loaded) }.getOrNull()
                    } else {
                        null
                    }
                    PhotoRenderer.renderForExport(loaded, edit, exportFace, exportMask)
                }
                val file = File(exportDir, exportFileName(format))
                val written = withContext(Dispatchers.IO) {
                    PhotoRenderer.export(exported, format, file)
                }
                if (!written) {
                    _uiState.update {
                        it.copy(
                            exporting = false,
                            notice = "Gagal menulis file ${format.label}."
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        exporting = false,
                        exportPath = file.absolutePath,
                        notice = "Foto diekspor sebagai ${format.label}."
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        exporting = false,
                        notice = "Gagal mengekspor foto: " +
                            (error.message ?: "kesalahan tak dikenal")
                    )
                }
            }
        }
    }

    /** Re-runs the analysis steps for the current recipe, on demand. */
    fun analyse() {
        scheduleRender(immediate = true)
    }

    private fun applyEdit(edit: PhotoEditState) {
        val loaded = source
        if (loaded == null) {
            _uiState.update { it.copy(edit = edit.sanitized(), notice = null) }
            return
        }
        update(loaded, edit)
    }

    private fun scheduleRender(immediate: Boolean = false) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            if (!immediate) delay(PREVIEW_DEBOUNCE_MS)
            val base = previewSource ?: return@launch
            val edit = _uiState.value.edit
            _uiState.update { it.copy(rendering = true) }
            val result = withContext(Dispatchers.Default) {
                val needsFace = BeautyEditor.isActive(edit.beauty)
                val resolvedFace = if (needsFace) {
                    face ?: runCatching { faceDetector.detect(base) }.getOrNull()
                        ?.also { faces ->
                            _uiState.update { state ->
                                state.copy(
                                    faceCount = faces.size,
                                    notice = if (faces.isEmpty()) {
                                        "Tidak ada wajah terdeteksi; slider retouch " +
                                            "tidak punya area untuk bekerja."
                                    } else {
                                        state.notice
                                    }
                                )
                            }
                        }
                        ?.firstOrNull()
                } else {
                    null
                }
                if (needsFace) face = resolvedFace

                val needsMask = needsMatting(edit)
                val resolvedMask = if (needsMask) {
                    mask ?: runCatching { subjectSegmenter.segment(base) }.getOrNull()
                        ?.also { segmented ->
                            _uiState.update { state ->
                                state.copy(
                                    subjectPercent = MattingEditor.subjectConfidencePercent(
                                        segmented.confidence
                                    )
                                )
                            }
                        }
                } else {
                    null
                }
                if (needsMask) mask = resolvedMask

                if (edit.isEdited || needsFace || needsMask) {
                    PhotoRenderer.render(base, edit, resolvedFace, resolvedMask)
                } else {
                    base
                }
            }
            _preview.value = result
            _uiState.update { it.copy(rendering = false) }
        }
    }

    private fun needsMatting(edit: PhotoEditState): Boolean =
        edit.matting.enabled &&
            edit.matting.background != MattingBackground.TRANSPARENT &&
            edit.matting.background != MattingBackground.AI

    private fun exportFileName(format: PhotoExportFormat): String =
        "photo-${System.currentTimeMillis()}.${format.extension}"

    private fun aspectRatio(bitmap: Bitmap): Float =
        if (bitmap.height == 0) 1f else bitmap.width.toFloat() / bitmap.height.toFloat()

    private fun scaleForPreview(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= PREVIEW_MAX_DIMENSION) return bitmap
        val scale = PREVIEW_MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private companion object {
        const val PREVIEW_DEBOUNCE_MS = 220L
        const val PREVIEW_MAX_DIMENSION = 1_080
        const val TEXT_LIMIT = 200
    }
}