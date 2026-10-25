package com.zaaam.zcut.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.zcut.core.domain.BeautyEditor
import com.zaaam.zcut.core.domain.BeautySlider
import com.zaaam.zcut.core.media.BeautyProcessor
import com.zaaam.zcut.core.model.BeautyConfig
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.render.FaceGeometry
import com.zaaam.zcut.core.util.TimeFormat
import com.zaaam.zcut.feature.media.rememberThumbnail
import com.zaaam.zcut.ui.LocalAppContainer
import com.zaaam.zcut.ui.clipToolViewModelFactory
import com.zaaam.zcut.ui.theme.ZcutSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/** Result of running the on-device face model over the clip's still frame. */
private sealed interface FaceProbe {
    data object Pending : FaceProbe
    data object NoFrame : FaceProbe

    /** The bundled model could not be loaded; say so instead of blaming the frame. */
    data object Unavailable : FaceProbe
    data class Faces(val detected: List<FaceGeometry>) : FaceProbe
}

@Composable
fun BeautyScreen(
    projectId: String,
    clipId: String?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ClipToolViewModel = viewModel(
        key = "beauty:$projectId:${clipId.orEmpty()}",
        factory = clipToolViewModelFactory(container, projectId, clipId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClipToolScreen(
        title = "Retouch",
        subtitle = state.clip?.let { clip ->
            val asset = clip.assetId?.let { id ->
                state.project?.assets?.firstOrNull { it.id == id }
            }
            "${asset?.displayName ?: "Klip"} · ${TimeFormat.clock(clip.durationMs)}"
        }.orEmpty(),
        state = state,
        onBack = onBack
    ) { project, clip ->
        BeautyBody(
            project = project,
            clip = clip,
            onBeauty = viewModel::setBeauty
        )
    }
}

@Composable
private fun BeautyBody(
    project: Project,
    clip: Clip,
    onBeauty: (BeautyConfig?) -> Unit
) {
    val container = LocalAppContainer.current
    val asset = clip.assetId?.let { id -> project.assets.firstOrNull { it.id == id } }
    val frame by rememberThumbnail(key = asset?.id) {
        if (asset == null) null else container.mediaThumbnails.load(asset, 720)
    }

    // Sliders edit a local draft (instant number feedback); the draft is written
    // on release so the per-pixel pass below runs once per commit.
    var draft by remember(clip.id, clip.beauty) {
        mutableStateOf(clip.beauty ?: BeautyConfig())
    }
    val committed = clip.beauty

    val probe by produceState<FaceProbe>(FaceProbe.Pending, frame) {
        val bitmap = frame
        value = when {
            bitmap == null -> FaceProbe.NoFrame
            else -> container.faceDetector.detect(bitmap.asAndroidBitmap())
                ?.let(FaceProbe::Faces)
                ?: FaceProbe.Unavailable
        }
    }

    val faces = (probe as? FaceProbe.Faces)?.detected.orEmpty()
    // The mockup shows one bounding box; the largest face is the one the
    // sliders act on.
    val subject = remember(faces) {
        faces.maxByOrNull { it.bounds.width * it.bounds.height }
    }

    val composite by produceState<ImageBitmap?>(null, probe, committed) {
        val source = frame
        val face = subject
        val config = committed
        value = if (source == null || face == null || config == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    BeautyProcessor.apply(source.asAndroidBitmap(), face, config)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    val detecting = probe == FaceProbe.Pending
    val label = when {
        probe == FaceProbe.NoFrame -> "TANPA MEDIA"
        detecting -> "MENDETEKSI WAJAH..."
        probe == FaceProbe.Unavailable -> "MODEL WAJAH TIDAK TERSEDIA"
        else -> BeautyEditor.faceLabel(faces.size)
    }

    BeautyPreview(
        label = label,
        frame = frame,
        composite = composite,
        face = subject,
        busy = detecting
    )

    var tab by remember(clip.id) { mutableStateOf(BeautyEditor.TABS.first()) }
    ToolSegmented(
        options = BeautyEditor.TABS,
        selectedIndex = BeautyEditor.TABS.indexOf(tab),
        onSelect = { index -> tab = BeautyEditor.TABS[index] }
    )

    if (tab == "Face") {
        FaceTab(
            draft = draft,
            onDraftChange = { draft = it },
            onCommit = { onBeauty(it) },
            subject = subject,
            probe = probe,
            committed = committed
        )
    } else {
        ToolSection(title = tab) {
            ToolHint(BeautyEditor.tabNote(tab).orEmpty())
        }
    }
}

@Composable
private fun FaceTab(
    draft: BeautyConfig,
    onDraftChange: (BeautyConfig) -> Unit,
    onCommit: (BeautyConfig) -> Unit,
    subject: FaceGeometry?,
    probe: FaceProbe,
    committed: BeautyConfig?
) {
    ToolSection(title = "Wajah") {
        when {
            probe == FaceProbe.NoFrame ->
                ToolHint("Klip ini belum punya media. Tambahkan media untuk memakai retouch.")

            probe == FaceProbe.Unavailable ->
                ToolHint(
                    "Model deteksi wajah (ML Kit, dibundel APK) gagal dimuat di perangkat ini. " +
                        "Fitur tetap terbuka dan slider tetap bisa digeser, tapi hasilnya belum " +
                        "bisa ditampilkan sampai model tersedia."
                )

            subject == null && probe !is FaceProbe.Pending ->
                ToolHint(
                    "Model tidak menemukan wajah pada frame ini, jadi slider tidak dijalankan " +
                        "(bukan hasil tebakan). Slot yang butuh wajah: Face slim, Eyes, Nose, Lips."
                )

            else -> ToolHint(
                "Deteksi wajah dan seluruh pemrosesan berjalan di perangkat dengan model ML Kit " +
                    "yang dibundel APK. Tidak ada wajah yang diunggah."
            )
        }

        BeautyEditor.FACE_SLIDERS.forEach { slider ->
            val value = slider.uiValue(draft)
            ToolSlider(
                label = slider.label,
                value = value,
                valueRange = 0f..100f,
                onValueChange = { onDraftChange(slider.withValue(draft, it)) },
                onValueChangeFinished = { onCommit(draft) },
                valueLabel = { position -> position.toInt().toString() }
            )
        }

        ToolHint(
            "Key asli tiap slider (assets/local_beauty_all.json): " +
                BeautyEditor.FACE_SLIDERS.joinToString(" · ") { "${it.label} → ${it.key}" }
        )
        ToolHint(BeautyEditor.summary(committed ?: BeautyConfig()))
    }

    ToolSection(title = "Preserve texture") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jaga detail pori",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = draft.preserveTexture,
                onCheckedChange = { keep ->
                    val updated = draft.copy(preserveTexture = keep)
                    onDraftChange(updated)
                    onCommit(updated)
                }
            )
        }
        ToolHint(
            if (draft.preserveTexture) {
                "Smoothing dilembutkan supaya pori tetap terlihat (kurangi efek plastik)."
            } else {
                "Smoothing penuh: kulit lebih rata, detail pori berkurang."
            }
        )
        ToolHint(
            "Retouch memakai satu frame klip dan pratinjaunya baru; export belum membakar " +
                "efek ini (batas render M13)."
        )
    }
}

/**
 * Preview with the detected face box, mirroring the mockup. The image is fitted
 * (not cropped) so the normalised geometry maps 1:1 onto the drawn box.
 */
@Composable
private fun BeautyPreview(
    label: String,
    frame: ImageBitmap?,
    composite: ImageBitmap?,
    face: FaceGeometry?,
    busy: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        val shown = composite ?: frame
        if (shown != null) {
            Image(
                bitmap = shown,
                contentDescription = "Pratinjau retouch",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Pratinjau tidak tersedia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (face != null && shown != null) {
            // The image is fitted (letterboxed), so the box has to be drawn in
            // the same scaled space instead of the full panel — otherwise a
            // portrait frame in a wide panel would show the box next to the face.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 1.5.dp.toPx()
                val scale = min(
                    size.width / shown.width.toFloat(),
                    size.height / shown.height.toFloat()
                )
                val shownWidth = shown.width * scale
                val shownHeight = shown.height * scale
                val left = (size.width - shownWidth) / 2f
                val top = (size.height - shownHeight) / 2f
                drawRect(
                    color = Color.White,
                    topLeft = Offset(
                        left + face.bounds.left * shownWidth,
                        top + face.bounds.top * shownHeight
                    ),
                    size = Size(
                        face.bounds.width * shownWidth,
                        face.bounds.height * shownHeight
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            }
        }

        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
        }

        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
    Box(modifier = Modifier.height(ZcutSpacing.xs))
}
