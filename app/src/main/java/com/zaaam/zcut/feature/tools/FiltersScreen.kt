package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.ColorPipeline
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.FilterConfig
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.roundToInt

@Composable
fun FiltersScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "filters:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Filters",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        FiltersBody(
            project = project,
            clip = clip,
            thumbnails = container.mediaThumbnails,
            onFilter = viewModel::setFilter
        )
    }
}

@Composable
private fun FiltersBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onFilter: (FilterConfig?) -> Unit
) {
    val active = clip.filters.firstOrNull()
    var intensity by remember(clip.id, active?.id, active?.intensity) {
        mutableFloatStateOf(active?.intensity ?: 1f)
    }
    val previewFilter = active?.let { FilterConfig(it.id, intensity) }
    val asset = clip.assetId?.let { id -> project.assets.firstOrNull { it.id == id } }
    val thumbnail by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else thumbnails.load(asset, 240)
    }

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(ColorPipeline.buildMatrix(previewFilter, clip.color))
        ),
        label = "${ColorPipeline.preset(active?.id)?.label?.uppercase() ?: "ORIGINAL"} · " +
            "${(intensity * 100).roundToInt()}%"
    )

    ToolSection(title = "Intensity") {
        if (active == null) {
            ToolHint("Pilih filter di bawah untuk mengaktifkan slider intensity.")
        } else {
            ToolSlider(
                label = "Kekuatan filter",
                value = intensity,
                valueRange = 0f..1f,
                onValueChange = { intensity = it },
                onValueChangeFinished = {
                    onFilter(FilterConfig(active.id, intensity))
                },
                valueLabel = { "${(it * 100).roundToInt()}%" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                ZcutSecondaryButton(
                    text = "Hapus filter",
                    onClick = { onFilter(null) },
                    modifier = Modifier.weight(1f)
                )
                ZcutSecondaryButton(
                    text = "100%",
                    onClick = { onFilter(FilterConfig(active.id, 1f)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    ToolSection(title = "Looks") {
        ColorPipeline.presets.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                row.forEach { preset ->
                    FilterTile(
                        label = preset.label,
                        matrix = preset.matrix,
                        bitmap = thumbnail,
                        selected = preset.id == (active?.id ?: "original"),
                        onClick = {
                            if (preset.id == "original") {
                                onFilter(null)
                            } else {
                                onFilter(FilterConfig(preset.id, intensity))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        ToolHint(
            "Look memakai matriks warna milik Zcut sendiri. Tidak ada LUT " +
                "atau effect package CapCut yang disalin."
        )
    }
}

@Composable
private fun FilterTile(
    label: String,
    matrix: FloatArray,
    bitmap: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) ZcutLime else MaterialTheme.colorScheme.outline,
                    shape = shape
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix(matrix)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ZcutLime else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
