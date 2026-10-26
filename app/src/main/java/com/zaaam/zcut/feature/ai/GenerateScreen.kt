package com.zaaam.zcut.feature.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.GenerationEntry
import com.zaaam.zcut.core.domain.GenerationStyle
import com.zaaam.zcut.core.domain.GenerationType
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.feature.templates.TemplateArtBox
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSegmented
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.generateViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Generate (F24). Mirrors the approved mockup: hero, prompt textarea, type /
 * aspect / style chips, the honest backend note, the Generate action and its
 * placeholder preview, and the local History grid. Nothing is generated on
 * device and no request leaves the app.
 */
@Composable
fun GenerateScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: GenerateViewModel = viewModel(
        key = "ai-generate",
        factory = generateViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val resultPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.importResult(uri) }

    val pickResult: () -> Unit = {
        resultPicker.launch(
            PickVisualMediaRequest(
                if (state.type == GenerationType.IMAGE) {
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                } else {
                    ActivityResultContracts.PickVisualMedia.VideoOnly
                }
            )
        )
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Generate",
                subtitle = "Text to image / video",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "BACKEND", accent = ZcutCoral) }
            )
        }
    ) { insets ->
        GenerateBody(
            state = state,
            thumbnails = container.mediaThumbnails,
            onPrompt = viewModel::setPrompt,
            onType = viewModel::setType,
            onAspect = viewModel::setAspectRatio,
            onStyle = viewModel::setStyle,
            onGenerate = viewModel::generate,
            onImport = pickResult,
            onDismissNotice = viewModel::clearNotice,
            onOpenEntry = { entry -> viewModel.createDraft(entry) { id -> if (id != null) onOpenEditor(id) } },
            onDeleteEntry = viewModel::deleteEntry,
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        )
    }
}

@Composable
private fun GenerateBody(
    state: GenerateUiState,
    thumbnails: MediaThumbnails,
    onPrompt: (String) -> Unit,
    onType: (GenerationType) -> Unit,
    onAspect: (AspectRatio) -> Unit,
    onStyle: (GenerationStyle) -> Unit,
    onGenerate: () -> Unit,
    onImport: () -> Unit,
    onDismissNotice: () -> Unit,
    onOpenEntry: (GenerationEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    modifier: Modifier
) {
    var selected by remember { mutableStateOf<GenerationEntry?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        HeroPanel()

        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPrompt,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Describe the shot you want…") }
        )

        ToolSegmented(
            options = listOf(GenerationType.IMAGE.label, GenerationType.VIDEO.label),
            selectedIndex = state.type.ordinal,
            onSelect = { index -> onType(GenerationType.entries[index]) }
        )

        Text(
            text = "Aspect",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolChipRow {
            AspectRatio.entries.forEach { ratio ->
                ToolChip(
                    label = ratio.label,
                    selected = ratio == state.aspectRatio,
                    onClick = { onAspect(ratio) }
                )
            }
        }

        Text(
            text = "Style",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToolChipRow {
            GenerationStyle.entries.forEach { style ->
                ToolChip(
                    label = style.label,
                    selected = style == state.style,
                    onClick = { onStyle(style) }
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Text-to-image/video, inpainting, video extend, face cover, " +
                        "dan translate membutuhkan backend/model sendiri. Tidak ada " +
                        "panggilan ke server CapCut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BackendBadge(featureId = "F24")
            }
        }

        ZcutPrimaryButton(
            text = "Generate",
            onClick = onGenerate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth()
        )

        ZcutSecondaryButton(
            text = if (state.importing) "Mengimpor…" else "Import hasil dari galeri",
            onClick = onImport,
            enabled = state.canImport,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.showBackendPanel) {
            PreviewPanel()
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

        Text(
            text = "History" + if (state.history.isNotEmpty()) " · ${state.history.size}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.history.isEmpty()) {
            Text(
                text = "Belum ada hasil. Import hasil dari galeri untuk menyimpannya " +
                    "di History dan menjadikannya draft.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            HistoryGrid(
                entries = state.history,
                thumbnails = thumbnails,
                onOpen = { entry -> selected = entry }
            )
        }

        ToolHint(
            "Generate tidak mengirim apa pun ke jaringan. History hanya berisi berkas " +
                "yang kamu impor sendiri; ketuk satu hasil untuk membuat draft."
        )

        Spacer(Modifier.height(ZcutSpacing.xl))
    }

    selected?.let { entry ->
        HistoryDialog(
            entry = entry,
            creating = state.creatingDraft,
            onDismiss = { selected = null },
            onCreateDraft = {
                selected = null
                onOpenEntry(entry)
            },
            onDelete = {
                selected = null
                onDeleteEntry(entry.id)
            }
        )
    }
}

@Composable
private fun HeroPanel() {
    val accent = Color(0xFFB69CFF)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.18f),
                            Color(0xFF59D9FF).copy(alpha = 0.07f),
                            Color(0xFFC9FF4A).copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Describe a shot.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generation needs a model backend. The UI, history, and " +
                        "import flow are local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewPanel() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preview placeholder",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(text = "BACKEND", accent = ZcutCoral)
            }
            TemplateArtBox(
                artKey = "night",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = "Hubungkan backend sendiri untuk menghasilkan aset. Kamu juga " +
                    "bisa import hasil dari galeri.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryGrid(
    entries: List<GenerationEntry>,
    thumbnails: MediaThumbnails,
    onOpen: (GenerationEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { entry ->
                    HistoryTile(
                        entry = entry,
                        thumbnails = thumbnails,
                        onClick = { onOpen(entry) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryTile(
    entry: GenerationEntry,
    thumbnails: MediaThumbnails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnail by rememberThumbnail(key = entry.asset.localPath) {
        thumbnails.load(entry.asset, 320)
    }

    ZcutCard(modifier = modifier, onClick = onClick) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = "Hasil: ${entry.prompt}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TemplateArtBox(
                    artKey = artKeyFor(entry),
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (entry.type == GenerationType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Hasil video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryDialog(
    entry: GenerationEntry,
    creating: Boolean,
    onDismiss: () -> Unit,
    onCreateDraft: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hasil generate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = entry.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${entry.type.label} · ${entry.style.label} · " +
                        "${entry.aspectRatio.label} · seed ${entry.seed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onDelete,
                    enabled = !creating
                ) {
                    Text(
                        text = "Hapus dari History",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateDraft, enabled = !creating) {
                Text(if (creating) "Membuat…" else "Buat draft")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

/** Deterministic art fallback so a tile without a thumbnail still looks distinct. */
private fun artKeyFor(entry: GenerationEntry): String {
    val keys = listOf("night", "pink", "blue", "gold", "green", "city", "portrait")
    val index = ((entry.seed + entry.prompt.hashCode().toLong()) and Long.MAX_VALUE) % keys.size
    return keys[index.toInt()]
}
