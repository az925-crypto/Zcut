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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.EffectsCatalog
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.EffectConfig
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutAmber
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.roundToInt

@Composable
fun EffectsScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "effects:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Effects",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        EffectsBody(
            project = project,
            clip = clip,
            thumbnails = container.mediaThumbnails,
            onEffects = viewModel::setEffects
        )
    }
}

@Composable
private fun EffectsBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onEffects: (List<EffectConfig>) -> Unit
) {
    val active = clip.effects.lastOrNull()
    var category by remember(clip.id) { mutableStateOf(EffectsCatalog.Category.TRENDING) }
    var intensity by remember(clip.id, active?.id, active?.intensity) {
        mutableFloatStateOf(active?.intensity ?: 1f)
    }
    val asset = clip.assetId?.let { id -> project.assets.firstOrNull { it.id == id } }
    val thumbnail by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else thumbnails.load(asset, 240)
    }
    val blurRadius = EffectsCatalog.blurRadius(clip.effects)

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(EffectsCatalog.previewMatrix(clip.effects))
        ),
        imageModifier = if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier,
        label = active?.let { config ->
            val effect = EffectsCatalog.find(config.id)
            "${effect?.label?.uppercase() ?: "EFEK"} · ${(config.intensity * 100).roundToInt()}%"
        } ?: "TANPA EFEK"
    )

    ToolSection(title = "Intensity") {
        if (active == null) {
            ToolHint("Pilih efek di bawah untuk mengaktifkan slider intensity.")
        } else {
            ToolSlider(
                label = "Kekuatan ${EffectsCatalog.find(active.id)?.label ?: ""}".trim(),
                value = intensity,
                valueRange = 0f..1f,
                onValueChange = { intensity = it },
                onValueChangeFinished = {
                    onEffects(
                        clip.effects.map { config ->
                            if (config.id == active.id) {
                                config.copy(intensity = intensity)
                            } else {
                                config
                            }
                        }
                    )
                },
                valueLabel = { "${(it * 100).roundToInt()}%" }
            )
            if (clip.effects.size > 1) {
                ToolValueRow("Efek aktif", clip.effects.size.toString())
                ToolHint("Slider mengubah efek terakhir yang dipilih.")
            }
            ZcutSecondaryButton(
                text = "Hapus semua efek",
                onClick = { onEffects(emptyList()) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    ToolSection(title = "Library") {
        ToolChipRow {
            EffectsCatalog.Category.entries.forEach { candidate ->
                ToolChip(
                    label = candidate.label,
                    selected = candidate == category,
                    onClick = { category = candidate }
                )
            }
        }
        EffectsCatalog.byCategory(category).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                row.forEach { effect ->
                    val selected = clip.effects.any { it.id == effect.id }
                    EffectTile(
                        effect = effect,
                        bitmap = thumbnail,
                        selected = selected,
                        onClick = {
                            val next = if (selected) {
                                clip.effects.filterNot { it.id == effect.id }
                            } else {
                                clip.effects + EffectConfig(effect.id, 1f)
                            }
                            onEffects(next)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        ToolHint(
            "Efek dasar dirender lokal dari shader Zcut. Efek AR/face yang " +
                "butuh model deteksi ditandai BUTUH BACKEND; preview-nya memakai " +
                "penyesuaian warna lokal, tidak pernah dikunci."
        )
    }
}

@Composable
private fun EffectTile(
    effect: EffectsCatalog.Effect,
    bitmap: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
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
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix(EffectsCatalog.previewMatrix(effect, 1f))
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Surface(
                color = if (effect.backendRequired) {
                    ZcutAmber.copy(alpha = 0.85f)
                } else {
                    Color.Black.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = if (effect.backendRequired) "BUTUH BACKEND" else "LOCAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (effect.backendRequired) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                text = effect.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Text(
            text = effect.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ZcutLime else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
