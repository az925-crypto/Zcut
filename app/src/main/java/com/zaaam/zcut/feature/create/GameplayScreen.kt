package com.zaaam.zcut.feature.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.zaaam.zcut.core.domain.FacecamShape
import com.zaaam.zcut.core.domain.GameplayConfig
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
import com.zaaam.zcut.ui.gameplayViewModelFactory

/**
 * Gameplay overlay (F30). Pick gameplay + facecam, choose the bubble corner,
 * size, border and shape; Apply renders one still composite into a draft.
 */
@Composable
fun GameplayScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: GameplayViewModel = viewModel(
        factory = gameplayViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val gameplayPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.pickGameplay(uri) }
    val facecamPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.pickFacecam(uri) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Gameplay",
                subtitle = "Game + facecam overlay",
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

            GameplayPreview(state = state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                ZcutSecondaryButton(
                    text = if (state.gameplay == null) "Pilih gameplay" else "Ganti gameplay",
                    onClick = {
                        gameplayPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f)
                )
                ZcutSecondaryButton(
                    text = if (state.facecam == null) "Pilih facecam" else "Ganti facecam",
                    onClick = {
                        facecamPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Facecam position",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FacecamPositionPicker(
                selected = state.position,
                onSelect = viewModel::setPosition
            )

            ToolSlider(
                label = "Facecam size",
                value = state.sizePercent.toFloat(),
                valueRange = GameplayConfig.MIN_SIZE_PERCENT.toFloat()..
                    GameplayConfig.MAX_SIZE_PERCENT.toFloat(),
                onValueChange = { viewModel.setSizePercent(it.toInt()) },
                onValueChangeFinished = {},
                valueLabel = { "${it.toInt()}%" }
            )
            ToolSlider(
                label = "Border",
                value = state.borderPercent.toFloat(),
                valueRange = 0f..GameplayConfig.MAX_BORDER_PERCENT.toFloat(),
                onValueChange = { viewModel.setBorderPercent(it.toInt()) },
                onValueChangeFinished = {},
                valueLabel = { it.toInt().toString() }
            )

            Text(
                text = "Shape",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToolChipRow {
                FacecamShape.entries.forEach { shape ->
                    ToolChip(
                        label = shape.label,
                        selected = shape == state.shape,
                        onClick = { viewModel.setShape(shape) }
                    )
                }
            }

            ToolSection {
                ToolValueRow(label = "Chroma key", value = "Off · belum didukung")
                ToolValueRow(label = "Rekaman", value = "MediaProjection belum ada")
                ToolValueRow(label = "Game audio", value = "Tidak ada (still)")
            }

            ToolHint(
                "Zcut memakai frame awal gameplay dan facecam untuk satu still " +
                    "komposit. Rekaman layar MediaProjection, chroma key, dan ekspor " +
                    "video overlay belum didukung."
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
private fun GameplayPreview(state: GameplayUiState) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF08090D))
    ) {
        CreateThumb(
            asset = state.gameplay,
            modifier = Modifier.fillMaxSize(),
            sizePx = 720
        )

        val camSize = maxWidth * (state.sizePercent / 100f)
        val shape = if (state.shape == FacecamShape.CIRCLE) {
            CircleShape
        } else {
            RoundedCornerShape(12.dp)
        }
        val borderWidth = (state.borderPercent * 0.5f).dp
        CreateThumb(
            asset = state.facecam,
            modifier = Modifier
                .align(state.position.toAlignment())
                .size(camSize)
                .clip(shape)
                .border(borderWidth, Color.White.copy(alpha = 0.45f), shape),
            sizePx = 360
        )
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = "GAMEPLAY + FACECAM",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}
