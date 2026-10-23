package com.zaaam.zcut.feature.projects

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    ProjectsContent(
        state = state,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        onQueryChange = viewModel::onQueryChange,
        onAspectChange = viewModel::onAspectChange,
        onDuplicate = viewModel::duplicate,
        onDelete = viewModel::delete,
        onCreate = { viewModel.createProject { onNavigate(ZcutRoutes.Editor) } }
    )
}

@Composable
private fun ProjectsContent(
    state: ProjectsUiState,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onAspectChange: (AspectRatio?) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: () -> Unit
) {
    val nowMs = System.currentTimeMillis()
    var menuForDraftId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<DraftSummary?>(null) }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Draft",
                subtitle = "${state.totalCount} project tersimpan di perangkat"
            )
        },
        bottomBar = {
            ZcutBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        floatingActionButton = {
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
                            selected = state.aspectRatio == aspect,
                            onClick = { onAspectChange(aspect) }
                        )
                    }
                }
            }

            if (state.error != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(ZcutSpacing.md)
                    )
                }
            }

            if (state.isEmpty && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (state.hasNoDraftsAtAll) "Belum ada draft" else "Tidak ada hasil",
                        message = if (state.hasNoDraftsAtAll) {
                            "Semua draft tersimpan lokal di perangkat ini. Mulai project baru untuk menambah."
                        } else {
                            "Coba ubah kata kunci atau filter aspek."
                        },
                        actionLabel = if (state.hasNoDraftsAtAll) "Project baru" else null,
                        onAction = if (state.hasNoDraftsAtAll) onCreate else null
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
                                onClick = { onNavigate(ZcutRoutes.Editor) },
                                onMore = { menuForDraftId = draft.id }
                            )
                            DropdownMenu(
                                expanded = menuForDraftId == draft.id,
                                onDismissRequest = { menuForDraftId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Duplikat") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuForDraftId = null
                                        onDuplicate(draft.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuForDraftId = null
                                        pendingDelete = draft
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Deleting a draft is permanent, so ask first.
        pendingDelete?.let { draft ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Hapus draft?") },
                text = { Text("\"${draft.name}\" akan dihapus permanen dari perangkat ini.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDelete = null
                            onDelete(draft.id)
                        }
                    ) {
                        Text(
                            text = "Hapus",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
