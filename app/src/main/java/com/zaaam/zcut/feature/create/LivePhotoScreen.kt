package com.zaaam.zcut.feature.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.LivePhotoConfig
import com.zaaam.zcut.core.domain.LivePhotoExport
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolSlider
import com.zaaam.zcut.feature.tools.ToolValueRow
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.livePhotoViewModelFactory
import java.util.Locale

/**
 * Live photo (F30). Splits a motion photo (or a video's first frame) into a
 * still, then combines the still and the motion clip into a draft. Only video
 * export is local; GIF and native motion-photo export are disabled honestly.
 */
@Composable
fun LivePhotoScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: LivePhotoViewModel = viewModel(
        factory = livePhotoViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.pick(uri) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Live photo",
                subtitle = "Motion photo toolkit",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    ZcutPrimaryButton(
                        text = if (state.busy) "…" else "Apply",
                        onClick = { viewModel.combine { id -> id?.let(onOpenEditor) } },
                        enabled = state.canCombine
                    )
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LivePreview(
                state = state,
                onPick = {
                    picker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onPlay = viewModel::showPreviewNotice
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                ZcutSecondaryButton(
                    text = "Split photo",
                    onClick = viewModel::splitPhoto,
                    enabled = state.canSplit,
                    modifier = Modifier.weight(1f)
                )
                ZcutSecondaryButton(
                    text = "Combine",
                    onClick = { viewModel.combine { id -> id?.let(onOpenEditor) } },
                    enabled = state.canCombine,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.asset == null) {
                ZcutSecondaryButton(
                    text = "Pilih motion photo / video",
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Export as",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToolChipRow {
                LivePhotoExport.entries.forEach { export ->
                    ToolChip(
                        label = export.label,
                        selected = export == state.export,
                        onClick = { viewModel.setExport(export) },
                        enabled = export.supported
                    )
                }
            }

            ToolSlider(
                label = "Duration",
                value = state.durationMs.toFloat(),
                valueRange = LivePhotoConfig.MIN_DURATION_MS.toFloat()..
                    LivePhotoConfig.MAX_DURATION_MS.toFloat(),
                onValueChange = viewModel::setDurationMs,
                onValueChangeFinished = {},
                valueLabel = { formatSeconds(it.toLong()) }
            )

            ToolSection {
                ToolValueRow(
                    label = "Key photo",
                    value = if (state.still != null) "Frame 01" else "Belum di-split"
                )
                ToolValueRow(
                    label = "Motion clip",
                    value = when {
                        state.video == null -> "Tidak ada"
                        state.asset?.kind == MediaKind.VIDEO -> "Video asli"
                        else -> "Diekstrak dari motion photo"
                    }
                )
                ToolValueRow(label = "Stabilize", value = "Tidak diterapkan")
                ToolValueRow(
                    label = "Audio",
                    value = if (state.video?.hasAudio == true) "Audio video asli" else "Tidak ada"
                )
            }

            ToolHint(
                "Semua proses lokal, tanpa upload. Motion photo dipecah dengan memindai " +
                    "video MP4 yang tertanam; kalau tidak ada, Combine memakai still " +
                    "saja. GIF, Still + motion, stabilisasi, dan motion strength belum " +
                    "didukung."
            )

            if (state.notice != null) {
                ToolSection {
                    Text(
                        text = state.notice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(ZcutSpacing.xl))
        }
    }
}

@Composable
private fun LivePreview(
    state: LivePhotoUiState,
    onPick: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF08090D))
    ) {
        CreateThumb(
            asset = state.asset,
            modifier = Modifier.fillMaxSize(),
            sizePx = 720
        )

        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "LIVE · " + formatSeconds(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        if (state.asset == null) {
            ZcutSecondaryButton(
                text = "Pilih media",
                onClick = onPick,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Surface(
                onClick = onPlay,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Pratinjau gerak",
                        tint = Color.White
                    )
                }
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = if (state.hasMotionVideo) "STILL + MOTION" else "STILL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

private fun formatSeconds(durationMs: Long): String =
    String.format(Locale.US, "%.1fs", durationMs.coerceAtLeast(0L) / 1000f)
