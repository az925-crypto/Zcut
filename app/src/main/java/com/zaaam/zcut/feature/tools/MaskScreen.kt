package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.ChromaConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MaskConfig
import com.zaaam.zcut.core.model.MaskShape
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.min

private val ChromaColors = listOf(
    "Hijau" to 0xFF25D366,
    "Biru" to 0xFF1E90FF,
    "Merah" to 0xFFFF3B30
)

@Composable
fun MaskScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "mask:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Mask & Chroma",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        MaskBody(
            project = project,
            clip = clip,
            thumbnails = container.mediaThumbnails,
            onMask = viewModel::setMask,
            onChroma = viewModel::setChroma
        )
    }
}

@Composable
private fun MaskBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onMask: (MaskConfig?) -> Unit,
    onChroma: (ChromaConfig?) -> Unit
) {
    var draft by remember(clip.id, clip.mask) {
        mutableStateOf(clip.mask ?: MaskConfig(shape = MaskShape.CIRCLE))
    }
    var chromaOn by remember(clip.id, clip.chroma) { mutableStateOf(clip.chroma != null) }
    var chroma by remember(clip.id, clip.chroma) {
        mutableStateOf(clip.chroma ?: ChromaConfig())
    }
    val activeMask = clip.mask

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        label = activeMask?.let { "MASK · ${shapeLabel(it.shape).uppercase()}" } ?: "TANPA MASK"
    ) {
        if (activeMask != null && activeMask.shape != MaskShape.NONE) {
            MaskOverlay(config = activeMask)
        }
    }

    ToolSection(title = "Bentuk") {
        ToolChipRow {
            MaskShape.entries.forEach { shape ->
                ToolChip(
                    label = shapeLabel(shape),
                    selected = shape == (activeMask?.shape ?: MaskShape.NONE),
                    onClick = {
                        if (shape == MaskShape.NONE) {
                            onMask(null)
                        } else {
                            onMask(draft.copy(shape = shape))
                        }
                    }
                )
            }
        }
        if (activeMask == null) {
            ToolHint("Pilih bentuk mask untuk menampilkan kontrol feather dan posisi.")
        } else {
            ToolSlider(
                label = "Feather",
                value = activeMask.feather,
                valueRange = 0f..0.5f,
                onValueChange = { draft = draft.copy(feather = it) },
                onValueChangeFinished = { onMask(draft) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolSlider(
                label = "Position X",
                value = activeMask.centerX,
                valueRange = 0f..1f,
                onValueChange = { draft = draft.copy(centerX = it) },
                onValueChangeFinished = { onMask(draft) },
                valueLabel = { "${((it - 0.5f) * 100).toInt()}" }
            )
            ToolSlider(
                label = "Position Y",
                value = activeMask.centerY,
                valueRange = 0f..1f,
                onValueChange = { draft = draft.copy(centerY = it) },
                onValueChangeFinished = { onMask(draft) },
                valueLabel = { "${((it - 0.5f) * 100).toInt()}" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = activeMask.invert,
                    onCheckedChange = { onMask(activeMask.copy(invert = it)) }
                )
            }
        }
    }

    ToolSection(title = "Chroma key") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktifkan chroma key",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = chromaOn,
                onCheckedChange = { enabled ->
                    chromaOn = enabled
                    onChroma(if (enabled) chroma else null)
                }
            )
        }
        if (!chromaOn) {
            ToolHint("Chroma key memisahkan warna latar (green screen) dari subjek.")
        } else {
            ToolChipRow {
                ChromaColors.forEach { (label, argb) ->
                    ToolChip(
                        label = label,
                        selected = chroma.keyColorArgb == argb,
                        onClick = {
                            chroma = chroma.copy(keyColorArgb = argb)
                            onChroma(chroma)
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(chroma.keyColorArgb))
                )
                Text(
                    text = "Key color #" +
                        chroma.keyColorArgb.toString(16).uppercase().takeLast(6),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ToolSlider(
                label = "Strength",
                value = chroma.similarity,
                valueRange = 0f..1f,
                onValueChange = { chroma = chroma.copy(similarity = it) },
                onValueChangeFinished = { onChroma(chroma) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolSlider(
                label = "Smoothness",
                value = chroma.smoothness,
                valueRange = 0f..1f,
                onValueChange = { chroma = chroma.copy(smoothness = it) },
                onValueChangeFinished = { onChroma(chroma) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolSlider(
                label = "Spill suppression",
                value = chroma.spillSuppression,
                valueRange = 0f..1f,
                onValueChange = { chroma = chroma.copy(spillSuppression = it) },
                onValueChangeFinished = { onChroma(chroma) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolHint(
                "Chroma key diproses lokal dengan shader Zcut dan diterapkan " +
                    "penuh saat export; preview menampilkan area mask saja."
            )
        }
    }
}

/**
 * Dims everything outside the mask and outlines the shape, mirroring the
 * mockup's dashed selection over a scrim.
 */
@Composable
private fun MaskOverlay(config: MaskConfig) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        val center = Offset(size.width * config.centerX, size.height * config.centerY)
        val halfWidth = size.width * 0.5f * config.width.coerceIn(0.05f, 1f)
        val halfHeight = size.height * 0.5f * config.height.coerceIn(0.05f, 1f)
        val scrim = Color.Black.copy(alpha = 0.4f)

        when (config.shape) {
            MaskShape.NONE -> Unit
            MaskShape.LINEAR -> {
                val y = center.y
                val region = if (config.invert) {
                    Rect(0f, 0f, size.width, y)
                } else {
                    Rect(0f, y, size.width, size.height)
                }
                drawRect(color = scrim, topLeft = region.topLeft, size = region.size)
                drawDashedLine(Offset(0f, y), Offset(size.width, y))
            }
            MaskShape.MIRROR -> {
                val x = center.x
                val region = if (config.invert) {
                    Rect(0f, 0f, x, size.height)
                } else {
                    Rect(x, 0f, size.width, size.height)
                }
                drawRect(color = scrim, topLeft = region.topLeft, size = region.size)
                drawDashedLine(Offset(x, 0f), Offset(x, size.height))
            }
            else -> {
                val shape = shapePath(
                    shape = config.shape,
                    center = center,
                    halfWidth = halfWidth,
                    halfHeight = halfHeight
                ) ?: return@Canvas
                val scrimPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    if (!config.invert) {
                        addRect(Rect(Offset.Zero, Size(size.width, size.height)))
                    }
                    addPath(shape)
                }
                drawPath(path = scrimPath, color = scrim)
                drawPath(
                    path = shape,
                    color = ZcutLime.copy(alpha = 0.9f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(12f, 9f),
                            0f
                        )
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawDashedLine(start: Offset, end: Offset) {
    drawLine(
        color = ZcutLime.copy(alpha = 0.9f),
        start = start,
        end = end,
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
    )
}

private fun shapePath(
    shape: MaskShape,
    center: Offset,
    halfWidth: Float,
    halfHeight: Float
): Path? {
    val bounds = Rect(
        left = center.x - halfWidth,
        top = center.y - halfHeight,
        right = center.x + halfWidth,
        bottom = center.y + halfHeight
    )
    return when (shape) {
        MaskShape.RECTANGLE -> Path().apply { addRect(bounds) }
        MaskShape.CIRCLE -> Path().apply { addOval(bounds) }
        MaskShape.HEART -> Path().apply {
            moveTo(center.x, bounds.bottom)
            cubicTo(
                bounds.left - halfWidth * 0.4f,
                center.y - halfHeight * 0.3f,
                center.x - halfWidth * 0.6f,
                bounds.top - halfHeight * 0.6f,
                center.x,
                center.y - halfHeight * 0.25f
            )
            cubicTo(
                center.x + halfWidth * 0.6f,
                bounds.top - halfHeight * 0.6f,
                bounds.right + halfWidth * 0.4f,
                center.y - halfHeight * 0.3f,
                center.x,
                bounds.bottom
            )
            close()
        }
        MaskShape.STAR -> starPath(center, min(halfWidth, halfHeight))
        MaskShape.NONE, MaskShape.LINEAR, MaskShape.MIRROR -> null
    }
}

private fun starPath(center: Offset, radius: Float): Path {
    val path = Path()
    val inner = radius * 0.45f
    for (point in 0 until 10) {
        val r = if (point % 2 == 0) radius else inner
        val angle = (-90.0 + point * 36.0) * Math.PI / 180.0
        val x = center.x + (r * kotlin.math.cos(angle)).toFloat()
        val y = center.y + (r * kotlin.math.sin(angle)).toFloat()
        if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun shapeLabel(shape: MaskShape): String = when (shape) {
    MaskShape.NONE -> "Off"
    MaskShape.LINEAR -> "Linear"
    MaskShape.MIRROR -> "Mirror"
    MaskShape.CIRCLE -> "Bulat"
    MaskShape.RECTANGLE -> "Kotak"
    MaskShape.HEART -> "Hati"
    MaskShape.STAR -> "Bintang"
}
