package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.TimelineEditor
import com.zaaam.zcut.core.domain.TransitionSide
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TransitionConfig
import com.zaaam.zcut.core.model.TransitionType
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.util.Locale
import kotlin.math.roundToLong

private val LibraryTypes = listOf(
    TransitionType.DISSOLVE,
    TransitionType.FADE,
    TransitionType.SLIDE_LEFT,
    TransitionType.SLIDE_RIGHT,
    TransitionType.ZOOM,
    TransitionType.WHIP,
    TransitionType.FLASH,
    TransitionType.CUT
)

@Composable
fun TransitionScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "transition:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Transitions",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · batas transisi"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        TransitionBody(
            project = project,
            clip = clip,
            onApply = { side, config -> viewModel.setTransition(side, config) }
        )
    }
}

@Composable
private fun TransitionBody(
    project: Project,
    clip: Clip,
    onApply: (TransitionSide, TransitionConfig?) -> Unit
) {
    val track = project.timeline.tracks.firstOrNull { candidate ->
        candidate.clips.any { it.id == clip.id }
    }
    val index = track?.clips?.indexOfFirst { it.id == clip.id } ?: -1
    val previous = track?.clips?.getOrNull(index - 1)
    val next = track?.clips?.getOrNull(index + 1)
    val initialSide = if (next != null) TransitionSide.OUT else TransitionSide.IN
    var side by remember(clip.id, previous?.id, next?.id) { mutableStateOf(initialSide) }
    val neighbour = if (side == TransitionSide.IN) previous else next
    val active = if (side == TransitionSide.IN) clip.transitionIn else clip.transitionOut
    val maxDuration = if (neighbour != null) {
        minOf(clip.durationMs, neighbour.durationMs)
    } else {
        0L
    }
    var durationMs by remember(clip.id, side, active?.type, active?.durationMs) {
        mutableLongStateOf(active?.durationMs ?: DEFAULT_DURATION_MS)
    }
    val maxSafeDuration = maxDuration.coerceAtLeast(TimelineEditor.MIN_TRANSITION_MS)
    val effectiveDuration = durationMs.coerceIn(
        TimelineEditor.MIN_TRANSITION_MS,
        maxSafeDuration
    )

    val first = if (side == TransitionSide.IN) neighbour else clip
    val second = if (side == TransitionSide.IN) clip else neighbour
    TransitionPreview(
        project = project,
        first = first,
        second = second,
        config = active
    )

    ToolSection(title = "Batas") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            if (previous != null) {
                ToolChip(
                    label = "Masuk",
                    selected = side == TransitionSide.IN,
                    onClick = { side = TransitionSide.IN }
                )
            }
            if (next != null) {
                ToolChip(
                    label = "Keluar",
                    selected = side == TransitionSide.OUT,
                    onClick = { side = TransitionSide.OUT }
                )
            }
        }
        if (neighbour == null) {
            ToolHint(
                "Klip ini belum punya tetangga di sisi tersebut. Transisi " +
                    "menempel pada batas dua klip."
            )
        } else {
            ToolValueRow("Klip A", assetName(project, first))
            ToolValueRow("Klip B", assetName(project, second))
            ToolValueRow("Maksimum", TimeFormat.clockMillis(maxDuration))
        }
    }

    if (neighbour != null) {
        ToolSection(title = "Durasi") {
            if (active == null) {
                ToolHint("Pilih jenis transisi dulu untuk mengatur durasinya.")
            } else {
                ToolSlider(
                    label = "Durasi transisi",
                    value = effectiveDuration.toFloat(),
                    valueRange = TimelineEditor.MIN_TRANSITION_MS.toFloat()..
                        maxSafeDuration.toFloat(),
                    onValueChange = { durationMs = it.roundToLong() },
                    onValueChangeFinished = {
                        onApply(side, active.copy(durationMs = effectiveDuration))
                    },
                    valueLabel = { formatSeconds(it) }
                )
            }
        }

        ToolSection(title = "Library") {
            LibraryTypes.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    row.forEach { type ->
                        TransitionTile(
                            type = type,
                            selected = if (type == TransitionType.CUT) {
                                active == null
                            } else {
                                active?.type == type
                            },
                            onClick = {
                                if (type == TransitionType.CUT) {
                                    onApply(side, null)
                                } else {
                                    onApply(
                                        side,
                                        TransitionConfig(type, effectiveDuration)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (active != null) {
                ZcutSecondaryButton(
                    text = "Hapus transisi",
                    onClick = { onApply(side, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ToolHint(
                "Transisi standar berjalan lokal dan disimpan untuk export. " +
                    "Preview transisi real-time dan transisi AI generatif belum " +
                    "tersedia tanpa backend."
            )
        }
    }
}

@Composable
private fun TransitionPreview(
    project: Project,
    first: Clip?,
    second: Clip?,
    config: TransitionConfig?
) {
    val firstAsset = assetOf(project, first)
    val secondAsset = assetOf(project, second)
    val container = LocalAppContainer.current
    val firstThumb by rememberThumbnail(key = firstAsset?.id) {
        if (firstAsset == null) null else container.mediaThumbnails.load(firstAsset, 480)
    }
    val secondThumb by rememberThumbnail(key = secondAsset?.id) {
        if (secondAsset == null) null else container.mediaThumbnails.load(secondAsset, 480)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            PreviewHalf(bitmap = firstThumb, modifier = Modifier.weight(1f))
            PreviewHalf(bitmap = secondThumb, modifier = Modifier.weight(1f))
        }
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (config == null) {
                    "CUT · tanpa transisi"
                } else {
                    "${typeLabel(config.type)} · ${formatSeconds(config.durationMs.toFloat())}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = ZcutLime,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PreviewHalf(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransitionTile(
    type: TransitionType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = typeLabel(type),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = typeHint(type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun assetOf(project: Project, clip: Clip?): MediaAsset? =
    clip?.assetId?.let { id -> project.assets.firstOrNull { it.id == id } }

private fun assetName(project: Project, clip: Clip?): String =
    assetOf(project, clip)?.displayName ?: "Klip"

private fun typeLabel(type: TransitionType): String = when (type) {
    TransitionType.CUT -> "Cut"
    TransitionType.DISSOLVE -> "Dissolve"
    TransitionType.FADE -> "Fade"
    TransitionType.SLIDE_LEFT -> "Slide kiri"
    TransitionType.SLIDE_RIGHT -> "Slide kanan"
    TransitionType.ZOOM -> "Zoom"
    TransitionType.WHIP -> "Whip"
    TransitionType.FLASH -> "Flash"
}

private fun typeHint(type: TransitionType): String = when (type) {
    TransitionType.CUT -> "tanpa transisi"
    TransitionType.DISSOLVE -> "blend halus"
    TransitionType.FADE -> "ke hitam"
    TransitionType.SLIDE_LEFT -> "geser kiri"
    TransitionType.SLIDE_RIGHT -> "geser kanan"
    TransitionType.ZOOM -> "punch"
    TransitionType.WHIP -> "cepat"
    TransitionType.FLASH -> "kilat putih"
}

private fun formatSeconds(value: Float): String =
    String.format(Locale.US, "%.1f s", value / 1_000f)

private const val DEFAULT_DURATION_MS = 500L
