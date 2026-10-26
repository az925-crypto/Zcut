package com.zaaam.zcut.feature.photo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.ColorPipeline
import com.zaaam.zcut.feature.tools.ToolChip
import com.zaaam.zcut.feature.tools.ToolChipRow
import com.zaaam.zcut.feature.tools.ToolHint
import com.zaaam.zcut.feature.tools.ToolSection
import com.zaaam.zcut.feature.tools.ToolSegmented
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.cameraViewModelFactory
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutSecondaryButton
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-app camera (F26). The viewfinder, photo capture and video recording are
 * CameraX — the platform pipeline, no proprietary SDK.
 *
 * An honest limit: the original shows EffectSDK beauty/filter *live* in the
 * viewfinder. Zcut cannot, so beauty and the filter are carried into the result
 * instead (the info row says which ones are active).
 */
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onDraftCreated: (String) -> Unit,
    onOpenGallery: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: CameraViewModel = viewModel(factory = cameraViewModelFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // The PreviewView is not state: it is captured here for the bind effect, which
    // is re-run once layout has attached it.
    val previewRef = remember { arrayOfNulls<PreviewView>(1) }
    var previewReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember {
        Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed ->
        granted = allowed
        if (!allowed) error = "Izin kamera ditolak; viewfinder tidak bisa dibuka."
    }

    // Bind (or re-bind) whenever the provider, the view or the facing changes.
    LaunchedEffect(granted, provider, previewReady, state.facing) {
        val cameraProvider = provider ?: return@LaunchedEffect
        val view = previewRef[0] ?: return@LaunchedEffect
        if (!granted) return@LaunchedEffect
        runCatching {
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(view.surfaceProvider)
            val selector = if (state.facing == CameraFacing.FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture,
                videoCapture
            )
            error = null
        }.onFailure { failure ->
            error = "Kamera tidak bisa dibuka di perangkat ini (" +
                (failure.message ?: "tak dikenal") + ")."
        }
    }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { provider = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            runCatching { recording?.stop() }
            runCatching { provider?.unbindAll() }
        }
    }

    fun stopRecording() {
        runCatching { recording?.stop() }
        recording = null
        viewModel.setRecording(false)
    }

    fun startRecording() {
        val dir = container.cameraDir
        dir.mkdirs()
        val target = File(dir, "clip-${System.currentTimeMillis()}.mp4")
        // Audio is only captured when the microphone permission is already held;
        // the clip is still recorded silently otherwise.
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val started = runCatching {
            val pending = recorder.prepareRecording(context, FileOutputOptions(target))
            if (audioGranted) pending.withAudioEnabled() else pending
        }
        if (started.isFailure) {
            error = "Rekaman tidak bisa dimulai (" +
                (started.exceptionOrNull()?.message ?: "tak dikenal") + ")."
            viewModel.setRecording(false)
            return
        }
        val slowMotion = state.mode == CameraMode.SLOW_MO
        recording = started.getOrThrow().start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                recording = null
                viewModel.setRecording(false)
                if (event.hasError()) {
                    error = "Rekaman gagal disimpan."
                } else {
                    viewModel.importRecording(Uri.fromFile(target), slowMotion) { projectId ->
                        if (projectId != null) onDraftCreated(projectId)
                    }
                }
            }
        }
        viewModel.setRecording(true)
    }

    fun takePhoto() {
        val dir = container.photoDir
        dir.mkdirs()
        val name = "capture-${System.currentTimeMillis()}.jpg"
        val target = File(dir, name)
        imageCapture.flashMode = when (state.flash) {
            FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
            FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashSetting.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(target).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(name)
                }

                override fun onError(exception: ImageCaptureException) {
                    error = "Foto gagal disimpan (" +
                        (exception.message ?: "tak dikenal") + ")."
                }
            }
        )
    }

    fun shutter() {
        when {
            state.recording -> stopRecording()
            state.isVideo -> startRecording()
            state.timerMs > 0L -> scope.launch {
                delay(state.timerMs)
                takePhoto()
            }

            else -> takePhoto()
        }
    }

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = "Kamera",
                subtitle = "${state.aspect.label} · ${if (state.isVideo) "video" else "foto"}",
                navigationIcon = {
                    ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Tutup",
                        onClick = {
                            if (state.recording) stopRecording()
                            onClose()
                        }
                    )
                }
            )
        }
    ) { insets ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(insets)

        if (!granted) {
            Box(modifier, contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Izin kamera dibutuhkan",
                    message = "Viewfinder, foto, dan rekaman berjalan di perangkat ini; " +
                        "tidak ada yang diunggah.",
                    actionLabel = "Izinkan kamera",
                    onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            return@ZcutScaffold
        }

        Column(modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewRef[0] = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { previewReady = true }
                )
                Text(
                    text = "9:16 · FHD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(ZcutSpacing.sm)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                if (state.recording) {
                    Text(
                        text = "REC",
                        style = MaterialTheme.typography.labelMedium,
                        color = ZcutLime,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(ZcutSpacing.sm)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ZcutSpacing.screen, vertical = ZcutSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(ZcutSpacing.sm)
            ) {
                ToolSegmented(
                    options = CameraMode.entries.map { it.label },
                    selectedIndex = CameraMode.entries.indexOf(state.mode),
                    onSelect = { viewModel.setMode(CameraMode.entries[it]) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZcutSecondaryButton(
                        text = "Galeri",
                        onClick = onOpenGallery,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (state.recording) ZcutLime else Color.White,
                        modifier = Modifier
                            .padding(horizontal = ZcutSpacing.lg)
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable { shutter() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .size(if (state.recording) 20.dp else 52.dp)
                                        .background(
                                            color = if (state.recording) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                Color.Red
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                )
                            } else {
                                Text(
                                    text = "O",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                    ZcutSecondaryButton(
                        text = if (state.facing == CameraFacing.BACK) "Depan" else "Belakang",
                        onClick = viewModel::toggleFacing,
                        enabled = !state.recording,
                        modifier = Modifier.weight(1f)
                    )
                }

                ToolChipRow {
                    ToolChip(
                        label = "BEAUTY ${if (state.effectiveBeauty) "ON" else "OFF"}",
                        selected = state.effectiveBeauty,
                        onClick = viewModel::toggleBeauty
                    )
                    ToolChip(
                        label = "FILTER · " + (
                            state.filterId?.let { ColorPipeline.preset(it)?.label } ?: "NONE"
                            ),
                        selected = state.filterId != null,
                        onClick = {
                            val ids = ColorPipeline.presets.map { it.id } + listOf<String?>(null)
                            val index = ids.indexOf(state.filterId)
                            viewModel.setFilter(ids[(index + 1) % ids.size])
                        }
                    )
                    ToolChip(
                        label = "TIMER · " + when (state.timerMs) {
                            3_000L -> "3s"
                            10_000L -> "10s"
                            else -> "OFF"
                        },
                        selected = state.timerMs > 0L,
                        onClick = viewModel::cycleTimer,
                        enabled = !state.isVideo
                    )
                    ToolChip(
                        label = "FLASH · " + state.flash.name,
                        selected = state.flash != FlashSetting.OFF,
                        onClick = viewModel::cycleFlash
                    )
                }

                val message = error ?: state.notice
                if (message != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                ToolSection {
                    ToolHint(
                        "Beauty dan filter di sini diterapkan ke hasil setelah diambil, " +
                            "bukan pratinjau langsung seperti EffectSDK aslinya. " +
                            "SLOW-MO merekam kecepatan normal lalu diputar 0.5x."
                    )
                }
            }
        }
    }
}