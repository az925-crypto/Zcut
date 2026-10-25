package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.MattingEditor
import com.zaaam.zcut.core.media.MattingComposer
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.render.SubjectMask
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of running the on-device segmenter over the clip's still frame. */
private sealed interface MaskProbe {
    data object Pending : MaskProbe
    data object NoFrame : MaskProbe
    data object Unavailable : MaskProbe
    data class Mask(val value: SubjectMask) : MaskProbe
}

@Composable
fun MattingScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "matting:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Cutout",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        MattingBody(
            project = project,
            clip = clip,
            onMatting = viewModel::setMatting
        )
    }
}

@Composable
private fun MattingBody(
    project: Project,
    clip: Clip,
    onMatting: (MattingConfig?) -> Unit
) {
    val container = LocalAppContainer.current
    val asset = clip.assetId?.let { id -> project.assets.firstOrNull { it.id == id } }
    val frame by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else container.mediaThumbnails.load(asset, 720)
    }

    // Sliders edit a local draft (instant number feedback); the draft is committed
    // on release, so the expensive composite below runs once per commit.
    var draft by remember(clip.id, clip.matting) {
        mutableStateOf(clip.matting ?: MattingConfig())
    }
    val committed = clip.matting

    val probe by produceState<MaskProbe>(MaskProbe.Pending, frame) {
        value = when (val bitmap = frame) {
            null -> MaskProbe.NoFrame
            else -> container.subjectSegmenter.segment(bitmap.asAndroidBitmap())
                ?.let(MaskProbe::Mask)
                ?: MaskProbe.Unavailable
        }
    }

    val composite by produceState<ImageBitmap?>(null, probe, committed) {
        val mask = (probe as? MaskProbe.Mask)?.value
        val source = frame
        val config = committed
        value = if (mask == null || source == null || config == null || !config.enabled) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    MattingComposer.compose(source.asAndroidBitmap(), mask, config).asImageBitmap()
                }.getOrNull()
            }
        }
    }

    val enabled = committed?.enabled == true
    val confidence = (probe as? MaskProbe.Mask)?.let {
        MattingEditor.subjectConfidencePercent(it.value.confidence)
    }
    val busy = enabled && probe == MaskProbe.Pending

    MattingPreview(
        label = when {
            !enabled -> "CUTOUT MATI"
            probe == MaskProbe.NoFrame -> "TANPA MEDIA"
            probe == MaskProbe.Pending -> "MENSEGMENTASI..."
            probe == MaskProbe.Unavailable -> "SEGMENTASI TIDAK TERSEDIA"
            confidence == null -> "TIDAK ADA SUBYEK"
            composite == null -> "CUTOUT GAGAL DIBUAT"
            else -> MattingEditor.subjectLabel(confidence)
        },
        frame = frame,
        composite = composite,
        busy = busy
    )

    ToolSection(title = "Subjek") {
        if (probe == MaskProbe.NoFrame) {
            ToolHint("Klip ini belum punya media. Tambahkan media untuk memakai cutout.")
        } else if (probe == MaskProbe.Unavailable) {
            ToolHint(
                "Segmentasi tidak tersedia di perangkat ini (model ML Kit gagal dimuat). " +
                    "Fitur tetap terbuka, hanya hasilnya belum bisa ditampilkan."
            )
        } else if (confidence == null && probe is MaskProbe.Mask) {
            ToolHint("Model tidak menemukan subjek pada frame ini. Coba frame lain atau klip lain.")
        } else {
            ToolHint(
                "Segmentasi subjek berjalan di perangkat dengan model ML Kit yang dibundel APK. " +
                    "Tidak ada gambar yang dikirim ke server."
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktifkan cutout",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    draft = draft.copy(enabled = on)
                    onMatting(draft)
                }
            )
        }

        if (enabled) {
            ToolSlider(
                label = "Edge refine",
                value = draft.edgeRefine,
                valueRange = 0f..1f,
                onValueChange = { draft = draft.copy(edgeRefine = it) },
                onValueChangeFinished = { onMatting(draft) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
            ToolSlider(
                label = "Shadow",
                value = draft.shadow,
                valueRange = 0f..1f,
                onValueChange = { draft = draft.copy(shadow = it) },
                onValueChangeFinished = { onMatting(draft) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
        }
    }

    if (enabled) {
        BackgroundSection(
            draft = draft,
            onCommit = { updated ->
                draft = updated
                onMatting(updated)
            }
        )
    }
}

@Composable
private fun BackgroundSection(
    draft: MattingConfig,
    onCommit: (MattingConfig) -> Unit
) {
    ToolSection(title = "Background") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            ToolChip(
                label = "Remove BG",
                selected = draft.background == MattingBackground.TRANSPARENT,
                onClick = { onCommit(draft.copy(background = MattingBackground.TRANSPARENT)) }
            )
            ToolChip(
                label = "Blur BG",
                selected = draft.background == MattingBackground.BLUR,
                onClick = { onCommit(draft.copy(background = MattingBackground.BLUR)) }
            )
        }

        if (draft.background == MattingBackground.BLUR) {
            // Local draft so a drag does not write the draft file on every step.
            var blurDraft by remember(draft.blur) { mutableStateOf(draft.blur) }
            ToolSlider(
                label = "Blur",
                value = blurDraft,
                valueRange = 0f..1f,
                onValueChange = { blurDraft = it },
                onValueChangeFinished = { onCommit(draft.copy(blur = blurDraft)) },
                valueLabel = { "${(it * 100).toInt()}%" }
            )
        }

        ToolChipRow {
            MattingEditor.COLOR_PRESETS.forEach { (label, argb) ->
                ToolChip(
                    label = label,
                    selected = draft.background == MattingBackground.COLOR &&
                        draft.colorArgb == argb,
                    onClick = {
                        onCommit(
                            draft.copy(background = MattingBackground.COLOR, colorArgb = argb)
                        )
                    }
                )
            }
            ToolChip(
                label = "AI",
                selected = draft.background == MattingBackground.AI,
                onClick = { onCommit(draft.copy(background = MattingBackground.AI)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            val swatch = when (draft.background) {
                MattingBackground.COLOR -> draft.colorArgb
                MattingBackground.TRANSPARENT -> 0x00000000
                else -> draft.colorArgb
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(swatch))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Text(
                text = MattingEditor.summary(draft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (MattingEditor.requiresBackend(draft.background)) {
            ToolHint(
                "Latar AI (generate gambar latar) BUTUH BACKEND, jadi belum dihasilkan di sini. " +
                    "Prompt disimpan di draft supaya bisa dipakai begitu backend tersedia."
            )
        }
        ToolHint(
            "Cutout pratinjau memakai satu frame klip. Matte per frame saat export belum ada " +
                "(batas render M13), jadi video final belum ikut dipotong."
        )
    }
}

/**
 * Checkerboard + composited frame, mirroring the mockup: transparency has to be
 * visible, and a solid backdrop would hide the fact that the background is gone.
 */
@Composable
private fun MattingPreview(
    label: String,
    frame: ImageBitmap?,
    composite: ImageBitmap?,
    busy: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Checkerboard()

        val shown = composite ?: frame
        if (shown != null) {
            Image(
                bitmap = shown,
                contentDescription = "Pratinjau cutout",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
        } else if (shown == null) {
            Text(
                text = "Pratinjau tidak tersedia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun Checkerboard() {
    val light = MaterialTheme.colorScheme.surfaceVariant
    val dark = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = 14.dp.toPx()
        drawRect(color = dark)
        var row = 0
        var y = 0f
        while (y < size.height) {
            var column = 0
            var x = 0f
            while (x < size.width) {
                if ((row + column) % 2 == 0) {
                    drawRect(color = light, topLeft = Offset(x, y), size = Size(cell, cell))
                }
                x += cell
                column++
            }
            y += cell
            row++
        }
    }
}
