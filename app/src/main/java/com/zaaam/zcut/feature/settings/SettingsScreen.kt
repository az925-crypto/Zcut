package com.zaaam.zcut.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.BuildConfig
import com.zaaam.zcut.core.data.AppLanguage
import com.zaaam.zcut.core.data.ThemeMode
import com.zaaam.zcut.core.domain.ExportDefaults
import com.zaaam.zcut.core.i18n.LocalZcutLanguage
import com.zaaam.zcut.core.i18n.ZcutText
import com.zaaam.zcut.core.i18n.zt
import com.zaaam.zcut.core.i18n.ztf
import com.zaaam.zcut.core.model.Resolution
import com.zaaam.zcut.core.util.FileSizeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.ListRow
import com.zaaam.zcut.ui.components.SectionHeader
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.zcutViewModelFactory

private enum class SettingsDialog { NONE, THEME, LANGUAGE, EXPORT, AUDIO }

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = zcutViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshStorage() }

    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeChange = viewModel::setThemeMode,
        onLanguageChange = viewModel::setLanguage,
        onResolutionChange = viewModel::setResolution,
        onFrameRateChange = viewModel::setFrameRate,
        onAudioChange = viewModel::setAudioSampleRate,
        onClearCache = viewModel::clearCache
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onResolutionChange: (Resolution) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onAudioChange: (Int) -> Unit,
    onClearCache: () -> Unit
) {
    var dialog by remember { mutableStateOf(SettingsDialog.NONE) }
    val language = LocalZcutLanguage.current

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Pengaturan",
                subtitle = "Preferensi lokal",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = zt(ZcutText.NoAds, language), accent = ZcutLime) }
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(
                start = ZcutSpacing.screen,
                end = ZcutSpacing.screen,
                top = ZcutSpacing.xs,
                bottom = ZcutSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
        ) {
            if (state.error != null) {
                item {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { SectionHeader(title = zt(ZcutText.Appearance, language)) }
            item {
                ZcutCard {
                    Column {
                        ListRow(
                            title = zt(ZcutText.Theme, language),
                            subtitle = themeLabel(state.themeMode, language),
                            trailing = { StatusBadge(text = themeBadge(state.themeMode, language)) },
                            onClick = { dialog = SettingsDialog.THEME }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.Language, language),
                            subtitle = zt(ZcutText.LanguageSubtitle, language),
                            trailing = { StatusBadge(text = state.language.tag.uppercase()) },
                            onClick = { dialog = SettingsDialog.LANGUAGE }
                        )
                    }
                }
            }

            item { SectionHeader(title = zt(ZcutText.Storage, language)) }
            item {
                ZcutCard {
                    Column {
                        ListRow(
                            title = zt(ZcutText.Workspace, language),
                            subtitle = "${FileSizeFormat.format(state.storageBytes)} · " +
                                ztf(ZcutText.DraftCount, language, state.draftCount),
                            trailing = {
                                ZcutSecondaryButton(
                                    text = zt(ZcutText.Clean, language),
                                    onClick = onClearCache,
                                    enabled = !state.isClearingCache
                                )
                            }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.Backup, language),
                            subtitle = zt(ZcutText.BackupSubtitle, language),
                            trailing = { StatusBadge(text = zt(ZcutText.Local, language)) }
                        )
                    }
                }
            }

            item { SectionHeader(title = zt(ZcutText.ExportDefaults, language)) }
            item {
                ZcutCard {
                    Column {
                        ListRow(
                            title = zt(ZcutText.Resolution, language),
                            subtitle = ExportDefaults.summary(state.resolution, state.frameRate),
                            onClick = { dialog = SettingsDialog.EXPORT }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.Audio, language),
                            subtitle = ExportDefaults.audioLabel(state.audioSampleRate),
                            onClick = { dialog = SettingsDialog.AUDIO }
                        )
                    }
                }
            }

            item { SectionHeader(title = zt(ZcutText.Privacy, language)) }
            item {
                ZcutCard {
                    Column {
                        ListRow(
                            title = zt(ZcutText.Telemetry, language),
                            subtitle = zt(ZcutText.TelemetrySubtitle, language),
                            trailing = {
                                Switch(
                                    checked = false,
                                    onCheckedChange = null,
                                    enabled = false
                                )
                            }
                        )
                        RowDivider()
                        ListRow(
                            title = zt(ZcutText.NetworkAccess, language),
                            subtitle = zt(ZcutText.NoNetworkSubtitle, language),
                            trailing = { StatusBadge(text = zt(ZcutText.None, language)) }
                        )
                    }
                }
            }

            item {
                ZcutCard {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = ZcutSpacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zt(ZcutText.Billing, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(text = zt(ZcutText.Removed, language), accent = ZcutLime)
                        }
                        Text(
                            text = zt(ZcutText.MonetizationNote, language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Zcut v${BuildConfig.VERSION_NAME} · com.zaaam.zcut",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = ZcutSpacing.sm)
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.NONE -> Unit

        SettingsDialog.THEME -> ChoiceDialog(
            title = zt(ZcutText.Theme, language),
            options = ThemeMode.entries,
            selected = state.themeMode,
            label = { themeLabel(it, language) },
            onSelect = onThemeChange,
            onDismiss = { dialog = SettingsDialog.NONE }
        )

        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = zt(ZcutText.Language, language),
            options = AppLanguage.entries,
            selected = state.language,
            label = { it.label },
            onSelect = onLanguageChange,
            onDismiss = { dialog = SettingsDialog.NONE }
        )

        SettingsDialog.EXPORT -> AlertDialog(
            onDismissRequest = { dialog = SettingsDialog.NONE },
            title = { Text(zt(ZcutText.ResolutionAndFrameRate, language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DialogGroupLabel(zt(ZcutText.Resolution, language))
                    Resolution.entries.forEach { resolution ->
                        ChoiceRow(
                            label = ExportDefaults.resolutionLabel(resolution),
                            selected = resolution == state.resolution,
                            onClick = { onResolutionChange(resolution) }
                        )
                    }
                    Spacer(Modifier.height(ZcutSpacing.sm))
                    DialogGroupLabel(zt(ZcutText.FrameRate, language))
                    listOf(24, 30, 60).forEach { frameRate ->
                        ChoiceRow(
                            label = "$frameRate fps",
                            selected = frameRate == state.frameRate,
                            onClick = { onFrameRateChange(frameRate) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialog = SettingsDialog.NONE }) { Text(zt(ZcutText.Close, language)) }
            }
        )

        SettingsDialog.AUDIO -> ChoiceDialog(
            title = zt(ZcutText.Audio, language),
            options = AppPreferencesRates.ALL,
            selected = state.audioSampleRate,
            label = ExportDefaults::audioLabel,
            onSelect = onAudioChange,
            onDismiss = { dialog = SettingsDialog.NONE }
        )
    }
}

/** Sample rates offered in Settings; must match AppPreferences validation. */
private object AppPreferencesRates {
    val ALL = listOf(44_100, 48_000)
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    ChoiceRow(
                        label = label(option),
                        selected = option == selected,
                        onClick = { onSelect(option) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(zt(ZcutText.Close, LocalZcutLanguage.current))
            }
        }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DialogGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = ZcutSpacing.xs)
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        thickness = 1.dp
    )
}

private fun themeLabel(mode: ThemeMode, language: AppLanguage): String = when (mode) {
    ThemeMode.DARK -> zt(ZcutText.ThemeDark, language)
    ThemeMode.LIGHT -> zt(ZcutText.ThemeLight, language)
    ThemeMode.SYSTEM -> zt(ZcutText.ThemeSystem, language)
}

private fun themeBadge(mode: ThemeMode, language: AppLanguage): String = when (mode) {
    ThemeMode.DARK -> zt(ZcutText.ThemeDarkBadge, language)
    ThemeMode.LIGHT -> zt(ZcutText.ThemeLightBadge, language)
    ThemeMode.SYSTEM -> zt(ZcutText.ThemeSystemBadge, language)
}
