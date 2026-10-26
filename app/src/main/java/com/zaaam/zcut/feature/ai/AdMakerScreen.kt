package com.zaaam.zcut.feature.ai

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.AdCta
import com.zaaam.zcut.core.domain.AdScene
import com.zaaam.zcut.core.domain.AdScript
import com.zaaam.zcut.core.domain.MusicGenerator
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolValueRow
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.adMakerViewModelFactory
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutCard
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.util.Locale

/**
 * Ad Maker (F31). Mirrors the approved mockup: preview card, script box with
 * three scenes, product name, CTA chips, voiceover/music panel, and one
 * "Generate ad draft" action. Everything visible here is local; the smart-ad
 * and product-album panels are marked BUTUH BACKEND.
 */
@Composable
fun AdMakerScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AdMakerViewModel = viewModel(factory = adMakerViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Ad Maker",
                subtitle = "Script → first cut · local",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { StatusBadge(text = "LOCAL") }
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AdPreviewCard(state = state)

            ScriptBox(state = state, onScriptChange = viewModel::setScript)

            OutlinedTextField(
                value = state.product,
                onValueChange = viewModel::setProduct,
                label = { Text("Product") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "CTA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToolChipRow {
                AdCta.entries.forEach { cta ->
                    ToolChip(
                        label = cta.label,
                        selected = cta == state.cta,
                        onClick = { viewModel.setCta(cta) }
                    )
                }
            }

            ToolSection(title = "Voiceover") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voiceover lokal",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.voiceover,
                        onCheckedChange = viewModel::setVoiceover
                    )
                }
                ToolValueRow(
                    label = "Voice",
                    value = state.selectedVoice?.label ?: "Default perangkat"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music (prosedural)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.music,
                        onCheckedChange = viewModel::setMusic
                    )
                }
                ToolValueRow(
                    label = "Music bed",
                    value = "Cinematic · ${MusicGenerator.SAMPLE_RATE / 1_000} kHz"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
                ) {
                    ZcutSecondaryButton(
                        text = "Preview suara",
                        onClick = viewModel::preview,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    )
                    ZcutSecondaryButton(
                        text = "Stop",
                        onClick = viewModel::stopPreview,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ZcutPrimaryButton(
                text = if (state.busy) "Menyiapkan…" else "Generate ad draft",
                onClick = { viewModel.generate(onOpenEditor) },
                enabled = state.ready,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.createdDraftName != null) {
                ZcutCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ZcutSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Draft siap",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.createdDraftName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ZcutSecondaryButton(
                            text = "Buka di editor",
                            onClick = {
                                state.createdDraftId?.let(onOpenEditor)
                            },
                            enabled = state.createdDraftId != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.notice != null) {
                ToolSection {
                    Text(
                        text = state.notice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ToolHint(
                "Skrip dipecah jadi scene, lalu voiceover TTS perangkat, caption, dan " +
                    "overlay CTA ditulis ke satu draft nyata. Smart ad generatif, product " +
                    "album AI, dan laporan/pixel iklan BUTUH BACKEND. Satu track audio per " +
                    "draft: voiceover dan musik tidak bisa aktif bersamaan."
            )

            ToolSection {
                Text(
                    text = "Smart ad, product album AI, dan pixel/report belum tersedia " +
                        "tanpa backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BackendBadge(featureId = "F31")
            }

            Spacer(Modifier.height(ZcutSpacing.xl))
        }
    }
}

@Composable
private fun AdPreviewCard(state: AdMakerUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B1020), Color(0xFF1B2B4B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = ZcutSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "ZCUT AD · LOCAL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = state.product.ifBlank { "Product ad" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(ZcutSpacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Product ad · ${formatSeconds(state.totalDurationMs)}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = "9:16 · local template · TTS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
        StatusBadge(
            text = state.cta.label,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ZcutSpacing.md)
        )
    }
}

@Composable
private fun ScriptBox(
    state: AdMakerUiState,
    onScriptChange: (String) -> Unit
) {
    val scenes = state.scenes
    ToolSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Script",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(text = "${scenes.size} SCENES")
        }
        scenes.take(AdScript.MAX_LINES).forEach { scene ->
            ScriptLine(scene)
        }
        if (scenes.isEmpty()) {
            Text(
                text = "Belum ada baris. Tulis satu kalimat per baris.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = state.script,
            onValueChange = onScriptChange,
            label = { Text("Satu baris = satu scene") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScriptLine(scene: AdScene) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = (scene.index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scene.text,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${scene.role.label} · ${formatRange(scene.startMs, scene.endMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSeconds(durationMs: Long): String =
    String.format(Locale.US, "%.1fs", durationMs.coerceAtLeast(0L) / 1000f)

private fun formatRange(startMs: Long, endMs: Long): String {
    val start = (startMs.coerceAtLeast(0L) / 1_000f)
    val end = (endMs.coerceAtLeast(0L) / 1_000f)
    return String.format(Locale.US, "%.0f–%.0fs", start, end)
}
