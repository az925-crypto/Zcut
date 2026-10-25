package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.ColorPipeline
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.abs
import kotlin.math.roundToInt

private data class AdjustParam(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val get: (ColorAdjustments) -> Float,
    val set: (ColorAdjustments, Float) -> ColorAdjustments
)

private val AdjustParams = listOf(
    AdjustParam("Brightness", -1f..1f, { it.brightness }, { c, v -> c.copy(brightness = v) }),
    AdjustParam("Contrast", -1f..1f, { it.contrast }, { c, v -> c.copy(contrast = v) }),
    AdjustParam("Saturation", -1f..1f, { it.saturation }, { c, v -> c.copy(saturation = v) }),
    AdjustParam("Temperature", -1f..1f, { it.temperature }, { c, v -> c.copy(temperature = v) }),
    AdjustParam("Tint", -1f..1f, { it.tint }, { c, v -> c.copy(tint = v) }),
    AdjustParam("Highlights", -1f..1f, { it.highlights }, { c, v -> c.copy(highlights = v) }),
    AdjustParam("Shadows", -1f..1f, { it.shadows }, { c, v -> c.copy(shadows = v) }),
    AdjustParam("Vignette", 0f..1f, { it.vignette }, { c, v -> c.copy(vignette = v) })
)

private val BandHues = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)

@Composable
fun AdjustScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "adjust:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Adjust",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        AdjustBody(
            project = project,
            clip = clip,
            thumbnails = container.mediaThumbnails,
            onColor = viewModel::setColor,
            onOpenFilters = onOpenFilters
        )
    }
}

@Composable
private fun AdjustBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onColor: (ColorAdjustments) -> Unit,
    onOpenFilters: () -> Unit
) {
    var draft by remember(clip.id, clip.color) { mutableStateOf(clip.color) }
    var bandHue by remember(clip.id) { mutableFloatStateOf(BandHues.first()) }
    val band = ColorPipeline.bandAt(draft, bandHue)

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(ColorPipeline.buildMatrix(clip.filters.firstOrNull(), draft))
        ),
        label = if (draft.isNeutral) "COLOR · NETRAL" else "COLOR · AKTIF"
    )

    ToolSection(title = "Basic") {
        AdjustParams.forEach { param ->
            AdjustSlider(param, draft, onDraft = { draft = it }, onCommit = { onColor(draft) })
        }
    }

    ToolSection(title = "HSL bands") {
        ToolChipRow {
            BandHues.forEach { hue ->
                ToolChip(
                    label = bandLabel(hue),
                    selected = abs(hue - bandHue) < 0.001f,
                    onClick = { bandHue = hue }
                )
            }
        }
        ToolSlider(
            label = "Saturation band",
            value = band.saturation,
            valueRange = -1f..1f,
            onValueChange = { draft = ColorPipeline.upsertBand(draft, band.copy(saturation = it)) },
            onValueChangeFinished = { onColor(draft) },
            valueLabel = { signedPercent(it) }
        )
        ToolSlider(
            label = "Luminance band",
            value = band.luminance,
            valueRange = -1f..1f,
            onValueChange = { draft = ColorPipeline.upsertBand(draft, band.copy(luminance = it)) },
            onValueChangeFinished = { onColor(draft) },
            valueLabel = { signedPercent(it) }
        )
        ToolValueRow("Band aktif", "${bandLabel(bandHue)} · ${draft.hslBands.size} band")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        ZcutSecondaryButton(
            text = "Reset",
            onClick = {
                draft = ColorAdjustments()
                onColor(draft)
            },
            modifier = Modifier.weight(1f)
        )
        ZcutSecondaryButton(
            text = "Filters",
            onClick = onOpenFilters,
            modifier = Modifier.weight(1f)
        )
    }

    ToolSection(title = "Advanced") {
        ToolValueRow("HSL", "${draft.hslBands.size} dari 8 band")
        ToolValueRow("Curves", "RGB + luma · menyusul")
        ToolValueRow("Color wheels", "lift / gamma / gain · menyusul")
        ToolHint(
            "Preview memakai matriks warna untuk brightness, contrast, saturation, " +
                "temperature, tint, highlights, dan shadows. Vignette dan HSL band " +
                "bersifat per-area/warna sehingga baru diterapkan saat export."
        )
    }
}

@Composable
private fun AdjustSlider(
    param: AdjustParam,
    draft: ColorAdjustments,
    onDraft: (ColorAdjustments) -> Unit,
    onCommit: () -> Unit
) {
    ToolSlider(
        label = param.label,
        value = param.get(draft),
        valueRange = param.range,
        onValueChange = { value -> onDraft(param.set(draft, value)) },
        onValueChangeFinished = onCommit,
        valueLabel = { signedPercent(it) }
    )
}

private fun signedPercent(value: Float): String {
    val rounded = (value * 100).roundToInt()
    return if (rounded > 0) "+$rounded" else "$rounded"
}

private fun bandLabel(hue: Float): String = when (hue.toInt()) {
    0 -> "Merah"
    45 -> "Oranye"
    90 -> "Kuning"
    135 -> "Hijau"
    180 -> "Cyan"
    225 -> "Biru"
    270 -> "Ungu"
    else -> "Magenta"
}
