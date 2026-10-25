package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.SpeedCurve
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutSpacing

@Composable
fun TrimScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "trim:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val thumbnails = container.mediaThumbnails

    ClipToolScreen(
        title = "Trim",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        TrimBody(
            project = project,
            clip = clip,
            thumbnails = thumbnails,
            onTrim = viewModel::trim,
            onSplit = viewModel::splitAt,
            onDuplicate = viewModel::duplicate,
            onDelete = { ripple ->
                viewModel.delete(ripple) { onBack() }
            }
        )
    }
}

@Composable
private fun TrimBody(
    project: Project,
    clip: Clip,
    thumbnails: MediaThumbnails,
    onTrim: (Long, Long) -> Unit,
    onSplit: (Long) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: (Boolean) -> Unit
) {
    var rangeStart by remember(clip.id, clip.startMs, clip.durationMs) { mutableStateOf(0f) }
    var rangeEnd by remember(clip.id, clip.startMs, clip.durationMs) {
        mutableStateOf(clip.durationMs.toFloat().coerceAtLeast(1f))
    }
    var playheadLocal by remember(clip.id, clip.durationMs) {
        mutableStateOf(clip.durationMs / 2f)
    }
    var ripple by remember { mutableStateOf(false) }
    val duration = clip.durationMs.coerceAtLeast(1L)

    ToolPreviewCard(
        project = project,
        clip = clip,
        thumbnails = thumbnails,
        label = "SUMBER · ${TimeFormat.clock(clip.sourceStartMs + clip.sourceDurationMs)}"
    )

    ToolSection(title = "Rentang klip") {
        RangeSlider(
            value = rangeStart..rangeEnd,
            onValueChange = { range ->
                rangeStart = range.start
                rangeEnd = range.endInclusive
            },
            onValueChangeFinished = {
                onTrim(
                    clip.startMs + rangeStart.toLong(),
                    clip.startMs + rangeEnd.toLong()
                )
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrimValue("MULAI", sourceTime(clip, rangeStart.toLong()))
            TrimValue("SELESAI", sourceTime(clip, rangeEnd.toLong()))
            TrimValue("DURASI", TimeFormat.clock((rangeEnd - rangeStart).toLong()))
        }
    }

    ToolSection(title = "Split") {
        ToolValueRow("Posisi", TimeFormat.clock(playheadLocal.toLong()))
        Slider(
            value = playheadLocal,
            onValueChange = { playheadLocal = it },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        ZcutSecondaryButton(
            text = "Split di posisi ini",
            onClick = { onSplit(clip.startMs + playheadLocal.toLong()) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        ZcutSecondaryButton(
            text = "Duplicate",
            onClick = onDuplicate,
            modifier = Modifier.weight(1f)
        )
        ZcutSecondaryButton(
            text = "Hapus",
            onClick = { onDelete(false) },
            modifier = Modifier.weight(1f)
        )
    }

    ToolSection(title = "Ripple") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Geser klip setelahnya saat dihapus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(checked = ripple, onCheckedChange = { ripple = it })
        }
        if (ripple) {
            ZcutSecondaryButton(
                text = "Hapus dengan ripple",
                onClick = { onDelete(true) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    ToolHint(
        "Trim memakai source time range. Klip lain tidak bergeser kecuali kamu " +
            "memilih ripple."
    )
}

@Composable
private fun TrimValue(label: String, value: String) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Presentation offset inside the clip converted back to source time, so the
 * numbers match what the decoder actually reads even when a speed curve is set.
 */
private fun sourceTime(clip: Clip, presentationOffsetMs: Long): String {
    val sourceDuration = clip.effectiveSourceDurationMs.coerceAtLeast(0L)
    val offset = SpeedCurve.sourceDurationForPresentationMs(
        sourceDurationMs = sourceDuration,
        speed = clip.speed.safeSpeed,
        curve = clip.speed.curve,
        presentationMs = presentationOffsetMs.coerceAtLeast(0L)
    )
    return TimeFormat.clock(clip.sourceStartMs + offset)
}
