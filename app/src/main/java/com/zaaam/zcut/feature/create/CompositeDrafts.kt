package com.zaaam.zcut.feature.create

import android.graphics.Bitmap
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.domain.DraftSummaries
import com.zaaam.zcut.core.domain.ProjectFactory
import com.zaaam.zcut.core.media.CompositeRenderer
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.MediaKind
import com.zaaam.zcut.core.model.Project
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared plumbing for the three Create tools: render a still, write it into the
 * app workspace, and turn it into a normal draft so the existing preview and
 * export pipelines apply unchanged.
 */
internal object CompositeDrafts {

    const val STILL_DURATION_MS = 3_000L

    /** Fits [aspectRatio] to a [longEdge]-pixel long side. */
    fun outputSize(
        aspectRatio: AspectRatio,
        longEdge: Int = CompositeRenderer.DEFAULT_LONG_EDGE
    ): Pair<Int, Int> {
        val width = aspectRatio.width.coerceAtLeast(1)
        val height = aspectRatio.height.coerceAtLeast(1)
        val edge = longEdge.coerceAtLeast(1)
        return if (width >= height) {
            edge to (edge * height / width).coerceAtLeast(1)
        } else {
            (edge * width / height).coerceAtLeast(1) to edge
        }
    }

    /**
     * Loads one frame per asset, renders the composite, saves a JPEG and
     * inserts a draft. Returns null when a source frame is unreadable, the
     * renderer refuses, or the file cannot be written.
     */
    suspend fun renderDraft(
        repository: ProjectRepository,
        thumbnails: MediaThumbnails,
        directory: File,
        assets: List<MediaAsset>,
        aspectRatio: AspectRatio,
        prefix: String,
        render: (List<Bitmap?>, Int, Int) -> Bitmap?
    ): Project? = withContext(Dispatchers.IO) {
        if (assets.isEmpty()) return@withContext null
        val frames = assets.map { thumbnails.load(it, CompositeRenderer.DEFAULT_LONG_EDGE) }
        if (frames.any { it == null }) return@withContext null

        val (width, height) = outputSize(aspectRatio)
        val composite = render(frames, width, height) ?: return@withContext null
        try {
            val file = uniqueFile(directory, prefix)
            if (!CompositeRenderer.saveJpeg(composite, file)) return@withContext null
            createDraft(
                repository = repository,
                still = file,
                width = width,
                height = height,
                aspectRatio = aspectRatio
            )
        } finally {
            composite.recycle()
        }
    }

    /** Inserts a draft whose first clip is [still]; extra clips follow in order. */
    suspend fun createDraft(
        repository: ProjectRepository,
        still: File,
        width: Int,
        height: Int,
        aspectRatio: AspectRatio,
        stillDurationMs: Long = STILL_DURATION_MS,
        extraAssets: List<MediaAsset> = emptyList()
    ): Project? = withContext(Dispatchers.IO) {
        if (!still.isFile || still.length() <= 0L) return@withContext null
        val image = MediaAsset(
            displayName = still.name,
            mimeType = "image/jpeg",
            kind = MediaKind.IMAGE,
            durationMs = stillDurationMs.coerceAtLeast(1L),
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            sizeBytes = still.length(),
            localPath = still.absolutePath
        )
        val taken = repository.summaries().map { it.name }.toSet()
        val project = ProjectFactory.fromAssets(
            name = DraftSummaries.nextName(taken),
            assets = listOf(image) + extraAssets,
            aspectRatio = aspectRatio
        )
        repository.upsert(project)
        project
    }

    private fun uniqueFile(directory: File, prefix: String): File {
        directory.mkdirs()
        val stamp = System.currentTimeMillis()
        while (true) {
            val suffix = Random.nextInt(0x10000).toString(16).padStart(4, '0')
            val candidate = File(directory, "${prefix}_${stamp}_$suffix.jpg")
            if (!candidate.exists()) return candidate
        }
    }
}
