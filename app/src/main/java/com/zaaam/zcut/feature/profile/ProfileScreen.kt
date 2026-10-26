package com.zaaam.zcut.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.BuildConfig
import com.zaaam.zcut.core.i18n.LocalZcutLanguage
import com.zaaam.zcut.core.i18n.ZcutText
import com.zaaam.zcut.core.i18n.zt
import com.zaaam.zcut.core.i18n.ztf
import com.zaaam.zcut.core.util.FileSizeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ListRow
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.navigation.ZcutRoutes
import com.zaaam.zcut.ui.theme.ZcutCyan
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutOnPrimary
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.zcutViewModelFactory

@Composable
fun ProfileScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ProfileViewModel = viewModel(factory = zcutViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshStorage() }

    ProfileContent(
        state = state,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        onSetDisplayName = viewModel::setDisplayName
    )
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSetDisplayName: (String) -> Unit
) {
    val language = LocalZcutLanguage.current
    var editingName by remember { mutableStateOf(false) }
    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Profil",
                subtitle = "Profil lokal · tanpa akun",
                actions = {
                    ZcutIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = zt(ZcutText.EditProfile, language),
                        onClick = { editingName = true }
                    )
                }
            )
        },
        bottomBar = {
            ZcutBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(
                start = ZcutSpacing.screen,
                end = ZcutSpacing.screen,
                top = ZcutSpacing.sm,
                bottom = ZcutSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            item { ProfileHead(state.displayName) }
            item { UnlockBanner() }

            if (state.error != null) {
                item {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { StatsRow(state) }

            item {
                ZcutCard {
                    Column {
                        ListRow(
                            title = zt(ZcutText.TemplatesRow, language),
                            subtitle = zt(ZcutText.TemplatesRowSubtitle, language),
                            leadingIcon = Icons.Filled.Star,
                            onClick = { onNavigate(ZcutRoutes.Templates) }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.DraftsRow, language),
                            subtitle = zt(ZcutText.DraftsRowSubtitle, language),
                            leadingIcon = Icons.Filled.List,
                            onClick = { onNavigate(ZcutRoutes.Projects) }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.AiStudioRow, language),
                            subtitle = zt(ZcutText.AiStudioRowSubtitle, language),
                            leadingIcon = Icons.Filled.Build,
                            onClick = { onNavigate(ZcutRoutes.AiStudio) }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.SettingsRow, language),
                            subtitle = zt(ZcutText.SettingsRowSubtitle, language),
                            leadingIcon = Icons.Filled.Edit,
                            onClick = { onNavigate(ZcutRoutes.Settings) }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.About, language),
                            subtitle = ztf(
                                ZcutText.AboutSubtitle,
                                language,
                                BuildConfig.VERSION_NAME
                            ),
                            leadingIcon = Icons.Filled.Info,
                            onClick = { onNavigate(ZcutRoutes.Settings) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = zt(ZcutText.NoLogin, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (editingName) {
        var draftName by remember(state.displayName) { mutableStateOf(state.displayName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text(zt(ZcutText.EditProfile, language)) },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = {
                        draftName = it.take(com.zaaam.zcut.core.data.AppPreferences.MAX_DISPLAY_NAME)
                    },
                    singleLine = true,
                    label = { Text(zt(ZcutText.DisplayName, language)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetDisplayName(draftName)
                        editingName = false
                    }
                ) {
                    Text(zt(ZcutText.Save, language))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) {
                    Text(zt(ZcutText.Cancel, language))
                }
            }
        )
    }
}

@Composable
private fun ProfileHead(displayName: String) {
    val language = LocalZcutLanguage.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ZcutLime, ZcutCyan))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.trim().take(1).uppercase().ifEmpty { "Z" },
                style = MaterialTheme.typography.headlineSmall,
                color = ZcutOnPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = zt(ZcutText.OnDeviceOnly, language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusBadge(text = zt(ZcutText.Free, language))
    }
}

@Composable
private fun UnlockBanner() {
    val language = LocalZcutLanguage.current
    ZcutCard {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = ZcutLime,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = zt(ZcutText.AllFeatures, language),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = zt(ZcutText.NoSubscriptionWatermark, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsRow(state: ProfileUiState) {
    val language = LocalZcutLanguage.current
    Row(horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)) {
        StatCard(
            value = state.draftCount.toString(),
            label = zt(ZcutText.StatDrafts, language),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = state.exportCount.toString(),
            label = zt(ZcutText.StatExports, language),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = FileSizeFormat.format(state.storageBytes),
            label = zt(ZcutText.StatUsed, language),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    ZcutCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ZcutSpacing.sm, vertical = ZcutSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(ZcutSpacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        thickness = 1.dp
    )
}
