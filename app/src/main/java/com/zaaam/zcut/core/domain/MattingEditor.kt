package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig

/**
 * Pure matting helpers: clamping for a config that arrives from a draft file
 * (untrusted), the confidence-to-alpha remap used by the compositor, and the
 * labels the cutout screen shows. No Android types, so all of it is unit-tested.
 */
object MattingEditor {

    private const val MAX_PROMPT_LENGTH = 200

    /**
     * Background tiles from the approved mockup. Deliberately flat colours: the
     * mockup draws stylised art, and inventing photographic backdrops would be
     * pretending we ship assets we do not have.
     */
    val COLOR_PRESETS: List<Pair<String, Long>> = listOf(
        "Studio" to 0xFF1D4ED8,
        "Forest" to 0xFF15803D,
        "Sunset" to 0xFFB45309
    )

    fun normalize(config: MattingConfig): MattingConfig = config.copy(
        blur = clamp01(config.blur),
        edgeRefine = clamp01(config.edgeRefine),
        shadow = clamp01(config.shadow),
        imageAssetId = config.imageAssetId?.takeIf { it.isNotBlank() },
        aiPrompt = config.aiPrompt.trim().take(MAX_PROMPT_LENGTH)
    )

    /** Confidence at or below this becomes fully transparent. */
    fun edgeThreshold(edgeRefine: Float): Float = clamp01(edgeRefine) * 0.6f

    /**
     * Remaps ML Kit confidence (0 = background, 1 = subject) into an alpha.
     * Below the threshold the pixel is dropped outright; above it the remaining
     * range is stretched, which is what "edge refine" means in the UI.
     */
    fun matteAlpha(confidence: Float, edgeRefine: Float): Float {
        val threshold = edgeThreshold(edgeRefine)
        val span = (1f - threshold).coerceAtLeast(0.01f)
        return clamp01((confidence - threshold) / span)
    }

    /**
     * Average confidence over the pixels the model calls foreground, as a
     * percentage. `null` when the matte found no subject at all, so the screen
     * can say so instead of printing a meaningless number.
     */
    fun subjectConfidencePercent(confidence: FloatArray): Int? {
        var sum = 0f
        var count = 0
        for (value in confidence) {
            if (value >= 0.5f) {
                sum += value.coerceIn(0f, 1f)
                count++
            }
        }
        if (count == 0) return null
        return Math.round(sum / count * 100f)
    }

    fun subjectLabel(percent: Int?): String =
        if (percent == null) "TIDAK ADA SUBYEK" else "SUBYEK · $percent%"

    fun backgroundLabel(background: MattingBackground): String = when (background) {
        MattingBackground.TRANSPARENT -> "Transparan"
        MattingBackground.COLOR -> "Warna"
        MattingBackground.BLUR -> "Blur"
        MattingBackground.IMAGE -> "Gambar"
        MattingBackground.AI -> "AI"
    }

    /** One-line description of what the cutout will do. */
    fun summary(config: MattingConfig): String = when (config.background) {
        MattingBackground.TRANSPARENT -> "Latar dihapus · transparan"
        MattingBackground.COLOR ->
            "Latar diganti warna #" + config.colorArgb.toString(16).uppercase().takeLast(6)
        MattingBackground.BLUR -> "Latar diburamkan " + "${(config.blur * 100).toInt()}%"
        MattingBackground.IMAGE ->
            if (config.imageAssetId == null) "Gambar latar belum dipilih" else "Latar pakai gambar"
        MattingBackground.AI -> "Latar AI · butuh backend"
    }

    /** Only the generative backdrop needs a server; everything else is local. */
    fun requiresBackend(background: MattingBackground): Boolean =
        background == MattingBackground.AI

    private fun clamp01(value: Float): Float =
        if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
}
