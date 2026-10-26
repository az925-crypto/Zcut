package com.zaaam.zcut.feature.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutChip
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.mediaPickerViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutOnPrimary
import com.zaaam.zcut.ui.theme.ZcutSpacing

@Composable
fun MediaPickerScreen(
    projectId: String?,
    templateId: String? = null,
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: MediaPickerViewModel = viewModel(
        key = "media-picker:${projectId ?: "new"}:${templateId.orEmpty()}",
        factory = mediaPickerViewModelFactory(container, projectId, templateId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val visualPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_ITEMS)
    ) { uris -> viewModel.addPicked(uris.map { it.toString() }) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> viewModel.addPicked(uris.map { it.toString() }) }

    val pickGallery: () -> Unit = {
        if (state.filter == MediaFilter.AUDIO) {
            documentPicker.launch(arrayOf(MediaFilter.AUDIO.mimeFilter))
        } else {
            val mediaType = if (state.filter == MediaFilter.VIDEOS) {
                ActivityResultContracts.PickVisualMedia.VideoOnly
            } else {
                ActivityResultContracts.PickVisualMedia.ImageOnly
            }
            visualPicker.launch(PickVisualMediaRequest(mediaType))
        }
    }

    MediaPickerContent(
        state = state,
        thumbnails = container.mediaThumbnails,
        canStartEmpty = projectId == null && templateId == null,
        onBack = onBack,
        onFilterChange = viewModel::setFilter,
        onPickGallery = pickGallery,
        onBrowseFiles = { documentPicker.launch(arrayOf(state.filter.mimeFilter)) },
        onRemove = viewModel::remove,
        onClear = viewModel::clear,
        onImport = { viewModel.import(onOpenEditor) },
        onStartEmpty = { viewModel.startEmpty(onOpenEditor) },
        onDismissNotice = viewModel::dismissNotice,
        onOpenEditor = onOpenEditor
    )
}

@Composable
private fun MediaPickerContent(
    state: MediaPickerUiState,
    thumbnails: MediaThumbnails,
    canStartEmpty: Boolean,
    onBack: () -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onPickGallery: () -> Unit,
    onBrowseFiles: () -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onImport: () -> Unit,
    onStartEmpty: () -> Unit,
    onDismissNotice: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Tambah media",
                subtitle = state.templateName?.let { "untuk template $it" }
                    ?: state.projectName?.let { "untuk $it" }
                    ?: "${state.selected.size} dipilih · workspace lokal",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    if (state.selected.isNotEmpty() && !state.isImporting) {
                        TextButton(onClick = onClear) { Text("Bersihkan") }
                    }
                }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            if (state.isImporting) {
                val progress = state.progress
                LinearProgressIndicator(
                    progress = {
                        if (progress == null || progress.total == 0) {
                            0f
                        } else {
                            progress.done.toFloat() / progress.total.toFloat()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                    MediaFilter.entries
                        // Template slots only take visual media, so audio is not
                        // offered there and can never produce a dead draft.
                        .filter { state.templateName == null || it != MediaFilter.AUDIO }
                        .forEach { filter ->
                            ZcutChip(
                                label = filter.label,
                                selected = state.filter == filter,
                                onClick = { onFilterChange(filter) }
                            )
                        }
                }

                val templateName = state.templateName
                if (templateName != null) {
                    Text(
                        text = "Template $templateName · butuh " +
                            "${state.requiredSlots ?: 0} media, satu per slot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                    ZcutPrimaryButton(
                        text = if (state.filter == MediaFilter.AUDIO) {
                            "Pilih audio"
                        } else {
                            "Pilih dari galeri"
                        },
                        onClick = onPickGallery,
                        enabled = !state.isImporting,
                        modifier = Modifier.weight(1f)
                    )
                    ZcutSecondaryButton(
                        text = "Telusuri file",
                        onClick = onBrowseFiles,
                        enabled = !state.isImporting,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Tidak ada izin galeri penuh. Media disalin ke workspace privat aplikasi, tanpa upload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.selected.isNotEmpty()) {
                    Text(
                        text = "${state.selected.size} dipilih · ketuk item untuk menghapus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.selected.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "Belum ada media dipilih",
                        message = "Pilih foto, video, atau audio dari perangkat. Semuanya diproses lokal."
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(ZcutSpacing.screen),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    itemsIndexed(state.selected, key = { _, uri -> uri }) { index, uri ->
                        SelectedTile(
                            index = index,
                            uri = uri,
                            thumbnails = thumbnails,
                            onRemove = { if (!state.isImporting) onRemove(uri) }
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
                ) {
                    ZcutPrimaryButton(
                        text = when {
                            state.templateName != null -> "Isi ${state.selected.size} slot"
                            state.selected.isEmpty() -> "Tambahkan media ke timeline"
                            else -> "Tambahkan ${state.selected.size} item ke timeline"
                        },
                        onClick = onImport,
                        enabled = state.selected.isNotEmpty() && !state.isImporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (canStartEmpty && state.selected.isEmpty() && !state.isImporting) {
                        TextButton(
                            onClick = onStartEmpty,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mulai dengan timeline kosong")
                        }
                    }
                }
            }
        }

        state.notice?.let { message ->
            val target = state.pendingProjectId
            AlertDialog(
                onDismissRequest = onDismissNotice,
                title = { Text(if (target != null) "Impor selesai" else "Impor media") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismissNotice()
                            if (target != null) onOpenEditor(target)
                        }
                    ) {
                        Text(if (target != null) "Buka editor" else "Tutup")
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectedTile(
    index: Int,
    uri: String,
    thumbnails: MediaThumbnails,
    onRemove: () -> Unit
) {
    val thumbnail by rememberThumbnail(key = uri) { thumbnails.loadUri(uri, 320) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onRemove)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = "Media terpilih ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = ZcutLime,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = ZcutOnPrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

private const val MAX_GALLERY_ITEMS = 10
