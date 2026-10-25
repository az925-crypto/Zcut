package com.zaaam.zcut.feature.tools

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.TtsScript
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.ttsViewModelFactory
import java.util.Locale

@Composable
fun TtsScreen(
    projectId: String,
    atMs: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: TtsViewModel = viewModel(
        key = "tts:$projectId:$atMs",
        factory = ttsViewModelFactory(container, projectId, atMs)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.stopPreview() }
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Text to speech",
                subtitle = "Android TTS · suara lokal",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = { TextButton(onClick = onBack) { Text("Selesai") } }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)
        when {
            state.notFound -> Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            state.project == null -> Box(modifier, contentAlignment = Alignment.Center) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }

            else -> TtsBody(
                state = state,
                onScript = viewModel::setScript,
                onVoice = viewModel::setVoice,
                onRate = viewModel::setRate,
                onPitch = viewModel::setPitch,
                onPreview = viewModel::preview,
                onGenerate = viewModel::generate,
                onDismissNotice = viewModel::clearNotice
            )
        }
    }
}

@Composable
private fun TtsBody(
    state: TtsUiState,
    onScript: (String) -> Unit,
    onVoice: (String) -> Unit,
    onRate: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onPreview: () -> Unit,
    onGenerate: () -> Unit,
    onDismissNotice: () -> Unit
) {
    var rateDraft by remember(state.rate) { mutableFloatStateOf(state.rate) }
    var pitchDraft by remember(state.pitch) { mutableFloatStateOf(state.pitch) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        val notice = state.notice
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

        ToolSection(title = "Skrip") {
            OutlinedTextField(
                value = state.script,
                onValueChange = onScript,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teks yang diucapkan") },
                minLines = 2,
                maxLines = 5,
                shape = MaterialTheme.shapes.small
            )
            ToolValueRow(
                "Perkiraan",
                "${state.script.length} / ${TtsScript.MAX_LENGTH} · " +
                    TimeFormat.clockMillis(TtsScript.estimateDurationMs(state.script))
            )
        }

        ToolSection(title = "Suara") {
            if (state.loadingVoices) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (state.voices.isEmpty()) {
                ToolHint("Mesin TTS perangkat tidak menyediakan daftar suara.")
            } else {
                ToolChipRow {
                    state.voices.forEach { voice ->
                        ToolChip(
                            label = voice.label,
                            selected = voice.id == state.selectedVoice?.id,
                            onClick = { onVoice(voice.id) }
                        )
                    }
                }
                ToolValueRow("Terpilih", state.selectedVoice?.label ?: "Bawaan perangkat")
            }
            ZcutSecondaryButton(
                text = "Pratinjau suara",
                onClick = onPreview,
                enabled = state.script.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        ToolSection(title = "Nada bicara") {
            ToolSlider(
                label = "Rate",
                value = rateDraft,
                valueRange = 0.5f..2f,
                onValueChange = { rateDraft = it },
                onValueChangeFinished = { onRate(rateDraft) },
                valueLabel = { String.format(Locale.US, "%.2f×", it) }
            )
            ToolSlider(
                label = "Pitch",
                value = pitchDraft,
                valueRange = 0.5f..2f,
                onValueChange = { pitchDraft = it },
                onValueChangeFinished = { onPitch(pitchDraft) },
                valueLabel = { String.format(Locale.US, "%.2f×", it) }
            )
        }

        ZcutPrimaryButton(
            text = if (state.isBusy) "Membuat suara…" else "Buat & sisipkan ke timeline",
            onClick = onGenerate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.generated) {
            ToolSection(title = "Hasil") {
                ToolValueRow("Status", "Siap · sudah masuk timeline")
                ToolHint("Klip audio ditambahkan pada posisi playhead saat panel ini dibuka.")
            }
        }

        ToolSection(title = "Catatan jujur") {
            ToolHint(
                "Suara dibuat oleh mesin TextToSpeech perangkat dan di-cache lokal; " +
                    "tidak ada permintaan ke server."
            )
            ToolHint("Voice clone suara tertentu BUTUH BACKEND; TTS sistem tetap gratis.")
        }

        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}