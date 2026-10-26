package com.zaaam.zcut.feature.tools

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.zaaam.zcut.core.domain.CaptionEditor
import com.zaaam.zcut.core.domain.CaptionSegmenter
import com.zaaam.zcut.core.model.CaptionCue
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import com.zaaam.zcut.core.model.coverAsset
import com.zaaam.zcut.core.util.SrtFormat
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.export.ExportDelivery
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.captionViewModelFactory
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutText
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaptionScreen(
    projectId: String,
    atMs: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: CaptionViewModel = viewModel(
        key = "caption:$projectId",
        factory = captionViewModelFactory(container, projectId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Kept as State and read only inside the manual rows, so a keystroke does not
    // recompose the whole panel.
    val draftsState = viewModel.drafts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // The platform recogniser owns the microphone and needs a Looper, so it
    // lives with the screen and is released when the screen goes away.
    val transcriber = remember { container.newSpeechTranscriber() }
    // Kept as State, not values: only the Auto tab reads them, so a partial
    // recognition result no longer recomposes the whole screen.
    val listeningState = transcriber.listening.collectAsStateWithLifecycle()
    val partialState = transcriber.partial.collectAsStateWithLifecycle()
    val transcriberNotice by transcriber.notice.collectAsStateWithLifecycle()
    val available = remember { transcriber.isAvailable }
    val anchorMs = atMs.coerceAtLeast(0L)
    var tab by remember { mutableStateOf(0) }
    var localNotice by remember { mutableStateOf<String?>(null) }

    // Recognised phrases are stamped with measured elapsed time; they map onto
    // the timeline from the playhead the panel was opened at.
    LaunchedEffect(transcriber) {
        transcriber.phrases.collect { phrase ->
            viewModel.addLines(
                CaptionSegmenter.segment(
                    text = phrase.text,
                    startMs = phrase.startMs + anchorMs,
                    endMs = phrase.endMs + anchorMs
                )
            )
        }
    }
    DisposableEffect(transcriber) {
        onDispose { transcriber.stop() }
    }

    val startListening: () -> Unit = {
        localNotice = null
        transcriber.start()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else localNotice = "Izin mikrofon ditolak."
    }
    val onTranscribe: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val srtPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        // Bounded read: a hand-made multi-megabyte .srt must not
                        // be pulled into one String just to be truncated later.
                        val reader = InputStreamReader(stream, Charsets.UTF_8)
                        val buffer = CharArray(16 * 1024)
                        val out = StringBuilder()
                        var total = 0
                        while (true) {
                            val read = reader.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_SRT_CHARS) break
                            out.append(buffer, 0, read)
                        }
                        out.toString()
                    }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                localNotice = "Berkas SRT tidak bisa dibaca."
                return@launch
            }
            viewModel.importSrt(text) { imported ->
                localNotice = if (imported) {
                    "SRT diimpor."
                } else {
                    "SRT tidak diterapkan: tidak ada baris terbaca atau gagal disimpan."
                }
            }
        }
    }

    val exportSrt: () -> Unit = {
        val project = state.project
        if (project == null) {
            localNotice = "Draft tidak ditemukan."
        } else {
            scope.launch {
                val srt = SrtFormat.write(state.transcript)
                if (srt.isEmpty()) {
                    localNotice = "Belum ada baris caption untuk diekspor."
                    return@launch
                }
                val file = File(container.exportDir, srtFileName(project.name))
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        container.exportDir.mkdirs()
                        file.writeText(srt)
                    }.isSuccess
                }
                if (!written) {
                    localNotice = "Gagal menulis berkas SRT."
                    return@launch
                }
                if (!ExportDelivery.share(context, file, "application/x-subrip")) {
                    localNotice = "SRT tersimpan sebagai ${file.name}, tapi tidak ada aplikasi " +
                        "untuk membagikannya."
                }
            }
        }
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Captions",
                subtitle = state.project?.let { "${it.name} · transkrip lokal" },
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    )
                },
                actions = {
                    TextButton(
                        onClick = exportSrt,
                        enabled = state.project != null && state.cues.isNotEmpty()
                    ) {
                        Text("SRT")
                    }
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
                    message = state.error!!,
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

            else -> CaptionBody(
                project = state.project!!,
                cues = state.cues,
                styleSeed = state.cues.firstOrNull()?.let(CaptionEditor::styleOf)
                    ?: CaptionEditor.defaultStyle(),
                anchorMs = anchorMs,
                tab = tab,
                onTabChange = { tab = it },
                listening = listeningState,
                partial = partialState,
                available = available,
                notice = localNotice ?: transcriberNotice,
                onTranscribe = onTranscribe,
                onStop = { transcriber.stop() },
                onAddCue = { text -> viewModel.addCue(text, anchorMs, anchorMs + 3_000L) },
                drafts = draftsState,
                onMarkDraft = viewModel::markDraft,
                onDeleteCue = viewModel::removeCue,
                onApplyStyle = viewModel::setStyle,
                onClearAll = viewModel::clearAll,
                onImportSrt = { srtPicker.launch(arrayOf("text/plain", "application/x-subrip", "*/*")) },
                onFlushDrafts = viewModel::flushDrafts,
                modifier = modifier
            )
        }
    }
}

/** Name for the exported subtitle file: never lets a draft name escape the folder. */
private fun srtFileName(projectName: String): String {
    val safe = projectName
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_', '.')
        .take(48)
        .ifBlank { "draft" }
    return "$safe-captions.srt"
}

@Composable
private fun CaptionBody(
    project: Project,
    cues: List<CaptionCue>,
    styleSeed: TextConfig,
    anchorMs: Long,
    tab: Int,
    onTabChange: (Int) -> Unit,
    listening: State<Boolean>,
    partial: State<String>,
    available: Boolean,
    notice: String?,
    onTranscribe: () -> Unit,
    onStop: () -> Unit,
    onAddCue: (String) -> Unit,
    drafts: State<Map<String, String>>,
    onMarkDraft: (String, String) -> Unit,
    onDeleteCue: (String) -> Unit,
    onApplyStyle: (TextConfig) -> Unit,
    onClearAll: () -> Unit,
    onImportSrt: () -> Unit,
    onFlushDrafts: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
    ) {
        CaptionPreviewCard(project = project, cue = cueAt(cues, anchorMs), cueCount = cues.size)

        ToolSegmented(
            options = listOf("Auto", "Manual", "Style"),
            selectedIndex = tab,
            onSelect = onTabChange
        )

        if (notice != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        when (tab) {
            0 -> CaptionAutoTab(
                cues = cues,
                listening = listening,
                partial = partial,
                available = available,
                onTranscribe = onTranscribe,
                onStop = onStop
            )

            1 -> CaptionManualTab(
                cues = cues,
                anchorMs = anchorMs,
                onAddCue = onAddCue,
                drafts = drafts,
                onMarkDraft = onMarkDraft,
                onDeleteCue = onDeleteCue,
                onImportSrt = onImportSrt,
                onClearAll = onClearAll,
                onFlushDrafts = onFlushDrafts
            )

            else -> CaptionStyleTab(seed = styleSeed, onApply = onApplyStyle)
        }
        Spacer(Modifier.height(ZcutSpacing.xl))
    }
}

private fun cueAt(cues: List<CaptionCue>, positionMs: Long): CaptionCue? =
    cues.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs } ?: cues.firstOrNull()

@Composable
private fun CaptionAutoTab(
    cues: List<CaptionCue>,
    listening: State<Boolean>,
    partial: State<String>,
    available: Boolean,
    onTranscribe: () -> Unit,
    onStop: () -> Unit
) {
    // Read the live recognition state here, in the Auto tab's own scope.
    val isListening = listening.value
    val heard = partial.value

    ToolSection(title = "Transkripsi") {
        ToolHint(
            "Transkripsi akurat multi-bahasa BUTUH BACKEND. Tombol lokal memakai " +
                "SpeechRecognizer perangkat: putar audio klip lewat speaker lalu mulai."
        )
        if (!available) {
            ToolHint("Perangkat ini tidak punya layanan pengenalan suara lokal.")
        }
        if (isListening) {
            ZcutSecondaryButton(
                text = "Berhenti mendengarkan",
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            )
            if (heard.isNotBlank()) {
                ToolValueRow("Terdengar", heard)
            }
        } else {
            ZcutPrimaryButton(
                text = "Transcribe locally",
                onClick = onTranscribe,
                enabled = available,
                modifier = Modifier.fillMaxWidth()
            )
        }
        ToolHint(
            "Waktu tiap frasa diukur dengan stopwatch sejak mendengarkan dimulai, " +
                "lalu digeser ke posisi playhead panel dibuka. Tidak ada perkiraan " +
                "waktu per kata."
        )
    }

    ToolSection(title = "Transcript · ${cues.size} baris") {
        if (cues.isEmpty()) {
            ToolHint("Belum ada caption. Pakai tab Manual untuk menulis sendiri.")
        } else {
            // Lazy + keyed: a long SRT would otherwise compose every row at once.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MAX_LIST_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cues, key = { it.id }) { cue -> CaptionLineRow(cue) }
            }
        }
    }
}

@Composable
private fun CaptionManualTab(
    cues: List<CaptionCue>,
    anchorMs: Long,
    onAddCue: (String) -> Unit,
    drafts: State<Map<String, String>>,
    onMarkDraft: (String, String) -> Unit,
    onDeleteCue: (String) -> Unit,
    onImportSrt: () -> Unit,
    onClearAll: () -> Unit,
    onFlushDrafts: () -> Unit
) {
    var newText by remember { mutableStateOf("") }

    // Leaving the tab (or the screen) must not drop a half-typed row: the
    // ViewModel holds the pending text and is asked to write it right away.
    DisposableEffect(Unit) {
        onDispose { onFlushDrafts() }
    }

    ToolSection(title = "Tambah baris") {
        OutlinedTextField(
            value = newText,
            onValueChange = { newText = it.take(CaptionEditor.MAX_LINE_LENGTH) },
            label = { Text("Teks caption") },
            minLines = 1,
            maxLines = 3,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        ZcutPrimaryButton(
            text = "Tambah di ${TimeFormat.clockMillis(anchorMs)}",
            onClick = {
                onAddCue(newText)
                newText = ""
            },
            enabled = newText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        ZcutSecondaryButton(
            text = "Impor SRT",
            onClick = onImportSrt,
            modifier = Modifier.fillMaxWidth()
        )
        ToolHint(
            "Impor SRT mengganti seluruh transkrip. Ekspor SRT ada di tombol pada " +
                "bar atas."
        )
    }

    ToolSection(title = "Edit baris · ${cues.size}") {
        if (cues.isEmpty()) {
            ToolHint("Belum ada baris untuk diedit.")
        } else {
            // Lazy + keyed: each manual row is a text field, so rendering the
            // whole 2000-cue cap at once froze the panel.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = MAX_LIST_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cues, key = { it.id }) { cue ->
                    // Read the draft in the item's own scope: a keystroke
                    // recomposes one row, not the panel.
                    val draft = drafts.value[cue.id] ?: cue.text
                    CaptionEditRow(
                        cue = cue,
                        text = draft,
                        onDraftChange = { text -> onMarkDraft(cue.id, text) },
                        onDelete = onDeleteCue
                    )
                }
            }
            ToolHint("Kosongkan teks sebuah baris untuk menghapusnya.")
            ZcutSecondaryButton(
                text = "Hapus semua caption",
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CaptionStyleTab(seed: TextConfig, onApply: (TextConfig) -> Unit) {
    // Seeded once: the panel styles *all* cues, so the seed is only the starting
    // point for the chips, not a live binding to any single cue.
    var style by remember { mutableStateOf(seed) }

    ToolSection(title = "Style") {
        ToolChipRow {
            StylePresets.forEach { preset ->
                val selected = style.stylePreset == preset
                ToolChip(
                    label = styleLabel(preset),
                    selected = selected,
                    onClick = {
                        style = style.copy(
                            stylePreset = if (selected) TextStylePreset.NONE else preset
                        )
                    }
                )
            }
        }
        ToolSlider(
            label = "Size",
            value = style.fontSizeSp,
            valueRange = 12f..72f,
            onValueChange = { style = style.copy(fontSizeSp = it) },
            onValueChangeFinished = {},
            valueLabel = { "${it.toInt()} pt" }
        )
    }

    ToolSection(title = "Warna") {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextColors.forEach { argb ->
                ColorSwatch(
                    argb = argb,
                    selected = style.colorArgb == argb,
                    onClick = { style = style.copy(colorArgb = argb) }
                )
            }
        }
    }

    ToolSection(title = "Terapkan") {
        ZcutPrimaryButton(
            text = "Terapkan ke semua baris",
            onClick = { onApply(style) },
            modifier = Modifier.fillMaxWidth()
        )
        ToolHint(
            "Style diterapkan ke semua caption. Style per baris dan highlight kata " +
                "menyusul (P2)."
        )
    }
}

@Composable
private fun CaptionLineRow(cue: CaptionCue) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = TimeFormat.clock(cue.startMs),
                style = MaterialTheme.typography.labelSmall,
                color = ZcutLime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = cue.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CaptionEditRow(
    cue: CaptionCue,
    text: String,
    onDraftChange: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${TimeFormat.clockMillis(cue.startMs)} → " +
                        TimeFormat.clockMillis(cue.endMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                ZcutIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Hapus baris",
                    onClick = { onDelete(cue.id) }
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { onDraftChange(it.take(CaptionEditor.MAX_LINE_LENGTH)) },
                minLines = 1,
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CaptionPreviewCard(project: Project, cue: CaptionCue?, cueCount: Int) {
    val thumbnails = LocalAppContainer.current.mediaThumbnails
    val cover = remember(project.id, project.assets, project.timeline) {
        project.coverAsset()
    }
    val thumbnail by rememberThumbnail(key = cover?.id) {
        if (cover == null) null else thumbnails.load(cover, 720)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = "Pratinjau caption",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        val frameHeightDp = maxHeight.value
        val config = cue?.let { CaptionEditor.styleOf(it).copy(text = it.text) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (config == null) {
                Text(
                    text = "Belum ada caption",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            } else {
                ZcutText(config = config, frameHeightDp = frameHeightDp)
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "CAPTION · $cueCount BARIS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

/** Bounded window for the lazy transcript/edit lists inside the scrolling page. */
private val MAX_LIST_HEIGHT = 420.dp

/** Ceiling on how much of a picked .srt file is read into memory. */
private const val MAX_SRT_CHARS = 4 * 1024 * 1024