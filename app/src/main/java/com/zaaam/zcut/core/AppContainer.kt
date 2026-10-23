package com.zaaam.zcut.core

import android.content.Context
import com.zaaam.zcut.core.data.JsonProjectRepository
import com.zaaam.zcut.core.data.ProjectRepository
import com.zaaam.zcut.core.render.AudioMixer
import com.zaaam.zcut.core.render.EffectPipeline
import com.zaaam.zcut.core.render.MediaProbe
import com.zaaam.zcut.core.render.PreviewEngine
import com.zaaam.zcut.core.render.SpeechEngine
import com.zaaam.zcut.core.render.SubjectSegmenter
import com.zaaam.zcut.core.render.ThumbnailGenerator
import com.zaaam.zcut.core.render.UnavailableAudioMixer
import com.zaaam.zcut.core.render.UnavailableEffectPipeline
import com.zaaam.zcut.core.render.UnavailableMediaProbe
import com.zaaam.zcut.core.render.UnavailablePreviewEngine
import com.zaaam.zcut.core.render.UnavailableSpeechEngine
import com.zaaam.zcut.core.render.UnavailableSubjectSegmenter
import com.zaaam.zcut.core.render.UnavailableThumbnailGenerator
import com.zaaam.zcut.core.render.UnavailableVideoRenderEngine
import com.zaaam.zcut.core.render.VideoRenderEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val projectRepository: ProjectRepository by lazy { JsonProjectRepository(appContext) }

    // Stage 3 replaces these with Media3, CameraX, ML Kit, and Android TTS
    // implementations. Until then every missing capability fails honestly.
    val renderEngine: VideoRenderEngine by lazy { UnavailableVideoRenderEngine() }
    val previewEngine: PreviewEngine by lazy { UnavailablePreviewEngine() }
    val mediaProbe: MediaProbe by lazy { UnavailableMediaProbe() }
    val thumbnailGenerator: ThumbnailGenerator by lazy { UnavailableThumbnailGenerator() }
    val audioMixer: AudioMixer by lazy { UnavailableAudioMixer() }
    val speechEngine: SpeechEngine by lazy { UnavailableSpeechEngine() }
    val subjectSegmenter: SubjectSegmenter by lazy { UnavailableSubjectSegmenter() }
    val effectPipeline: EffectPipeline by lazy { UnavailableEffectPipeline() }
}
