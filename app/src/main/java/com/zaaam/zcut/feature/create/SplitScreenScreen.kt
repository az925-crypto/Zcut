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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.zaaam.zcut.core.domain.SplitLayout
import com.zaaam.zcut.core.domain.SplitScreenConfig
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
import com.zaaam.zcut.ui.splitScreenViewModelFactory

/**
 * Split screen (F30). The mockup's preview grid, layout picker, ratio chips and
 * border/corner sliders; Apply renders a still composite and opens the draft.
 */
@Composable
fun SplitScreenScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: SplitScreenViewModel = viewModel(
        factory = splitScreenViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(SplitLayout.MAX_CELLS)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addMedia(uris) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Split screen",
                subtitle = "${state.layout.label} · center crop",
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
                        onClick = { viewModel.apply { id -> id?.let(onOpenEditor) } },
                        enabled = state.ready
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

            SplitPreview(state = state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                Text(
                    text = "Video ${state.assets.size}/${state.requiredVideos}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                ZcutSecondaryButton(
                    text = if (state.assets.isEmpty()) "Pilih video" else "Tambah video",
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    enabled = !state.busy && state.assets.size < SplitLayout.MAX_CELLS
                )
            }

            if (state.assets.isNotEmpty()) {
                ToolChipRow {
                    state.assets.forEachIndexed { index, asset ->
                        ToolChip(
                            label = "Video ${index + 1} ×",
                            selected = false,
                            onClick = { viewModel.removeAsset(asset.id) },
                            enabled = !state.busy
                        )
                    }
                }
            }

            Text(
                text = "Layout",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SplitLayoutPicker(
                selected = state.layout,
                onSelect = viewModel::setLayout
            )

            Text(
                text = "Ratio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AspectChips(
                selected = state.aspectRatio,
                onSelect = viewModel::setAspectRatio
            )

            ToolSlider(
                label = "Border",
                value = state.borderPercent.toFloat(),
                valueRange = 0f..SplitScreenConfig.MAX_BORDER_PERCENT.toFloat(),
                onValueChange = { viewModel.setBorderPercent(it.toInt()) },
                onValueChangeFinished = {},
                valueLabel = { it.toInt().toString() }
            )
            ToolSlider(
                label = "Corner radius",
                value = state.cornerPercent.toFloat(),
                valueRange = 0f..SplitScreenConfig.MAX_CORNER_PERCENT.toFloat(),
                onValueChange = { viewModel.setCornerPercent(it.toInt()) },
                onValueChangeFinished = {},
                valueLabel = { it.toInt().toString() }
            )

            ToolSection {
                ToolValueRow(label = "Background", value = "#08090D")
                ToolValueRow(label = "Center crop", value = "On")
                ToolValueRow(label = "Audio", value = "Tidak ada (still)")
            }

            ToolHint(
                "Zcut merender satu still komposit resolusi tinggi dari frame awal " +
                    "tiap video, lalu memasukkannya ke draft. Pemutaran dan ekspor " +
                    "beberapa video serentak belum didukung."
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
private fun SplitPreview(state: SplitScreenUiState) {
    val gap = (state.borderPercent * 0.5f + 1f).dp
    val corner = (state.cornerPercent * 0.5f).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(state.aspectRatio.ratio)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF08090D))
            .padding(gap)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            repeat(state.layout.rows) { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    repeat(state.layout.columns) { column ->
                        val index = row * state.layout.columns + column
                        CreateThumb(
                            asset = state.visibleAssets.getOrNull(index),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(corner)),
                            sizePx = 360
                        )
                    }
                }
            }
        }
    }
}
