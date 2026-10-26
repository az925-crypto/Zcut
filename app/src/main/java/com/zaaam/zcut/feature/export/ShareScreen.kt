package com.zaaam.zcut.feature.export

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.core.domain.ExportPlan
import com.zaaam.zcut.core.media.AudioDuration
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.feature.templates.TemplateArtBox
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One channel tile. A null package means "let Android choose". */
private data class ShareChannel(
    val id: String,
    val label: String,
    val packageName: String?,
    val mark: String
)

private val ShareChannels = listOf(
    ShareChannel("tiktok", "TikTok", "com.zhiliaoapp.musically", "T"),
    ShareChannel("instagram", "Instagram", "com.instagram.android", "IG"),
    ShareChannel("youtube", "YouTube", "com.google.android.youtube", "YT"),
    ShareChannel("whatsapp", "WhatsApp", "com.whatsapp", "WA"),
    ShareChannel("copy", "Copy", null, "⧉")
)

private const val SAMPLE_CAPTION =
    "the quiet side of the city at midnight #zcut #nightwalk #tokyo"

/**
 * Share screen (F27). Local-only: the file is handed to Android's share sheet
 * (optionally aimed at one installed app) or copied to the gallery. Publishing
 * to a platform account needs that platform's SDK/credentials and stays
 * backend-only, which the panel states plainly.
 */
@Composable
fun ShareScreen(
    fileName: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val file = remember(fileName) {
        fileName?.takeIf { it.isNotBlank() }?.let { name ->
            runCatching {
                val exportDir = container.exportDir.canonicalFile
                val candidate = File(exportDir, name).canonicalFile
                candidate.takeIf { it.parentFile == exportDir && it.isFile }
            }.getOrNull()
        }
    }

    var caption by remember { mutableStateOf(SAMPLE_CAPTION) }
    var selected by remember { mutableStateOf<ShareChannel?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val thumbnail by rememberThumbnail(key = file?.absolutePath) {
        if (file == null) null else container.mediaThumbnails.load(
            MediaAsset(
                displayName = file.name,
                mimeType = "video/mp4",
                kind = MediaKind.VIDEO,
                localPath = file.absolutePath
            ),
            480
        )
    }
    val durationMs by produceState(initialValue = 0L, file) {
        value = file?.let { path ->
            withContext(Dispatchers.IO) { AudioDuration.of(path.absolutePath) }
        } ?: 0L
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Share",
                subtitle = "Share sheet · no watermark",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "READY") }
            )
        }
    ) { insets ->
        if (file == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Berkas export tidak ditemukan",
                    message = "Export video dulu, lalu buka Share dari layar hasil export. " +
                        "Zcut tidak mengunggah apa pun."
                )
            }
            return@ZcutScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!,
                            contentDescription = "Pratinjau hasil export",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        TemplateArtBox(artKey = "night", modifier = Modifier.fillMaxSize())
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Color.Black.copy(alpha = 0.55f)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "1080p · " + ExportPlan.durationLabel(durationMs) +
                                " · " + ExportPlan.sizeLabel(file.length()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(
                text = "Caption",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(CAPTION_LIMIT) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Tulis caption untuk dibagikan…") }
            )

            Text(
                text = "Share to",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShareChannels.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { channel ->
                            ChannelTile(
                                channel = channel,
                                selected = selected?.id == channel.id,
                                onClick = {
                                    notice = null
                                    if (channel.id == "copy") {
                                        clipboard.setText(AnnotatedString(caption))
                                        notice = if (caption.isBlank()) {
                                            "Caption kosong; tidak ada yang disalin."
                                        } else {
                                            "Caption disalin ke clipboard."
                                        }
                                    } else {
                                        selected = if (selected?.id == channel.id) null else channel
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            ZcutPrimaryButton(
                text = "Share now",
                onClick = {
                    val opened = ExportDelivery.share(
                        context = context,
                        file = file,
                        targetPackage = selected?.packageName
                    )
                    notice = if (opened) {
                        if (selected == null) {
                            "Share sheet dibuka. Zcut tidak mengunggah apa pun."
                        } else {
                            "${selected?.label} dibuka lewat share sheet Android."
                        }
                    } else {
                        "Tidak ada aplikasi yang bisa membagikan video ini."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            ZcutSecondaryButton(
                text = if (saving) "Menyimpan…" else "Save to gallery",
                onClick = {
                    if (saving) return@ZcutSecondaryButton
                    saving = true
                    scope.launch {
                        val location = withContext(Dispatchers.IO) {
                            ExportDelivery.saveToGallery(context, file)
                        }
                        saving = false
                        notice = if (location == null) {
                            "Gagal menyimpan ke galeri."
                        } else {
                            "Tersimpan di $location"
                        }
                    }
                },
                enabled = !saving,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publish langsung ke platform",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    BackendBadge(featureId = "F27")
                }
                ToolHint(
                    "Login, metadata, dan upload ke akun TikTok/Instagram/YouTube " +
                        "membutuhkan SDK/API dan kredensial masing-masing; itu BUTUH " +
                        "BACKEND. Tombol di atas hanya membuka share sheet Android, " +
                        "tanpa watermark dan tanpa server Zcut."
                )
            }

            Spacer(Modifier.height(ZcutSpacing.xl))
        }
    }
}

@Composable
private fun ChannelTile(
    channel: ShareChannel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZcutCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = ZcutSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.mark,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Text(
                text = channel.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private const val CAPTION_LIMIT = 2_000
