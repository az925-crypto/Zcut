package com.zaaam.zcut.core

import android.content.Context
import com.zaaam.zcut.core.data.AppStorage
import com.zaaam.zcut.core.data.DataStorePreferencesRepository
import com.zaaam.zcut.core.data.JsonProjectRepository
import com.zaaam.zcut.core.data.PreferencesRepository
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.media.AndroidSpeechEngine
import com.zaaam.zcut.core.media.MediaImporter
import com.zaaam.zcut.core.media.MediaThumbnails
import com.zaaam.zcut.core.media.VoiceoverRecorder
import com.zaaam.zcut.core.render.AudioMixer
import com.zaaam.zcut.core.render.EffectPipeline
import com.zaaam.zcut.core.render.Media3ExportEngine
import com.zaaam.zcut.core.render.FaceDetector
import com.zaaam.zcut.core.render.Media3PreviewEngine
import com.zaaam.zcut.core.render.MlKitFaceDetector
import com.zaaam.zcut.core.render.MlKitSubjectSegmenter
import com.zaaam.zcut.core.render.MediaProbe
import com.zaaam.zcut.core.render.PreviewEngine
import com.zaaam.zcut.core.render.SpeechEngine
import com.zaaam.zcut.core.render.SubjectSegmenter
import com.zaaam.zcut.core.render.ThumbnailGenerator
import com.zaaam.zcut.core.render.UnavailableAudioMixer
import com.zaaam.zcut.core.render.UnavailableEffectPipeline
import com.zaaam.zcut.core.render.UnavailableMediaProbe
import com.zaaam.zcut.core.render.UnavailableThumbnailGenerator
import com.zaaam.zcut.core.render.VideoRenderEngine
import java.io.File

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val projectRepository: ProjectRepository by lazy { JsonProjectRepository(appContext) }

    val preferencesRepository: PreferencesRepository by lazy {
        DataStorePreferencesRepository(appContext)
    }

    val storage: AppStorage by lazy { AppStorage(appContext.filesDir, appContext.cacheDir) }

    private val mediaDir: File by lazy { File(appContext.filesDir, "media") }
    private val thumbnailDir: File by lazy { File(appContext.cacheDir, "thumbs") }

    /** Synthesised speech cache; survives "Bersihkan cache" on purpose. */
    val ttsCacheDir: File by lazy { File(appContext.filesDir, "tts") }

    /** Finished renders; private to the app until the user shares or saves one. */
    val exportDir: File by lazy { File(appContext.filesDir, "exports") }

    val mediaThumbnails: MediaThumbnails by lazy {
        MediaThumbnails(appContext, thumbnailDir)
    }

    val mediaImporter: MediaImporter by lazy {
        MediaImporter(appContext, mediaDir, thumbnailDir)
    }

    // Stage 3 replaces these with CameraX implementations. Until then every
    // missing capability fails honestly.
    val mediaProbe: MediaProbe by lazy { UnavailableMediaProbe() }
    val thumbnailGenerator: ThumbnailGenerator by lazy { UnavailableThumbnailGenerator() }
    val audioMixer: AudioMixer by lazy { UnavailableAudioMixer() }
    val speechEngine: SpeechEngine by lazy { AndroidSpeechEngine(appContext) }

    /**
     * ML Kit's selfie model is bundled in the APK and loaded once per process,
     * so the segmenter is app-scoped rather than per-screen.
     */
    val subjectSegmenter: SubjectSegmenter by lazy { MlKitSubjectSegmenter() }

    /**
     * Face geometry for the retouch sliders. Like the segmenter, the bundled
     * model is loaded once per process, so one instance is app-scoped.
     */
    val faceDetector: FaceDetector by lazy { MlKitFaceDetector() }
    val effectPipeline: EffectPipeline by lazy { UnavailableEffectPipeline() }

    /**
     * A recorder owns the microphone, so one is created per recording session
     * and released by the screen that started it.
     */
    fun newVoiceoverRecorder(): VoiceoverRecorder = VoiceoverRecorder(appContext)

    /**
     * A player owns a GL pipeline and a surface, so it cannot be a singleton:
     * every editor screen gets its own engine and releases it in onCleared.
     */
    fun newPreviewEngine(): PreviewEngine = Media3PreviewEngine(appContext)

    /**
     * A Transformer must be driven from one thread and only one export may run
     * at a time, so each export screen owns its engine.
     */
    fun newRenderEngine(): VideoRenderEngine = Media3ExportEngine(appContext)
}
