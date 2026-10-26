package com.zaaam.zcut.feature.ai

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.DigitalHumanAvatar
import com.zaaam.zcut.feature.templates.TemplateArtBox
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.digitalHumanViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutSpacing

/**
 * Digital Human (F25). Mirrors the approved mockup: preview hero with play,
 * avatar grid, script, the voice/lip-sync/background rows, the generate action
 * and the backend note. Lip sync and avatar render stay backend-only; script,
 * voice, background and subtitles are prepared locally.
 */
@Composable
fun DigitalHumanScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: DigitalHumanViewModel = viewModel(
        key = "digital-human",
        factory = digitalHumanViewModelFactory(container)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Digital Human",
                subtitle = "Avatar presenter · backend render",
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
        DigitalHumanBody(
            state = state,
            onAvatar = viewModel::setAvatar,
            onScript = viewModel::setScript,
            onVoice = viewModel::setVoice,
            onSubtitles = viewModel::setSubtitles,
            onPreview = viewModel::preview,
            onGenerate = { viewModel.generate { id -> id?.let(onOpenEditor) } },
            onOpenDraft = { id -> onOpenEditor(id) },
            onDismissNotice = viewModel::clearNotice,
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        )
    }
}

@Composable
private fun DigitalHumanBody(
    state: DigitalHumanUiState,
    onAvatar: (String) -> Unit,
    onScript: (String) -> Unit,
    onVoice: (String) -> Unit,
    onSubtitles: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onGenerate: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier
) {
    var pickingVoice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        PreviewHero(avatar = state.avatar, onPreview = onPreview)

        Text(
            text = "Avatar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.avatars.forEach { avatar ->
                AvatarTile(
                    avatar = avatar,
                    selected = avatar.id == state.selectedAvatarId,
                    onClick = { onAvatar(avatar.id) },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.8f)
                )
            }
        }

        ToolSection(title = "Script") {
            OutlinedTextField(
                value = state.script,
                onValueChange = onScript,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("What should the avatar say?") }
            )
        }

        ToolSection {
            DetailRow(
                label = "Voice",
                value = state.selectedVoice?.label ?: "TTS perangkat",
                onAction = { pickingVoice = true }
            )
            DetailRow(label = "Lip sync", value = "Backend", valueColor = ZcutCoral)
            DetailRow(label = "Background", value = state.avatar.background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = state.subtitles, onCheckedChange = onSubtitles)
            }
        }

        ZcutPrimaryButton(
            text = if (state.busy) "Menyiapkan…" else "Generate avatar video",
            onClick = onGenerate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.createdDraftId != null) {
            ZcutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ZcutSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    Text(
                        text = "Draft persiapan siap",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = (state.createdDraftName ?: "Draft") +
                            " berisi background, suara, dan subtitle (perkiraan).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ZcutSecondaryButton(
                        text = "Buka draft",
                        onClick = { onOpenDraft(state.createdDraftId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    text = "Render avatar dan lip sync membutuhkan backend/model. Script, " +
                        "TTS, subtitle, dan background lokal tetap bisa disiapkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BackendBadge(featureId = "F25")
            }
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

        ToolHint(
            "Suara dibuat oleh mesin TTS perangkat. Batas waktu subtitle dihitung " +
                "proporsional dari durasi suara, jadi itu perkiraan, bukan hasil " +
                "pengenalan ucapan."
        )

        Spacer(Modifier.height(ZcutSpacing.xl))
    }

    if (pickingVoice) {
        VoiceDialog(
            state = state,
            onDismiss = { pickingVoice = false },
            onSelect = { id ->
                pickingVoice = false
                onVoice(id)
            }
        )
    }
}

/** Label/value row behind the mockup's Voice / Lip sync / Background panel. */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor
            )
            if (onAction != null) {
                TextButton(onClick = onAction) { Text("Pilih") }
            }
        }
    }
}

@Composable
private fun PreviewHero(avatar: DigitalHumanAvatar, onPreview: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        TemplateArtBox(artKey = avatar.artKey, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = "${avatar.name} · ${avatar.background}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = "Preview only · render butuh backend",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
        ) {
            ZcutIconButton(
                icon = Icons.Filled.PlayArrow,
                contentDescription = "Putar pratinjau suara",
                onClick = onPreview,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AvatarTile(
    avatar: DigitalHumanAvatar,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TemplateArtBox(artKey = avatar.artKey, modifier = Modifier.fillMaxSize())
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun VoiceDialog(
    state: DigitalHumanUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih suara") },
        text = {
            if (state.voices.isEmpty()) {
                Text(
                    text = if (state.loadingVoices) {
                        "Memuat suara perangkat…"
                    } else {
                        "Perangkat ini tidak melaporkan suara TTS. Suara default akan " +
                            "dipakai."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.voices.forEach { voice ->
                        TextButton(
                            onClick = { onSelect(voice.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${voice.label} · ${voice.locale}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}