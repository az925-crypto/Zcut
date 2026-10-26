package com.zaaam.zcut.feature.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.BeautySlider
import com.zaaam.zcut.core.domain.ColorPipeline
import com.zaaam.zcut.core.domain.PhotoAdjustTab
import com.zaaam.zcut.core.domain.PhotoAspect
import com.zaaam.zcut.core.domain.PhotoEditState
import com.zaaam.zcut.core.domain.PhotoExportFormat
import com.zaaam.zcut.core.domain.PhotoPoint
import com.zaaam.zcut.core.domain.StickerCatalog
import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.feature.export.ExportDelivery
import com.zaaam.zcut.feature.tools.ColorSwatch
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolSegmented
import com.zaaam.zcut.feature.tools.ToolSlider
import com.zaaam.zcut.feature.tools.ToolValueRow
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.photoEditorViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.io.File
import kotlin.math.roundToInt

/** One adjust slider: a label plus how it reads and writes [ColorAdjustments]. */
private data class PhotoAdjustParam(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val get: (ColorAdjustments) -> Float,
    val set: (ColorAdjustments, Float) -> ColorAdjustments
)

private val LightParams = listOf(
    PhotoAdjustParam("Brightness", -1f..1f, { it.brightness }, { c, v -> c.copy(brightness = v) }),
    PhotoAdjustParam("Contrast", -1f..1f, { it.contrast }, { c, v -> c.copy(contrast = v) }),
    PhotoAdjustParam("Highlights", -1f..1f, { it.highlights }, { c, v -> c.copy(highlights = v) }),
    PhotoAdjustParam("Shadows", -1f..1f, { it.shadows }, { c, v -> c.copy(shadows = v) })
)

private val ColorParams = listOf(
    PhotoAdjustParam("Saturation", -1f..1f, { it.saturation }, { c, v -> c.copy(saturation = v) }),
    PhotoAdjustParam("Temperature", -1f..1f, { it.temperature }, { c, v -> c.copy(temperature = v) }),
    PhotoAdjustParam("Tint", -1f..1f, { it.tint }, { c, v -> c.copy(tint = v) })
)

private val DetailParams = listOf(
    PhotoAdjustParam("Vignette", 0f..1f, { it.vignette }, { c, v -> c.copy(vignette = v) })
)

/** Brush colours; the same list the toolbar shows as swatches. */
private val BrushColors = listOf(0xFFFF3B30, 0xFFFFD60A, 0xFF34C759, 0xFF0A84FF, 0xFFFFFFFF, 0xFF000000)

/**
 * Photo editor (F26). Every edit is a recipe in [PhotoEditState]; pixels are only
 * produced for the preview and the export, so each step stays reversible.
 */
@Composable
fun PhotoEditorScreen(
    photoFile: String?,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current

    // A route argument is a bare file name inside the app's photo folder; reject
    // anything that would escape it.
    val photoDir = container.photoDir
    val sourcePath = remember(photoFile) {
        photoFile?.let { name ->
            runCatching {
                val candidate = File(photoDir, name).canonicalFile
                if (candidate.parentFile == photoDir.canonicalFile) {
                    candidate.absolutePath
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    val viewModel: PhotoEditorViewModel = viewModel(
        key = "photo:${sourcePath.orEmpty()}",
        factory = photoEditorViewModelFactory(container, sourcePath)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.pickPhoto(uri) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Photo",
                subtitle = "Local photo editor",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    TextButton(
                        onClick = viewModel::export,
                        enabled = state.canExport
                    ) {
                        Text("Export")
                    }
                }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)

        when {
            state.loading -> Box(modifier, contentAlignment = Alignment.Center) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            state.needsPhoto -> Box(modifier, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
                ) {
                    EmptyState(
                        title = "Belum ada foto",
                        message = "Pilih foto dari perangkat, atau ambil satu lewat kamera. " +
                            "Semua proses berjalan di perangkat ini.",
                        actionLabel = "Pilih foto",
                        onAction = { picker.launch("image/*") }
                    )
                    ZcutSecondaryButton(
                        text = "Buka kamera",
                        onClick = onOpenCamera,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            }

            state.error != null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Foto tidak bisa dibuka",
                    message = state.error!!,
                    actionLabel = "Pilih foto lain",
                    onAction = { picker.launch("image/*") }
                )
            }

            else -> PhotoEditorBody(
                state = state,
                preview = preview,
                onPickPhoto = { picker.launch("image/*") },
                viewModel = viewModel,
                context = context,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PhotoEditorBody(
    state: PhotoEditorUiState,
    preview: android.graphics.Bitmap?,
    onPickPhoto: () -> Unit,
    viewModel: PhotoEditorViewModel,
    context: android.content.Context,
    modifier: Modifier
) {
    val edit = state.edit
    // The brush settings live here (not inside the preview) so the colour panel
    // below the preview and the gesture on it are always the same two values.
    var brushColor by remember { mutableStateOf(BrushColors.first()) }
    var brushWidth by remember { mutableStateOf(0.02f) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        PhotoStage(
            preview = preview,
            edit = edit,
            brushActive = state.tool == PhotoTool.BRUSH,
            brushColor = brushColor,
            brushWidth = brushWidth,
            onStroke = viewModel::addStroke,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
        )
        Text(
            text = if (state.sourceWidth > 0) {
                "${edit.aspect.label} · ${state.sourceWidth}x${state.sourceHeight}"
            } else {
                edit.aspect.label
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ToolChipRow {
            PhotoTool.entries.forEach { tool ->
                ToolChip(
                    label = tool.label,
                    selected = state.tool == tool,
                    onClick = { viewModel.setTool(tool) }
                )
            }
        }

        when (state.tool) {
            PhotoTool.CROP -> CropPanel(state, viewModel)
            PhotoTool.ADJUST -> AdjustPanel(state, viewModel)
            PhotoTool.FILTER -> FilterPanel(state, viewModel)
            PhotoTool.RETOUCH -> RetouchPanel(state, viewModel)
            PhotoTool.TEXT -> TextPanel(state, viewModel)
            PhotoTool.STICKER -> StickerPanel(state, viewModel)
            PhotoTool.CUTOUT -> CutoutPanel(state, viewModel)
            PhotoTool.BRUSH -> BrushPanel(
                state = state,
                viewModel = viewModel,
                color = brushColor,
                width = brushWidth,
                onColor = { brushColor = it },
                onWidth = { brushWidth = it }
            )
        }

        ExportPanel(
            state = state,
            onFormat = viewModel::setExportFormat,
            onExport = viewModel::export,
            onShare = { path ->
                ExportDelivery.share(context, File(path), state.exportFormat.mimeType)
            },
            onSave = { path -> ExportDelivery.savePhotoToGallery(context, File(path)) },
            onDismiss = viewModel::dismissExport,
            onPickPhoto = onPickPhoto
        )

        state.notice?.let { notice ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        ToolHint(
            "Retouch dan cutout memakai model on-device (ML Kit) yang sama dengan " +
                "tool video. Text, sticker, dan brush digambar di atas foto saat " +
                "pratinjau dan saat export."
        )
    }
}

@Composable
private fun PhotoStage(
    preview: android.graphics.Bitmap?,
    edit: PhotoEditState,
    brushActive: Boolean,
    brushColor: Long,
    brushWidth: Float,
    onStroke: (List<PhotoPoint>, Long, Float) -> Unit,
    modifier: Modifier
) {
    // A gesture lasts one drag; the points never need to trigger recomposition.
    val strokes = remember { mutableListOf<PhotoPoint>() }
    val ratio = edit.aspect.ratio ?: preview?.let {
        if (it.height == 0) 1f else it.width.toFloat() / it.height.toFloat()
    } ?: (3f / 4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null) {
            val image = remember(preview) { preview.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = "Pratinjau foto",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .then(
                        if (brushActive) {
                            Modifier.pointerInput(brushActive, brushColor, brushWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        strokes.clear()
                                        strokes += PhotoPoint(
                                            offset.x / size.width,
                                            offset.y / size.height
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        strokes += PhotoPoint(
                                            change.position.x / size.width,
                                            change.position.y / size.height
                                        )
                                    },
                                    onDragEnd = {
                                        if (strokes.size >= 2) {
                                            onStroke(strokes.toList(), brushColor, brushWidth)
                                        }
                                        strokes.clear()
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Text(
                text = "Menyiapkan pratinjau…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CropPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    ToolSection(title = "Crop") {
        ToolChipRow {
            PhotoAspect.entries.forEach { aspect ->
                ToolChip(
                    label = aspect.label,
                    selected = state.edit.aspect == aspect,
                    onClick = { viewModel.setAspect(aspect) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            ZcutSecondaryButton(
                text = "Putar 90°",
                onClick = viewModel::rotate,
                modifier = Modifier.weight(1f)
            )
            ZcutSecondaryButton(
                text = "Flip H",
                onClick = viewModel::flipHorizontal,
                modifier = Modifier.weight(1f)
            )
            ZcutSecondaryButton(
                text = "Flip V",
                onClick = viewModel::flipVertical,
                modifier = Modifier.weight(1f)
            )
        }
        ToolValueRow("Quarter turns", "${state.edit.quarterTurns % 4} dari 3")
        ToolHint(
            "Preset rasio memusatkan jendela crop. Jendela bebas (geser sudut) dan " +
                "straighten menyusul."
        )
    }
}

@Composable
private fun AdjustPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    val params = when (state.tab) {
        PhotoAdjustTab.LIGHT -> LightParams
        PhotoAdjustTab.COLOR -> ColorParams
        PhotoAdjustTab.DETAIL -> DetailParams
    }
    ToolSection(title = "Adjust") {
        ToolSegmented(
            options = PhotoAdjustTab.entries.map { it.label },
            selectedIndex = PhotoAdjustTab.entries.indexOf(state.tab),
            onSelect = { viewModel.setTab(PhotoAdjustTab.entries[it]) }
        )
        params.forEach { param ->
            ToolSlider(
                label = param.label,
                value = param.get(state.edit.color),
                valueRange = param.range,
                onValueChange = { value -> viewModel.setAdjust { param.set(it, value) } },
                onValueChangeFinished = {},
                valueLabel = { signedPercent(it) }
            )
        }
        ZcutSecondaryButton(
            text = "Reset adjust",
            onClick = { viewModel.setAdjust { ColorAdjustments() } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    ToolSection(title = "Filter") {
        ToolChipRow {
            ColorPipeline.presets.forEach { preset ->
                val id = preset.id.takeIf { it != "original" }
                ToolChip(
                    label = preset.label,
                    selected = state.edit.filterId == id,
                    onClick = { viewModel.setFilter(id) }
                )
            }
        }
        ToolSlider(
            label = "Intensity",
            value = state.edit.filterIntensity,
            valueRange = 0f..1f,
            onValueChange = viewModel::setFilterIntensity,
            onValueChangeFinished = {},
            valueLabel = { "${(it * 100).roundToInt()}%" }
        )
        ToolHint("Matriks filter yang sama dipakai tool video dan tool foto.")
    }
}

@Composable
private fun RetouchPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    ToolSection(title = "Retouch") {
        ToolValueRow(
            "Wajah terdeteksi",
            when (val faces = state.faceCount) {
                null -> "belum dianalisis"
                0 -> "tidak ada"
                else -> "$faces"
            }
        )
        BeautySlider.entries.forEach { slider ->
            val value = slider.uiValue(state.edit.beauty)
            ToolSlider(
                label = slider.label,
                value = value,
                valueRange = slider.relativeMin..slider.relativeMax,
                onValueChange = { next ->
                    viewModel.setBeauty { slider.withValue(it, next) }
                },
                onValueChangeFinished = {},
                valueLabel = { signedPercent(it) }
            )
        }
        ToolHint(
            "Retouch butuh wajah terdeteksi; kalau tidak ada, slider tetap bisa " +
                "digeser tapi tidak punya area untuk bekerja."
        )
    }
}

@Composable
private fun TextPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    val text = state.edit.text
    ToolSection(title = "Text") {
        OutlinedTextField(
            value = text?.text.orEmpty(),
            onValueChange = { value -> viewModel.setText(value.take(200)) },
            label = { Text("Teks di foto") },
            minLines = 1,
            maxLines = 3,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        ToolSlider(
            label = "Size",
            value = text?.fontSizeSp ?: 42f,
            valueRange = 16f..96f,
            onValueChange = { value -> viewModel.setTextStyle { it.copy(fontSizeSp = value) } },
            onValueChangeFinished = {},
            valueLabel = { "${it.roundToInt()} pt" }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextColors.forEach { argb ->
                ColorSwatch(
                    argb = argb,
                    selected = (text?.colorArgb ?: 0xFFFFFFFF) == argb,
                    onClick = { viewModel.setTextStyle { it.copy(colorArgb = argb) } }
                )
            }
        }
        ToolChipRow {
            ToolChip(
                label = "Shadow",
                selected = text?.shadow == true,
                onClick = { viewModel.setTextStyle { it.copy(shadow = !it.shadow) } }
            )
            ToolChip(
                label = "Stroke",
                selected = (text?.strokeWidth ?: 0f) > 0f,
                onClick = {
                    viewModel.setTextStyle {
                        it.copy(strokeWidth = if (it.strokeWidth > 0f) 0f else 4f)
                    }
                }
            )
        }
    }
}

@Composable
private fun StickerPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    var category by remember { mutableStateOf(StickerCatalog.categories.first()) }
    ToolSection(title = "Sticker") {
        ToolChipRow {
            StickerCatalog.categories.forEach { entry ->
                ToolChip(
                    label = entry.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = entry == category,
                    onClick = { category = entry }
                )
            }
        }
        ToolChipRow {
            ToolChip(
                label = "Hapus",
                selected = state.edit.stickerId == null,
                onClick = { viewModel.setSticker(null) }
            )
            StickerCatalog.byCategory(category).forEach { sticker ->
                ToolChip(
                    label = sticker.glyph,
                    selected = state.edit.stickerId == sticker.id,
                    onClick = { viewModel.setSticker(sticker.id) }
                )
            }
        }
        ToolValueRow(
            "Aktif",
            state.edit.stickerId?.let { StickerCatalog.canonical(it).label } ?: "tidak ada"
        )
        ToolHint("Semua sticker adalah glyph buatan Zcut sendiri; tanpa aset pihak lain.")
    }
}

@Composable
private fun CutoutPanel(state: PhotoEditorUiState, viewModel: PhotoEditorViewModel) {
    ToolSection(title = "Cutout") {
        ToolChipRow {
            MattingBackground.entries.forEach { background ->
                ToolChip(
                    label = when (background) {
                        MattingBackground.TRANSPARENT -> "Transparan"
                        MattingBackground.COLOR -> "Warna"
                        MattingBackground.BLUR -> "Blur"
                        MattingBackground.IMAGE -> "Gambar"
                        MattingBackground.AI -> "AI"
                    },
                    selected = state.edit.matting.enabled &&
                        state.edit.matting.background == background,
                    // AI is selectable but renders nothing: it only records the
                    // prompt, and the hint below says the generation needs a
                    // backend. Disabling it would hide the option entirely.
                    onClick = { viewModel.setMattingBackground(background) }
                )
            }
        }
        ToolValueRow(
            "Subjek",
            state.subjectPercent?.let { "$it%" } ?: "belum dianalisis"
        )
        if (state.edit.matting.background == MattingBackground.BLUR) {
            ToolSlider(
                label = "Blur latar",
                value = state.edit.matting.blur,
                valueRange = 0f..1f,
                onValueChange = viewModel::setMattingBlur,
                onValueChangeFinished = {},
                valueLabel = { "${(it * 100).roundToInt()}%" }
            )
        }
        ToolHint(
            "Generate latar AI BUTUH BACKEND; mode AI hanya menyimpan prompt. " +
                "Pemisahan subjek memakai model selfie-segmentation on-device."
        )
    }
}

@Composable
private fun BrushPanel(
    state: PhotoEditorUiState,
    viewModel: PhotoEditorViewModel,
    color: Long,
    width: Float,
    onColor: (Long) -> Unit,
    onWidth: (Float) -> Unit
) {
    ToolSection(title = "Brush") {
        ToolValueRow("Stroke", "${state.edit.brush.size}")
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            BrushColors.forEach { argb ->
                ColorSwatch(
                    argb = argb,
                    selected = color == argb,
                    onClick = { onColor(argb) }
                )
            }
        }
        ToolSlider(
            label = "Tebal",
            value = width,
            valueRange = 0.005f..0.08f,
            onValueChange = onWidth,
            onValueChangeFinished = {},
            valueLabel = { "${(it * 100).roundToInt()}%" }
        )
        ZcutSecondaryButton(
            text = "Undo stroke",
            onClick = viewModel::undoStroke,
            enabled = state.canUndo,
            modifier = Modifier.fillMaxWidth()
        )
        ZcutSecondaryButton(
            text = "Hapus semua stroke",
            onClick = viewModel::clearBrush,
            enabled = state.canUndo,
            modifier = Modifier.fillMaxWidth()
        )
        ToolHint(
            "Geser di atas pratinjau untuk menggambar dengan warna dan tebal di atas."
        )
    }
}

@Composable
private fun ExportPanel(
    state: PhotoEditorUiState,
    onFormat: (PhotoExportFormat) -> Unit,
    onExport: () -> Unit,
    onShare: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit
) {
    ToolSection(title = "Export") {
        ToolChipRow {
            PhotoExportFormat.entries.forEach { format ->
                ToolChip(
                    label = format.label,
                    selected = state.exportFormat == format,
                    onClick = { onFormat(format) }
                )
            }
        }
        ZcutPrimaryButton(
            text = if (state.exporting) "Mengekspor…" else "Export foto",
            onClick = onExport,
            enabled = state.canExport,
            modifier = Modifier.fillMaxWidth()
        )
        ZcutSecondaryButton(
            text = "Pilih foto lain",
            onClick = onPickPhoto,
            modifier = Modifier.fillMaxWidth()
        )
        val path = state.exportPath
        if (path != null) {
            ZcutSecondaryButton(
                text = "Bagikan",
                onClick = { onShare(path) },
                modifier = Modifier.fillMaxWidth()
            )
            ZcutSecondaryButton(
                text = "Simpan ke galeri",
                onClick = { onSave(path) },
                modifier = Modifier.fillMaxWidth()
            )
            ToolValueRow("Berkas", File(path).name)
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    }
}

/** Swatches used by the text tool; matches the caption panel's palette. */
private val TextColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFFFD60A, 0xFF34C759, 0xFF0A84FF, 0xFFC9FF4A
)

private fun signedPercent(value: Float): String {
    val rounded = (value * 100).roundToInt()
    return if (rounded > 0) "+$rounded" else "$rounded"
}