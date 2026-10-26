package com.zaaam.zcut.feature.tools

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.MusicCatalog
import com.zaaam.zcut.core.domain.MusicGenerator
import com.zaaam.zcut.core.domain.MusicMood
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.aiMusicViewModelFactory
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.StatusBadge
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * AI Music (F18). The approved mockup's copy is kept, but the engine line stays
 * honest: what runs here is a procedural synthesiser, not a generative model.
 */
@Composable
fun AiMusicScreen(
    projectId: String,
    atMs: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AiMusicViewModel = viewModel(
        key = "ai-music:$projectId:$atMs",
        factory = aiMusicViewModelFactory(container, projectId, atMs)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "AI Music",
                subtitle = "Procedural local generator",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    StatusBadge(text = "NO CREDITS", accent = ZcutLime)
                    TextButton(onClick = onBack) { Text("Selesai") }
                }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        when {
            state.isLoading -> Box(modifier, contentAlignment = Alignment.Center) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            state.error != null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Gagal memuat draft",
                    message = state.error.orEmpty(),
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            state.project == null -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> AiMusicBody(
                prompt = state.prompt,
                mood = state.mood,
                durationMs = state.durationMs,
                bpm = state.style.bpm,
                keyLabel = state.style.keyLabel,
                busy = state.busy,
                result = state.result,
                notice = state.notice,
                onPrompt = viewModel::setPrompt,
                onMood = viewModel::setMood,
                onDuration = viewModel::setDurationMs,
                onGenerate = viewModel::generate,
                onInsert = { viewModel.insert { stored -> if (stored) onBack() } },
                onDismissNotice = viewModel::clearNotice,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AiMusicBody(
    prompt: String,
    mood: MusicMood,
    durationMs: Long,
    bpm: Int,
    keyLabel: String,
    busy: Boolean,
    result: AiMusicResult?,
    notice: String?,
    onPrompt: (String) -> Unit,
    onMood: (MusicMood) -> Unit,
    onDuration: (Long) -> Unit,
    onGenerate: () -> Unit,
    onInsert: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier
) {
    var durationDraft by remember(durationMs) { mutableFloatStateOf(durationMs / 1_000f) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        ToolSection {
            Text(
                text = "Set the mood.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Generate a local loop or bring your own track. No server, " +
                    "no license gate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPrompt,
            label = { Text("Music prompt") },
            minLines = 2,
            maxLines = 4,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )

        ToolSection(title = "Mood") {
            ToolChipRow {
                MusicCatalog.moods.forEach { option ->
                    ToolChip(
                        label = MusicCatalog.style(option).label,
                        selected = option == mood,
                        onClick = { onMood(option) },
                        enabled = !busy
                    )
                }
            }
        }

        ToolSection {
            ToolSlider(
                label = "Duration",
                value = durationDraft,
                valueRange = MIN_SECONDS..MAX_SECONDS,
                onValueChange = { durationDraft = it },
                onValueChangeFinished = { onDuration((durationDraft * 1_000f).roundToLong()) },
                valueLabel = { "${it.roundToLong()}s" }
            )
            ToolValueRow("Tempo", "$bpm BPM")
            ToolValueRow("Key", keyLabel)
            ToolValueRow("Engine", "Local procedural")
        }

        ZcutPrimaryButton(
            text = if (busy) "Membuat loop..." else "Generate music",
            onClick = onGenerate,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )

        if (notice != null) {
            ToolSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissNotice) { Text("Tutup") }
                }
            }
        }

        if (result != null) {
            ToolSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusBadge(text = "READY", accent = ZcutLime)
                }
                WaveformStrip(modifier = Modifier.fillMaxWidth())
                ToolValueRow("Durasi", "${result.durationMs / 1_000}s")
                ToolValueRow("Ukuran", "${result.sizeBytes / 1_024} KB")
                ZcutSecondaryButton(
                    text = "Insert to timeline",
                    onClick = onInsert,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ToolSection(title = "Catatan jujur") {
            ToolHint(
                "Model musik generatif penuh ditandai BUTUH BACKEND. Generator " +
                    "prosedural lokal menyediakan loop dasar."
            )
            ToolHint(
                "Prompt dan mood hanya menentukan benih angka, bukan model: loop " +
                    "yang sama selalu terdengar sama. Tidak ada audio yang diunggah."
            )
            ToolHint(
                "Loop masuk ke timeline sebagai klip audio biasa, dan tempo-nya " +
                    "dipakai sebagai grid snap beat. Video-to-music dan lyric video " +
                    "menyusul (P1/P2)."
            )
        }

        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

/**
 * Decorative bar strip next to the result, matching the mockup's waveform
 * ornament. It is not a measurement of the rendered file.
 */
@Composable
private fun WaveformStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // ponytail: fixed pseudo-random bar heights, no file read; the strip is
        // ornament, so a real amplitude scan would buy nothing.
        val heights = remember {
            List(WAVE_BARS) { index ->
                val wave = abs(kotlin.math.sin(index * 1.7)) + abs(kotlin.math.cos(index * 0.9))
                (0.25f + wave.coerceIn(0f, 2f) / 4f)
            }
        }
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((22 * fraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZcutLime.copy(alpha = 0.75f))
            )
        }
    }
}

private const val WAVE_BARS = 40
private val MIN_SECONDS = MusicGenerator.MIN_DURATION_MS / 1_000f
private val MAX_SECONDS = MusicGenerator.MAX_DURATION_MS / 1_000f