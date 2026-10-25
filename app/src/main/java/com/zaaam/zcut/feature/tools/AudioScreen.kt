package com.zaaam.zcut.feature.tools

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.AudioEditor
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.audioViewModelFactory
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AudioScreen(
    projectId: String,
    clipId: String?,
    atMs: Long,
    onBack: () -> Unit,
    onAddAudio: () -> Unit,
    onOpenTts: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: AudioViewModel = viewModel(
        key = "audio:$projectId:${clipId.orEmpty()}",
        factory = audioViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Audio",
                subtitle = "Music + voiceover · mix lokal",
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

            state.notFound || state.project == null -> Box(
                modifier,
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Draft tidak ditemukan",
                    message = "Draft ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali",
                    onAction = onBack
                )
            }

            else -> AudioBody(
                project = state.project!!,
                clips = state.clips,
                selected = state.selectedClip,
                busy = state.busy,
                notice = state.notice,
                atMs = atMs,
                onSelect = viewModel::selectClip,
                onVolume = viewModel::setVolume,
                onFades = viewModel::setFades,
                onMute = viewModel::setMuted,
                onLoop = viewModel::setLooping,
                onPitch = viewModel::setPitch,
                onDelete = viewModel::deleteSelected,
                onNormalize = viewModel::normalizeLoudness,
                onDetectBeats = viewModel::detectBeats,
                onSnap = viewModel::setSnapToBeat,
                onInsertRecording = viewModel::insertRecording,
                onAddAudio = onAddAudio,
                onOpenTts = onOpenTts,
                onDismissNotice = viewModel::clearNotice
            )
        }
    }
}

@Composable
private fun AudioBody(
    project: Project,
    clips: List<Clip>,
    selected: Clip?,
    busy: Boolean,
    notice: String?,
    atMs: Long,
    onSelect: (String) -> Unit,
    onVolume: (Float) -> Unit,
    onFades: (Long, Long) -> Unit,
    onMute: (Boolean) -> Unit,
    onLoop: (Boolean) -> Unit,
    onPitch: (Float) -> Unit,
    onDelete: () -> Unit,
    onNormalize: () -> Unit,
    onDetectBeats: () -> Unit,
    onSnap: (Boolean) -> Unit,
    onInsertRecording: (java.io.File, Long, Long) -> Unit,
    onAddAudio: () -> Unit,
    onOpenTts: () -> Unit,
    onDismissNotice: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val recorder = remember { container.newVoiceoverRecorder() }
    var recording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var localNotice by remember { mutableStateOf<String?>(null) }

    val startRecording: () -> Unit = {
        if (recorder.start() == null) {
            localNotice = "Tidak bisa memulai rekaman di perangkat ini."
        } else {
            recording = true
            elapsedMs = 0L
        }
    }
    val stopRecording: () -> Unit = {
        val result = recorder.stop()
        recording = false
        if (result == null) {
            localNotice = "Rekaman terlalu pendek untuk disimpan."
        } else {
            onInsertRecording(result.file, result.durationMs, atMs)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else localNotice = "Izin mikrofon ditolak."
    }

    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(200)
            elapsedMs += 200
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        val visibleNotice = localNotice ?: notice
        if (visibleNotice != null) {
            ToolSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = visibleNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            localNotice = null
                            onDismissNotice()
                        }
                    ) { Text("Tutup") }
                }
            }
        }

        if (clips.isEmpty()) {
            ToolSection(title = "Klip audio") {
                Text(
                    text = "Belum ada audio di timeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ToolHint("Impor dari perangkat atau rekam voiceover. Tidak ada unggahan.")
            }
        } else {
            ToolSection(title = "Klip audio") {
                ToolChipRow {
                    clips.forEachIndexed { index, clip ->
                        ToolChip(
                            label = clipLabel(project, clip, index),
                            selected = clip.id == selected?.id,
                            onClick = { onSelect(clip.id) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
        ) {
            ZcutSecondaryButton(
                text = "Tambah audio",
                onClick = onAddAudio,
                modifier = Modifier.weight(1f)
            )
            ZcutSecondaryButton(
                text = "Text to speech",
                onClick = onOpenTts,
                modifier = Modifier.weight(1f)
            )
        }

        if (selected != null) {
            AudioClipSection(
                clip = selected,
                project = project,
                busy = busy,
                onVolume = onVolume,
                onFades = onFades,
                onMute = onMute,
                onLoop = onLoop,
                onPitch = onPitch,
                onNormalize = onNormalize,
                onDelete = onDelete
            )
        }

        ToolSection(title = "Beat sync") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snap ke beat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = project.settings.snapToBeat,
                    onCheckedChange = onSnap,
                    enabled = selected != null && !busy
                )
            }
            ToolValueRow("Marker", "${project.settings.beatGridMs.size} beat")
            ZcutSecondaryButton(
                text = "Deteksi beat dari klip",
                onClick = onDetectBeats,
                enabled = selected != null && !busy,
                modifier = Modifier.fillMaxWidth()
            )
            ToolHint("Beat dihitung lokal dari waveform klip terpilih. Tidak ada audio yang diunggah.")
        }

        ToolSection(title = "Voiceover") {
            Text(
                text = if (recording) {
                    "Merekam… ${TimeFormat.clockMillis(elapsedMs)}"
                } else {
                    "Rekam narasi dari mikrofon perangkat."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            ZcutPrimaryButton(
                text = if (recording) "Stop rekaman" else "Rekam voiceover",
                onClick = {
                    if (recording) {
                        stopRecording()
                    } else if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ToolHint("Rekaman disimpan di workspace privat aplikasi, di posisi playhead.")
        }

        ToolSection(title = "Catatan jujur") {
            ToolHint(
                "Volume, fade, dan mute tersimpan di draft dan diterapkan saat export " +
                    "(M13). Preview Media3 masih memutar audio asli."
            )
            ToolHint("Pustaka musik bawaan belum ada; impor lagu dari perangkat.")
            ToolHint("Pemisahan vokal dan dubbing BUTUH BACKEND. Denoise menyusul.")
        }

        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

@Composable
private fun AudioClipSection(
    clip: Clip,
    project: Project,
    busy: Boolean,
    onVolume: (Float) -> Unit,
    onFades: (Long, Long) -> Unit,
    onMute: (Boolean) -> Unit,
    onLoop: (Boolean) -> Unit,
    onPitch: (Float) -> Unit,
    onNormalize: () -> Unit,
    onDelete: () -> Unit
) {
    var volumeDraft by remember(clip.id, clip.audio.volume) {
        mutableFloatStateOf(clip.audio.volume)
    }
    var fadeInDraft by remember(clip.id, clip.audio.fadeInMs) {
        mutableLongStateOf(clip.audio.fadeInMs)
    }
    var fadeOutDraft by remember(clip.id, clip.audio.fadeOutMs) {
        mutableLongStateOf(clip.audio.fadeOutMs)
    }
    var pitchDraft by remember(clip.id, clip.audio.pitch) {
        mutableFloatStateOf(clip.audio.pitch)
    }
    val maxFade = minOf(3_000L, clip.durationMs).coerceAtLeast(1L).toFloat()

    ToolSection(title = "Klip terpilih") {
        selectedClipName(project, clip)?.let { ToolValueRow("Nama", it) }
        ToolValueRow("Mulai", TimeFormat.clockMillis(clip.startMs))
        ToolValueRow("Durasi", TimeFormat.clockMillis(clip.durationMs))

        ToolSlider(
            label = "Volume",
            value = volumeDraft.coerceIn(0f, AudioEditor.MAX_VOLUME),
            valueRange = 0f..AudioEditor.MAX_VOLUME,
            onValueChange = { volumeDraft = it },
            onValueChangeFinished = { onVolume(volumeDraft) },
            valueLabel = { "${(it * 100).roundToInt()}%" }
        )
        ToolSlider(
            label = "Fade in",
            value = fadeInDraft.coerceIn(0L, maxFade.toLong()).toFloat(),
            valueRange = 0f..maxFade,
            onValueChange = { fadeInDraft = it.toLong() },
            onValueChangeFinished = { onFades(fadeInDraft, fadeOutDraft) },
            valueLabel = { "${it.roundToInt()} ms" }
        )
        ToolSlider(
            label = "Fade out",
            value = fadeOutDraft.coerceIn(0L, maxFade.toLong()).toFloat(),
            valueRange = 0f..maxFade,
            onValueChange = { fadeOutDraft = it.toLong() },
            onValueChangeFinished = { onFades(fadeInDraft, fadeOutDraft) },
            valueLabel = { "${it.roundToInt()} ms" }
        )
        ToolSlider(
            label = "Pitch (voice change)",
            value = pitchDraft.coerceIn(AudioEditor.MIN_PITCH, AudioEditor.MAX_PITCH),
            valueRange = AudioEditor.MIN_PITCH..AudioEditor.MAX_PITCH,
            onValueChange = { pitchDraft = it },
            onValueChangeFinished = { onPitch(pitchDraft) },
            valueLabel = { String.format(java.util.Locale.US, "%.2f×", it) }
        )

        ToolChipRow {
            ToolChip(
                label = "Mute",
                selected = clip.audio.muted,
                onClick = { onMute(!clip.audio.muted) }
            )
            ToolChip(
                label = "Loop",
                selected = clip.audio.looping,
                onClick = { onLoop(!clip.audio.looping) }
            )
        }

        ZcutSecondaryButton(
            text = "Normalisasi volume",
            onClick = onNormalize,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        ZcutSecondaryButton(
            text = "Hapus klip",
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        )
        ToolHint("Normalisasi mengukur level RMS lokal lalu mendekatkannya ke ≈ -16 dBFS.")
    }
}

private fun clipLabel(project: Project, clip: Clip, index: Int): String =
    selectedClipName(project, clip) ?: "Audio ${index + 1}"

private fun selectedClipName(project: Project, clip: Clip): String? {
    val assetId = clip.assetId ?: return null
    return project.assets
        .firstOrNull { it.id == assetId }
        ?.displayName
        ?.substringBeforeLast('.')
        ?.takeIf { it.isNotBlank() }
}