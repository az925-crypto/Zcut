package com.zaaam.zcut.core.render

import com.zaaam.zcut.core.model.MediaAsset
import com.zaaam.zcut.core.model.Track

/**
 * Explicit placeholders for the Stage 3 wiring. They keep the app honest:
 * missing engines fail with a readable message instead of pretending to render.
 */
class UnavailableMediaProbe : MediaProbe {
    override suspend fun probe(uri: String): MediaAsset? = null
}

class UnavailableThumbnailGenerator : ThumbnailGenerator {
    override suspend fun generate(
        asset: MediaAsset,
        atMs: Long,
        outputPath: String
    ): Boolean = false
}

class UnavailableAudioMixer : AudioMixer {
    override suspend fun mix(tracks: List<Track>, outputPath: String): Boolean = false
}

class UnavailableSpeechEngine : SpeechEngine {
    override suspend fun synthesize(
        text: String,
        voiceId: String?,
        rate: Float,
        pitch: Float,
        outputPath: String
    ): Boolean = false
}

class UnavailableSubjectSegmenter : SubjectSegmenter {
    override suspend fun segment(inputUri: String, outputMaskPath: String): Boolean = false
}

class UnavailableEffectPipeline : EffectPipeline {
    override fun supports(effectId: String): Boolean = false
}
