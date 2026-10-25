package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.SpeedCurve
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.ClipType
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.SpeedPoint
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val SpeedPresets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 4f)

@Composable
fun SpeedScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "speed:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val thumbnails = container.mediaThumbnails

    ClipToolScreen(
        title = "Speed",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        SpeedBody(
            project = project,
            clip = clip,
            thumbnails = thumbnails,
            onSpeed = viewModel::setSpeed,
            onCurve = viewModel::setCurve,
            onPreservePitch = viewModel::setPreservePitch,
            onReversed = viewModel::setReversed
        )
    }
}

@Composable
private fun SpeedBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onSpeed: (Float) -> Unit,
    onCurve: (List<SpeedPoint>, Float) -> Unit,
    onPreservePitch: (Boolean) -> Unit,
    onReversed: (Boolean) -> Unit
) {
    val isImage = clip.type == ClipType.IMAGE
    val curveActive = !isImage && clip.speed.curve.size >= 2
    val fallbackSpeed = clip.speed.safeSpeed
    val effectiveSpeed = if (curveActive) {
        SpeedCurve.speedAt(clip.speed.curve, 0.5f, fallbackSpeed)
    } else {
        fallbackSpeed
    }
    var smoothness by remember(clip.id) { mutableFloatStateOf(0.5f) }

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        label = "PRATINJAU · ${formatSpeed(effectiveSpeed)}"
    )

    ToolSection(title = "Kecepatan") {
        ToolChipRow {
            SpeedPresets.forEach { preset ->
                ToolChip(
                    label = formatSpeed(preset),
                    selected = !curveActive && abs(fallbackSpeed - preset) < 0.01f,
                    onClick = { onSpeed(preset) }
                )
            }
            ToolChip(
                label = "Reverse",
                selected = clip.isReversed,
                onClick = { onReversed(!clip.isReversed) }
            )
        }
        ToolValueRow("Durasi klip", TimeFormat.clock(clip.durationMs))
        ToolValueRow("Kecepatan", formatSpeed(effectiveSpeed))
    }

    if (isImage) {
        ToolHint(
            "Gambar tidak punya frame untuk curve. Speed hanya mengubah durasi " +
                "klip; preview tetap memakai satu frame diam."
        )
    } else {
        ToolSection(title = "Speed curve") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (curveActive) "Kurva aktif" else "Kurva nonaktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CurveBadge(active = curveActive)
            }
            if (curveActive) {
                SpeedCurveEditor(
                    curve = clip.speed.curve,
                    fallbackSpeed = fallbackSpeed,
                    onCommit = { points ->
                        onCurve(points, SpeedCurve.speedAt(points, 0.5f, fallbackSpeed))
                    }
                )
                ToolSlider(
                    label = "Smoothness",
                    value = smoothness,
                    valueRange = 0f..1f,
                    onValueChange = { smoothness = it },
                    onValueChangeFinished = {
                        val smoothed = SpeedCurve.smoothed(clip.speed.curve, smoothness)
                        onCurve(
                            smoothed,
                            SpeedCurve.speedAt(smoothed, 0.5f, fallbackSpeed)
                        )
                    },
                    valueLabel = { "${(it * 100).roundToInt()}%" }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    ZcutSecondaryButton(
                        text = "Reset ke 1×",
                        onClick = { onSpeed(1f) },
                        modifier = Modifier.weight(1f)
                    )
                    ZcutSecondaryButton(
                        text = "Kurva datar",
                        onClick = {
                            onCurve(defaultCurve(fallbackSpeed), fallbackSpeed)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ToolHint(
                    "Titik kurva mengubah kecepatan sepanjang klip: naikkan titik " +
                        "untuk slow motion, turunkan untuk speed-up."
                )
                ZcutSecondaryButton(
                    text = "Aktifkan curve",
                    onClick = { onCurve(defaultCurve(fallbackSpeed), fallbackSpeed) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    ToolSection(title = "Audio & reverse") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pertahankan pitch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = clip.speed.preservePitch,
                onCheckedChange = onPreservePitch
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reverse",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = clip.isReversed,
                onCheckedChange = onReversed
            )
        }
        ToolHint(
            "Preview selalu memakai retiming yang mempertahankan pitch. " +
                "Setelan pitch dan reverse disimpan untuk export; preview " +
                "reverse belum tersedia."
        )
    }
}

@Composable
private fun CurveBadge(active: Boolean) {
    Surface(
        color = if (active) {
            ZcutLime.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (active) "CUSTOM" else "OFF",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) ZcutLime else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * Four-handle curve editor. The x axis is the clip's source range, the y axis a
 * logarithmic 0.1×–8× speed scale so slow motion gets as much room as speed-up.
 * Points are committed on drag end, matching the tool-wide rule that a gesture
 * writes once.
 */
@Composable
private fun SpeedCurveEditor(
    curve: List<SpeedPoint>,
    fallbackSpeed: Float,
    onCommit: (List<SpeedPoint>) -> Unit
) {
    val stored = remember(curve, fallbackSpeed) {
        if (curve.size >= 2) SpeedCurve.normalized(curve, fallbackSpeed)
        else defaultCurve(fallbackSpeed)
    }
    var points by remember(stored) { mutableStateOf(stored) }
    var draggingIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingIndex = nearestPoint(
                            points = points,
                            position = offset,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            thresholdPx = 32.dp.toPx()
                        )
                    },
                    onDragEnd = {
                        if (draggingIndex >= 0) onCommit(points)
                        draggingIndex = -1
                    },
                    onDragCancel = {
                        draggingIndex = -1
                        points = stored
                    }
                ) { change, _ ->
                    val index = draggingIndex
                    if (index < 0 || index > points.lastIndex) return@detectDragGestures
                    change.consume()
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val height = size.height.toFloat().coerceAtLeast(1f)
                    val isEdge = index == 0 || index == points.lastIndex
                    val minX = if (index == 0) 0f else points[index - 1].atFraction + MIN_POINT_GAP
                    val maxX = if (isEdge) 1f else points[index + 1].atFraction - MIN_POINT_GAP
                    val x = if (isEdge) {
                        points[index].atFraction
                    } else {
                        (change.position.x / width).coerceIn(minX, maxX.coerceAtLeast(minX))
                    }
                    val speed = speedForY(change.position.y, height)
                    points = points.toMutableList().also { list ->
                        list[index] = SpeedPoint(x, speed)
                    }
                }
            }
    ) {
        val grid = Color.White.copy(alpha = 0.08f)
        for (line in 1..3) {
            val y = size.height * line / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        for (line in 1..3) {
            val x = size.width * line / 4f
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }

        val path = Path()
        val samples = 72
        for (step in 0..samples) {
            val fraction = step.toFloat() / samples
            val x = fraction * size.width
            val y = yForSpeed(
                SpeedCurve.speedAtNormalized(points, fraction, fallbackSpeed),
                size.height
            )
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = ZcutLime,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            val center = Offset(
                x = point.atFraction * size.width,
                y = yForSpeed(point.speed, size.height)
            )
            val radius = if (index == draggingIndex) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(Color(0xFF08090D), radius = radius + 2.dp.toPx(), center = center)
            drawCircle(ZcutLime, radius = radius, center = center)
        }
    }
}

private fun nearestPoint(
    points: List<SpeedPoint>,
    position: Offset,
    width: Float,
    height: Float,
    thresholdPx: Float
): Int {
    var bestIndex = -1
    var bestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val dx = point.atFraction * width - position.x
        val dy = yForSpeed(point.speed, height) - position.y
        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return if (bestDistance <= thresholdPx * thresholdPx) bestIndex else -1
}

private fun defaultCurve(fallbackSpeed: Float): List<SpeedPoint> {
    val speed = SpeedCurve.clampSpeed(fallbackSpeed)
    return listOf(
        SpeedPoint(0f, speed),
        SpeedPoint(1f / 3f, speed),
        SpeedPoint(2f / 3f, speed),
        SpeedPoint(1f, speed)
    )
}

private fun yForSpeed(speed: Float, height: Float): Float {
    val low = ln(MIN_CURVE_SPEED)
    val high = ln(MAX_CURVE_SPEED)
    val value = ln(speed.coerceIn(MIN_CURVE_SPEED, MAX_CURVE_SPEED))
    return height * (1f - (value - low) / (high - low))
}

private fun speedForY(y: Float, height: Float): Float {
    val low = ln(MIN_CURVE_SPEED)
    val high = ln(MAX_CURVE_SPEED)
    val fraction = 1f - (y / height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    return exp(low + fraction * (high - low)).coerceIn(MIN_CURVE_SPEED, MAX_CURVE_SPEED)
}

private fun formatSpeed(value: Float): String = when {
    abs(value - value.roundToInt()) < 0.001f -> "${value.roundToInt()}×"
    abs(value * 10f - (value * 10f).roundToInt()) < 0.001f ->
        String.format(Locale.US, "%.1f×", value)
    else -> String.format(Locale.US, "%.2f×", value)
}

private const val MIN_CURVE_SPEED = 0.1f
private const val MAX_CURVE_SPEED = 8f
private const val MIN_POINT_GAP = 0.03f
