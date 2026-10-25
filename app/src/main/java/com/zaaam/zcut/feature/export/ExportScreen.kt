package com.zaaam.zcut.feature.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.ExportDefaults
import com.zaaam.zcut.core.domain.ExportPlan
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.coverAsset
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ListRow
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.exportViewModelFactory
import java.util.Locale

@Composable
fun ExportScreen(
    projectId: String,
    onBack: () -> Unit,
    onExported: (ExportResultUi) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ExportViewModel = viewModel(
        key = "export:$projectId",
        factory = exportViewModelFactory(container, projectId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val result = state.result
    LaunchedEffect(result) {
        result?.let(onExported)
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Export",
                subtitle = "Render lokal · tanpa watermark",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "GRATIS") }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        when {
            state.loading -> Box(modifier, contentAlignment = Alignment.Center) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            state.error != null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Gagal memuat draft",
                    message = state.error.orEmpty(),
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            state.notFound || state.project == null -> Box(
                modifier,
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> ExportBody(
                project = state.project!!,
                phase = state.phase,
                percent = state.percent,
                smartCover = state.smartCover,
                message = state.message,
                onSmartCover = viewModel::setSmartCover,
                onExport = viewModel::startExport,
                onCancel = viewModel::cancelExport,
                onDismissMessage = viewModel::clearMessage
            )
        }
    }
}

@Composable
private fun ExportBody(
    project: Project,
    phase: ExportPhase,
    percent: Float,
    smartCover: Boolean,
    message: String?,
    onSmartCover: (Boolean) -> Unit,
    onExport: () -> Unit,
    onCancel: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val preset = project.settings.exportPreset
    val durationMs = project.timeline.durationMs
    val running = phase == ExportPhase.PREPARING ||
        phase == ExportPhase.RENDERING ||
        phase == ExportPhase.FINALIZING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        ExportPreviewCard(project = project)

        ToolSection(title = "Setelan render") {
            KvRow("Resolusi", ExportDefaults.resolutionLabel(preset.resolution))
            KvRow("Frame rate", "${project.frameRate} fps")
            KvRow("Format", ExportPlan.formatLabel(preset.format))
            KvRow("Audio", "AAC")
            KvRow("Bitrate", "Otomatis")
            ToolHint(
                text = "Resolusi mengikuti setelan draft. Encoder perangkat menentukan " +
                    "frame rate, bitrate, dan sample rate akhir."
            )
        }

        ToolSection {
            ListRow(
                title = "Smart cover",
                subtitle = "Simpan satu frame sebagai cover ekspor",
                trailing = {
                    Switch(checked = smartCover, onCheckedChange = onSmartCover)
                }
            )
        }

        ToolSection {
            ListRow(
                title = "Watermark",
                subtitle = "Tidak pernah ditambahkan",
                trailing = { StatusBadge(text = "OFF") }
            )
        }

        if (running) {
            ToolSection(title = "Merender") {
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = phaseLabel(phase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%d%%", (percent * 100f).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (message != null) {
            ToolSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissMessage) { Text("Tutup") }
                }
            }
        }

        if (running) {
            ZcutSecondaryButton(
                text = "Batalkan",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ZcutPrimaryButton(
                text = "Export video",
                onClick = onExport,
                enabled = project.timeline.tracks.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        ToolHint(
            text = "Render berjalan di perangkat dengan Media3. Estimasi " +
                ExportPlan.sizeLabel(ExportPlan.estimatedSizeBytes(preset, durationMs)) +
                " · " + ExportPlan.durationLabel(durationMs) +
                ". Tidak ada unggahan."
        )

        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

@Composable
private fun ExportPreviewCard(project: Project) {
    val container = LocalAppContainer.current
    val asset = project.coverAsset()
    val thumbnail by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else container.mediaThumbnails.load(asset, 720)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = "Cover draft",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Tanpa cover",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(ZcutSpacing.sm))
        Column {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = project.let { ExportDefaults.summary(it.settings.exportPreset.resolution, it.frameRate) } +
                    " · " + ExportPlan.durationLabel(project.timeline.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun phaseLabel(phase: ExportPhase): String = when (phase) {
    ExportPhase.PREPARING -> "Menyiapkan timeline..."
    ExportPhase.RENDERING -> "Merender video..."
    ExportPhase.FINALIZING -> "Menyelesaikan file..."
    ExportPhase.DONE -> "Selesai"
    ExportPhase.FAILED -> "Gagal"
    ExportPhase.IDLE -> "Siap"
}
