package com.zaaam.zcut.feature.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.AutoCut
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.aiClipperViewModelFactory
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolSlider
import java.util.Locale
import kotlin.math.roundToLong

/**
 * AI Clipper (F22). Follows the approved mockup's layout: source panel, target
 * length and style chips, one action button, the honest backend note, the scored
 * suggestions, and the bulk draft button.
 */
@Composable
fun AiClipperScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AiClipperViewModel = viewModel(
        key = "ai-clipper",
        factory = aiClipperViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.selectSource(uri) }

    val pickVideo: () -> Unit = {
        videoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "AI Clipper",
                subtitle = "Long video → short moments",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "LOCAL*", accent = ZcutLime) }
            )
        }
    ) { insets ->
        AiClipperBody(
            state = state,
            thumbnails = container.mediaThumbnails,
            onPickVideo = pickVideo,
            onTarget = viewModel::setTargetMs,
            onCustomTarget = viewModel::setCustomTarget,
            onStyle = viewModel::setStyle,
            onFind = viewModel::findHighlights,
            onDismissNotice = viewModel::clearNotice,
            onCreateOne = { window ->
                viewModel.createDraft(listOf(window)) { id -> if (id != null) onOpenEditor(id) }
            },
            onCreateAll = {
                viewModel.createDraft(state.windows) { id -> if (id != null) onOpenEditor(id) }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        )
    }
}

@Composable
private fun AiClipperBody(
    state: AiClipperUiState,
    thumbnails: MediaThumbnails,
    onPickVideo: () -> Unit,
    onTarget: (Long) -> Unit,
    onCustomTarget: (Boolean) -> Unit,
    onStyle: (ClipperStyle) -> Unit,
    onFind: () -> Unit,
    onDismissNotice: () -> Unit,
    onCreateOne: (AutoCut.Window) -> Unit,
    onCreateAll: () -> Unit,
    modifier: Modifier
) {
    var customDraft by remember(state.targetMs) {
        mutableStateOf((state.targetMs / 1_000f))
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        SourcePanel(
            source = state.source,
            importing = state.importing,
            thumbnails = thumbnails,
            onPickVideo = onPickVideo
        )

        ToolSection(title = "Target length") {
            ToolChipRow {
                AutoCut.TARGETS_MS.forEach { option ->
                    ToolChip(
                        label = "${option / 1_000}s",
                        selected = !state.targetIsCustom && option == state.targetMs,
                        onClick = {
                            onCustomTarget(false)
                            onTarget(option)
                        },
                        enabled = !state.analyzing
                    )
                }
                ToolChip(
                    label = "Custom",
                    selected = state.targetIsCustom,
                    onClick = {
                        onTarget((customDraft * 1_000f).roundToLong())
                        onCustomTarget(true)
                    },
                    enabled = !state.analyzing
                )
            }
            if (state.targetIsCustom) {
                ToolSlider(
                    label = "Panjang klip",
                    value = customDraft,
                    valueRange = (AiClipperUiState.MIN_TARGET_MS / 1_000f)..
                        (AiClipperUiState.MAX_TARGET_MS / 1_000f),
                    onValueChange = { customDraft = it },
                    onValueChangeFinished = { onTarget((customDraft * 1_000f).roundToLong()) },
                    valueLabel = { "${it.roundToLong()}s" }
                )
            }
        }

        ToolSection(title = "Style") {
            ToolChipRow {
                ClipperStyle.entries.forEach { option ->
                    ToolChip(
                        label = option.label,
                        selected = option == state.style,
                        onClick = { onStyle(option) },
                        enabled = !state.analyzing
                    )
                }
            }
        }

        ZcutPrimaryButton(
            text = when {
                state.importing -> "Mengimpor..."
                state.analyzing -> "Menganalisis..."
                else -> "Find highlights"
            },
            onClick = onFind,
            enabled = state.hasSource && !state.analyzing && !state.importing,
            modifier = Modifier.fillMaxWidth()
        )

        ToolSection {
            Text(
                text = "ASR dan highlight scoring penuh ditandai BUTUH BACKEND. " +
                    "Heuristik energi berjalan lokal di perangkat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BackendBadge(featureId = "F22")
        }

        if (state.notice != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.notice.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissNotice) { Text("Tutup") }
            }
        }

        if (state.analyzing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (state.hasResults) {
            Text(
                text = "Suggested cuts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.windows.forEach { window ->
                SuggestionRow(
                    window = window,
                    source = state.source,
                    thumbnails = thumbnails,
                    onClick = { onCreateOne(window) }
                )
            }
            ZcutSecondaryButton(
                text = "Create draft from all ${state.windows.size}",
                onClick = onCreateAll,
                enabled = !state.creating,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ToolHint(
            "Analisis membaca audio yang bisa didekode dalam jendela ${AutoCut.HOP_MS} ms, " +
                "maksimal 60 detik." +
                if (state.truncated) {
                    " Video ini lebih panjang, jadi hanya bagian awal yang dinilai."
                } else {
                    ""
                }
        )
        ToolHint(
            "Skor adalah RMS level jendela yang dipetakan ke 0–100, bukan model " +
                "penilai. Tiap saran jadi klip nyata dengan offset sumber aslinya."
        )
        if (state.analysedMs > 0L) {
            ToolHint("Audio terbaca: ${formatDuration(state.analysedMs)}.")
        }

        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

@Composable
private fun SourcePanel(
    source: MediaAsset?,
    importing: Boolean,
    thumbnails: MediaThumbnails,
    onPickVideo: () -> Unit
) {
    val thumbnailKey = source?.localPath
    val thumbnail by rememberThumbnail(key = thumbnailKey) {
        source?.let { thumbnails.load(it, 320) }
    }

    ToolSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (source == null) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    thumbnail?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source?.displayName ?: "Belum ada video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = source?.let(::sourceSummary)
                        ?: "Pilih satu video panjang untuk dipotong.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ZcutIconButton(
                icon = Icons.Filled.PlayArrow,
                contentDescription = if (source == null) "Pilih video" else "Ganti video",
                onClick = onPickVideo,
                enabled = !importing
            )
        }

        if (source == null) {
            ZcutPrimaryButton(
                text = "Pilih video",
                onClick = onPickVideo,
                enabled = !importing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    window: AutoCut.Window,
    source: MediaAsset?,
    thumbnails: MediaThumbnails,
    onClick: () -> Unit
) {
    val thumbnail by rememberThumbnail(key = source?.localPath) {
        source?.let { thumbnails.load(it, 320) }
    }

    ZcutCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ZcutSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                thumbnail?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                StatusBadge(
                    text = window.score.toString(),
                    accent = ZcutLime,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatTime(window.startMs)} · ${window.durationMs / 1_000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = window.kind.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Buat draft",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** "08:42 · 1080p · 1.2 GB", matching the mockup's second line. */
private fun sourceSummary(asset: MediaAsset): String {
    val duration = if (asset.durationMs > 0L) formatTime(asset.durationMs) else "--:--"
    val resolution = if (asset.height > 0) "${asset.height}p" else "—"
    return "$duration · $resolution · ${formatSize(asset.sizeBytes)}"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1_000L
    return String.format(
        Locale.US,
        "%02d:%02d.%d",
        totalSeconds / 60L,
        totalSeconds % 60L,
        (ms.coerceAtLeast(0L) % 1_000L) / 100L
    )
}

private fun formatDuration(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1_000L
    return "${seconds / 60L}m ${seconds % 60L}s"
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes >= 1_073_741_824L -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    else -> "${bytes / 1_024L} KB"
}