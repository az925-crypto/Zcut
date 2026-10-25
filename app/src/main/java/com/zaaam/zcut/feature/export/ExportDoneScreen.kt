package com.zaaam.zcut.feature.export

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.core.domain.ExportDefaults
import com.zaaam.zcut.core.domain.ExportPlan
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ListRow
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportDoneScreen(
    projectId: String,
    fileName: String,
    durationMs: Long,
    sizeBytes: Long,
    coverFileName: String?,
    onBackHome: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = remember(fileName) { File(container.exportDir, fileName) }
    var notice by remember { mutableStateOf<String?>(null) }

    val project by produceState<Project?>(initialValue = null, projectId) {
        value = runCatching { container.projectRepository.get(projectId) }.getOrNull()
    }

    ZcutScaffold { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "RENDER SELESAI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Video kamu siap.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = (project?.name ?: "Draft") + resolutionSuffix(project) +
                    " · " + ExportPlan.durationLabel(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(ZcutSpacing.sm))

            ZcutCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ExportPlan.sizeLabel(sizeBytes) + " · tersimpan di ruang kerja app",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (coverFileName != null) {
                        Text(
                            text = "Cover: $coverFileName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                ZcutPrimaryButton(
                    text = "Bagikan",
                    onClick = {
                        notice = if (ExportDelivery.share(context, file)) {
                            null
                        } else {
                            "Tidak ada aplikasi yang bisa membagikan video ini."
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ZcutSecondaryButton(
                    text = "Simpan",
                    onClick = {
                        scope.launch {
                            val location = withContext(Dispatchers.IO) {
                                ExportDelivery.saveToGallery(context, file)
                            }
                            notice = if (location == null) {
                                "Gagal menyimpan ke galeri."
                            } else {
                                "Tersimpan di $location"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ZcutSecondaryButton(
                text = "Kembali ke home",
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth()
            )

            if (notice != null) {
                ToolSection {
                    Text(
                        text = notice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ToolSection {
                ListRow(
                    title = "Watermark",
                    trailing = { StatusBadge(text = "TIDAK ADA") }
                )
                ListRow(
                    title = "Paywall",
                    trailing = { StatusBadge(text = "TIDAK ADA") }
                )
                ListRow(
                    title = "Unggahan",
                    trailing = { StatusBadge(text = "TIDAK ADA") }
                )
                ToolHint(
                    text = "Video ditulis ke penyimpanan perangkat. Zcut tidak mengunggah " +
                        "apa pun dan tidak memanggil server apa pun."
                )
            }

            Spacer(Modifier.height(ZcutSpacing.xl))
        }
    }
}

private fun resolutionSuffix(project: Project?): String {
    val preset = project?.settings?.exportPreset ?: return ""
    return " · " + ExportDefaults.resolutionLabel(preset.resolution)
}
