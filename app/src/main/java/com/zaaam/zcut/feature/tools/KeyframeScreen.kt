package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.KeyframeEditor
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Easing
import com.zaaam.zcut.core.model.Keyframe
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutRed
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.roundToLong

@Composable
fun KeyframeScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "keyframe:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Keyframes",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        KeyframeBody(
            project = project,
            clip = clip,
            thumbnails = container.mediaThumbnails,
            onAdd = viewModel::addKeyframe,
            onRemove = viewModel::removeKeyframe,
            onClear = viewModel::clearKeyframes,
            onPropertyValue = viewModel::setPropertyValue,
            onEasing = viewModel::setKeyframeEasing
        )
    }
}

@Composable
private fun KeyframeBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onAdd: (AnimatableProperty, Long) -> Unit,
    onRemove: (AnimatableProperty, Long) -> Unit,
    onClear: (AnimatableProperty) -> Unit,
    onPropertyValue: (AnimatableProperty, Long, Float) -> Unit,
    onEasing: (AnimatableProperty, Long, Easing) -> Unit
) {
    var property by remember(clip.id) { mutableStateOf(AnimatableProperty.POSITION_X) }
    // Keyed on the duration so a trim/retime elsewhere can never leave the
    // playhead slider pointing past the clip's end.
    var timeMs by remember(clip.id, clip.durationMs) { mutableLongStateOf(0L) }
    val frames = clip.keyframes
        .filter { it.property == property }
        .sortedBy { it.timeMs }
    val activeFrame = KeyframeEditor.keyframeAt(clip, property, timeMs)
    val sampled = KeyframeEditor.sample(clip, property, timeMs)
    var draftValue by remember(
        clip.id,
        property,
        timeMs,
        activeFrame?.timeMs,
        activeFrame?.value,
        frames.size
    ) { mutableFloatStateOf(sampled) }

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        label = "${KeyframeEditor.labelOf(property)} · " +
            KeyframeEditor.formatValue(property, sampled)
    ) {
        val scale = KeyframeEditor.sample(clip, AnimatableProperty.SCALE, timeMs)
            .coerceIn(0.1f, 4f)
        val positionX = KeyframeEditor.sample(clip, AnimatableProperty.POSITION_X, timeMs)
        val positionY = KeyframeEditor.sample(clip, AnimatableProperty.POSITION_Y, timeMs)
        val rotation = KeyframeEditor.sample(clip, AnimatableProperty.ROTATION, timeMs)
        val opacity = KeyframeEditor.sample(clip, AnimatableProperty.OPACITY, timeMs)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.58f)
                .graphicsLayer {
                    translationX = positionX / 100f * size.width
                    translationY = positionY / 100f * size.height
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    alpha = opacity.coerceIn(0f, 1f)
                }
                .border(
                    width = 1.5.dp,
                    color = ZcutLime,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }

    ToolSection(title = "Playhead") {
        Slider(
            value = timeMs.toFloat(),
            onValueChange = { timeMs = it.roundToLong() },
            valueRange = 0f..clip.durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TimeFormat.clockMillis(timeMs),
                style = MaterialTheme.typography.labelMedium,
                color = ZcutLime
            )
            Text(
                text = TimeFormat.clockMillis(clip.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        KeyframeTrack(
            durationMs = clip.durationMs,
            frames = frames,
            timeMs = timeMs,
            onSeek = { timeMs = it }
        )
    }

    ToolSection(title = "Properties") {
        AnimatableProperty.entries.forEach { candidate ->
            PropertyRow(
                property = candidate,
                value = KeyframeEditor.sample(clip, candidate, timeMs),
                count = clip.keyframes.count { it.property == candidate },
                selected = candidate == property,
                onClick = { property = candidate }
            )
        }
        ToolSlider(
            label = KeyframeEditor.labelOf(property),
            value = draftValue,
            valueRange = KeyframeEditor.rangeOf(property),
            onValueChange = { draftValue = it },
            onValueChangeFinished = {
                onPropertyValue(property, timeMs, draftValue)
            },
            valueLabel = { KeyframeEditor.formatValue(property, it) }
        )
        ToolHint(
            if (frames.isEmpty()) {
                "Geser nilai untuk mengubah properti statis. Tambahkan keyframe " +
                    "pertama untuk mulai menganimasikan."
            } else {
                "Properti ini sudah dianimasikan: setiap perubahan nilai ikut " +
                    "menulis keyframe di posisi playhead."
            }
        )
    }

    ToolSection(title = "Keyframe") {
        ToolValueRow("Waktu", TimeFormat.clockMillis(timeMs))
        ToolValueRow(
            "Status",
            activeFrame?.let { "Keyframe aktif · ${easingLabel(it.easing)}" }
                ?: "Belum ada keyframe di playhead"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            if (activeFrame == null) {
                ZcutSecondaryButton(
                    text = "Tambah keyframe",
                    onClick = { onAdd(property, timeMs) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                ZcutSecondaryButton(
                    text = "Hapus keyframe",
                    onClick = { onRemove(property, activeFrame.timeMs) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (frames.isNotEmpty()) {
                ZcutSecondaryButton(
                    text = "Hapus semua",
                    onClick = { onClear(property) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (activeFrame != null) {
            ToolChipRow {
                Easing.entries.forEach { easing ->
                    ToolChip(
                        label = easingLabel(easing),
                        selected = activeFrame.easing == easing,
                        onClick = { onEasing(property, activeFrame.timeMs, easing) }
                    )
                }
            }
        }
    }

    ToolSection(title = "Curve editor") {
        EasingCurvePreview(easing = activeFrame?.easing ?: Easing.LINEAR)
        ToolHint(
            "Kurva menggambarkan easing dari keyframe aktif menuju keyframe " +
                "berikutnya. Tanpa keyframe, nilainya tetap."
        )
    }
}

@Composable
private fun PropertyRow(
    property: AnimatableProperty,
    value: Float,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = KeyframeEditor.labelOf(property),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) ZcutLime else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = buildString {
                append(KeyframeEditor.formatValue(property, value))
                if (count > 0) append(" · $count◆")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Diamond track. A tap seeks the playhead; the diamonds are the keyframes of the
 * selected property, matching the reference's per-property track.
 */
@Composable
private fun KeyframeTrack(
    durationMs: Long,
    frames: List<Keyframe>,
    timeMs: Long,
    onSeek: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val fraction = (offset.x / width).coerceIn(0f, 1f)
                    onSeek((fraction * safeDuration).roundToLong())
                }
            }
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2.dp.toPx()
        )
        frames.forEach { frame ->
            val x = frame.timeMs.toFloat() / safeDuration * size.width
            val active = kotlin.math.abs(frame.timeMs - timeMs) <=
                KeyframeEditor.MATCH_TOLERANCE_MS
            drawDiamond(
                center = Offset(x, centerY),
                radius = if (active) 7.dp.toPx() else 5.dp.toPx(),
                fill = if (active) ZcutLime else Color(0xFF8B90A0)
            )
        }
        val playheadX = timeMs.toFloat() / safeDuration * size.width
        drawLine(
            color = ZcutLime.copy(alpha = 0.8f),
            start = Offset(playheadX, 4.dp.toPx()),
            end = Offset(playheadX, size.height - 4.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawDiamond(center: Offset, radius: Float, fill: Color) {
    // Four edges drawn twice (thick translucent under, thin solid over) instead
    // of Path objects, so a playhead drag with many keyframes allocates nothing.
    val top = Offset(center.x, center.y - radius)
    val right = Offset(center.x + radius, center.y)
    val bottom = Offset(center.x, center.y + radius)
    val left = Offset(center.x - radius, center.y)
    val fillAlpha = fill.copy(alpha = 0.35f)
    drawLine(fillAlpha, top, right, 4.dp.toPx())
    drawLine(fillAlpha, right, bottom, 4.dp.toPx())
    drawLine(fillAlpha, bottom, left, 4.dp.toPx())
    drawLine(fillAlpha, left, top, 4.dp.toPx())
    drawLine(fill, top, right, 2.dp.toPx())
    drawLine(fill, right, bottom, 2.dp.toPx())
    drawLine(fill, bottom, left, 2.dp.toPx())
    drawLine(fill, left, top, 2.dp.toPx())
}

@Composable
private fun EasingCurvePreview(easing: Easing) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        for (line in 1..3) {
            val y = size.height * line / 4f
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        val path = Path()
        val samples = 64
        for (step in 0..samples) {
            val progress = step.toFloat() / samples
            val value = KeyframeEditor.easingTransform(easing, progress)
            val x = progress * size.width
            val y = size.height * (1f - value)
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = ZcutLime,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = ZcutLime,
            radius = 3.dp.toPx(),
            center = Offset(size.width, 0f)
        )
        drawCircle(
            color = ZcutRed,
            radius = 3.dp.toPx(),
            center = Offset(0f, size.height)
        )
    }
}

private fun easingLabel(easing: Easing): String = when (easing) {
    Easing.LINEAR -> "Linear"
    Easing.EASE_IN -> "Ease in"
    Easing.EASE_OUT -> "Ease out"
    Easing.EASE_IN_OUT -> "Ease in-out"
    Easing.BEZIER -> "Bezier"
}
