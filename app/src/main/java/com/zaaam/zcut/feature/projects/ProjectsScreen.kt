package com.zaaam.zcut.feature.projects

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.data.DraftBackup
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.DraftSummary
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ProjectRowCard
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.components.ZcutChip
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.navigation.ZcutRoutes
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.zcutViewModelFactory

@Composable
fun ProjectsScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ProjectsViewModel = viewModel(factory = zcutViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingBackup by remember { mutableStateOf<DraftSummary?>(null) }
    var pendingRename by remember { mutableStateOf<DraftSummary?>(null) }

    val createBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DraftBackup.MIME_TYPE)
    ) { uri ->
        val draft = pendingBackup
        pendingBackup = null
        if (uri != null && draft != null) {
            viewModel.backup(draft.id) { context.contentResolver.openOutputStream(uri) }
        }
    }
    val openBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup { context.contentResolver.openInputStream(uri) }
        }
    }

    ProjectsContent(
        state = state,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        onQueryChange = viewModel::onQueryChange,
        onAspectChange = viewModel::onAspectChange,
        onDuplicate = viewModel::duplicate,
        onRename = { draft -> pendingRename = draft },
        onTrash = viewModel::trash,
        onRestore = viewModel::restore,
        onDeleteForever = viewModel::delete,
        onEmptyTrash = viewModel::emptyTrash,
        onToggleTrash = viewModel::toggleTrash,
        onBackup = { draft ->
            pendingBackup = draft
            createBackup.launch(DraftBackup.fileNameFor(draft.name))
        },
        onImport = {
            openBackup.launch(arrayOf(DraftBackup.MIME_TYPE, "application/octet-stream"))
        },
        onClearNotice = viewModel::clearNotice,
        onCreate = { onNavigate(ZcutRoutes.mediaPicker()) }
    )

    pendingRename?.let { draft ->
        RenameDialog(
            draft = draft,
            onDismiss = { pendingRename = null },
            onConfirm = { name ->
                pendingRename = null
                viewModel.rename(draft.id, name)
            }
        )
    }
}

@Composable
private fun ProjectsContent(
    state: ProjectsUiState,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onAspectChange: (AspectRatio?) -> Unit,
    onDuplicate: (String) -> Unit,
    onRename: (DraftSummary) -> Unit,
    onTrash: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onToggleTrash: () -> Unit,
    onBackup: (DraftSummary) -> Unit,
    onImport: () -> Unit,
    onClearNotice: () -> Unit,
    onCreate: () -> Unit
) {
    val nowMs = System.currentTimeMillis()
    var menuForDraftId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<DraftSummary?>(null) }
    var pendingTrash by remember { mutableStateOf<DraftSummary?>(null) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = if (state.trashMode) "Sampah" else "Draft",
                subtitle = if (state.trashMode) {
                    "${state.totalCount} draft di sampah"
                } else {
                    "${state.totalCount} project tersimpan di perangkat"
                },
                actions = {
                    TextButton(onClick = onImport) { Text("Impor") }
                    if (state.trashMode) {
                        TextButton(onClick = onEmptyTrash) { Text("Kosongkan") }
                    }
                }
            )
        },
        bottomBar = {
            ZcutBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        floatingActionButton = {
            if (!state.trashMode) {
                FloatingActionButton(
                    onClick = onCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Project baru"
                    )
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Column(
                modifier = Modifier.padding(
                    start = ZcutSpacing.screen,
                    end = ZcutSpacing.screen,
                    top = ZcutSpacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = { Text("Cari draft") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
                    items(ZcutAspectFilters, key = { it?.name ?: "all" }) { aspect ->
                        ZcutChip(
                            label = aspect?.label ?: "Semua",
                            selected = !state.trashMode && state.aspectRatio == aspect,
                            onClick = { onAspectChange(aspect) }
                        )
                    }
                    item(key = "trash") {
                        ZcutChip(
                            label = if (state.trashCount > 0) {
                                "Sampah (${state.trashCount})"
                            } else {
                                "Sampah"
                            },
                            selected = state.trashMode,
                            onClick = onToggleTrash
                        )
                    }
                }
            }

            if (state.error != null) {
                Banner(text = state.error, error = true)
            }
            if (state.notice != null) {
                Banner(text = state.notice.orEmpty(), onDismiss = onClearNotice)
            }

            if (state.isEmpty && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = when {
                            state.trashMode -> "Sampah kosong"
                            state.hasNoDraftsAtAll -> "Belum ada draft"
                            else -> "Tidak ada hasil"
                        },
                        message = when {
                            state.trashMode ->
                                "Draft yang dipindah ke sampah muncul di sini dan bisa dipulihkan."
                            state.hasNoDraftsAtAll ->
                                "Semua draft tersimpan lokal di perangkat ini. Mulai project baru untuk menambah."
                            else -> "Coba ubah kata kunci atau filter aspek."
                        },
                        actionLabel = if (!state.trashMode && state.hasNoDraftsAtAll) {
                            "Project baru"
                        } else {
                            null
                        },
                        onAction = if (!state.trashMode && state.hasNoDraftsAtAll) onCreate else null
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = ZcutSpacing.screen,
                        end = ZcutSpacing.screen,
                        top = ZcutSpacing.sm,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    items(state.drafts, key = DraftSummary::id) { draft ->
                        Box {
                            ProjectRowCard(
                                draft = draft,
                                nowMs = nowMs,
                                onClick = {
                                    if (state.trashMode) {
                                        onRestore(draft.id)
                                    } else {
                                        onNavigate(ZcutRoutes.editor(draft.id))
                                    }
                                },
                                onMore = { menuForDraftId = draft.id }
                            )
                            DropdownMenu(
                                expanded = menuForDraftId == draft.id,
                                onDismissRequest = { menuForDraftId = null }
                            ) {
                                if (state.trashMode) {
                                    DropdownMenuItem(
                                        text = { Text("Pulihkan") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForDraftId = null
                                            onRestore(draft.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hapus permanen") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForDraftId = null
                                            pendingDelete = draft
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Ganti nama") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForDraftId = null
                                            onRename(draft)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplikat") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Add, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForDraftId = null
                                            onDuplicate(draft.id)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Backup .zip") },
                                        onClick = {
                                            menuForDraftId = null
                                            onBackup(draft)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pindah ke sampah") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            menuForDraftId = null
                                            pendingTrash = draft
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Moving to the trash is reversible; permanent deletion is not.
        pendingTrash?.let { draft ->
            AlertDialog(
                onDismissRequest = { pendingTrash = null },
                title = { Text("Pindah ke sampah?") },
                text = {
                    Text(
                        "\"${draft.name}\" akan dipindah ke sampah dan bisa dipulihkan " +
                            "dari tab Sampah."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingTrash = null
                            onTrash(draft.id)
                        }
                    ) {
                        Text("Pindahkan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingTrash = null }) { Text("Batal") }
                }
            )
        }

        pendingDelete?.let { draft ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Hapus permanen?") },
                text = { Text("\"${draft.name}\" akan dihapus permanen dari perangkat ini.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDelete = null
                            onDeleteForever(draft.id)
                        }
                    ) {
                        Text(
                            text = "Hapus",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
private fun RenameDialog(
    draft: DraftSummary,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(draft.id) { mutableStateOf(draft.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti nama draft") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                singleLine = true,
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun Banner(
    text: String,
    error: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        color = if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(start = ZcutSpacing.md, top = ZcutSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (error) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = ZcutSpacing.xs)
            )
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    }
}
