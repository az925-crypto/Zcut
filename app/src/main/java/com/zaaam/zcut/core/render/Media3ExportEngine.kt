package com.zaaam.zcut.core.render

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Local export through Media3's [Transformer]. The same [PreviewPlan] the
 * preview plays is turned into a composition, so a draft that cannot play also
 * cannot export — and the UI can say exactly which media is missing.
 *
 * Everything runs on the main thread because a Transformer must be created,
 * started, queried and cancelled from a single thread with a Looper.
 */
@OptIn(UnstableApi::class)
class Media3ExportEngine(
    private val context: Context
) : VideoRenderEngine {

    @Volatile
    private var transformer: Transformer? = null

    override fun render(request: RenderRequest): Flow<RenderProgress> = channelFlow {
        try {
            val project = request.project
            val output = File(request.outputPath)
            if (!output.isAbsolute) {
                send(RenderProgress.Failed("Lokasi ekspor tidak valid."))
                return@channelFlow
            }

            val plan = try {
                PreviewPlanBuilder.build(project, context.filesDir.absolutePath)
            } catch (error: IllegalArgumentException) {
                send(RenderProgress.Failed(error.message ?: "Rencana render tidak valid."))
                return@channelFlow
            }
            if (plan.videoItems.isEmpty() && plan.audioItems.isEmpty()) {
                send(RenderProgress.Failed("Belum ada media yang bisa diekspor. Tambahkan klip dulu."))
                return@channelFlow
            }

            val composition = try {
                Media3CompositionBuilder.build(
                    plan,
                    presentationShortSide = request.preset.resolution.shortEdge
                )
            } catch (error: IllegalArgumentException) {
                send(RenderProgress.Failed(error.message ?: "Komposisi tidak bisa dirender."))
                return@channelFlow
            }

            send(RenderProgress.Preparing)
            output.parentFile?.mkdirs()

            val finished = CompletableDeferred<RenderProgress>()
            val started = withContext(Dispatchers.Main.immediate) {
                val instance = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            finished.complete(
                                RenderProgress.Completed(
                                    outputPath = output.absolutePath,
                                    durationMs = plan.durationMs,
                                    sizeBytes = output.length()
                                )
                            )
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            finished.complete(
                                RenderProgress.Failed(
                                    message = exportException.message
                                        ?: "Render gagal di perangkat ini.",
                                    cause = exportException
                                )
                            )
                        }
                    })
                    .build()
                transformer = instance
                instance.start(composition, output.absolutePath)
                instance
            }

            val poller = launch(Dispatchers.Main.immediate) {
                val holder = ProgressHolder()
                while (isActive) {
                    delay(PROGRESS_INTERVAL_MS)
                    val state = started.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        val percent = (holder.progress / 100f).coerceIn(0f, 1f)
                        send(
                            if (percent >= 1f) {
                                RenderProgress.Finalizing
                            } else {
                                RenderProgress.Rendering(percent)
                            }
                        )
                    }
                }
            }

            val result = finished.await()
            poller.cancel()
            send(result)
        } finally {
            // Covers the collector being cancelled (screen closed mid-render):
            // the Transformer must not keep encoding once nobody listens.
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                runCatching { transformer?.cancel() }
                transformer = null
            }
        }
    }

    override suspend fun cancel() {
        withContext(Dispatchers.Main.immediate) {
            runCatching { transformer?.cancel() }
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 400L
    }
}
