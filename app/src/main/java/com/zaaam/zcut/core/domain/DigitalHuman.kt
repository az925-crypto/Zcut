package com.zaaam.zcut.core.domain

/**
 * Digital human catalog (F25).
 *
 * The artifact's avatars, dressings and templates are server-side assets that
 * cannot be copied. Zcut ships four self-authored presenter presets: a name, the
 * gradient art used for the tile/preview, and a background label. Rendering an
 * avatar with lip sync still needs a backend; these presets only drive the local
 * preparation draft.
 */
data class DigitalHumanAvatar(
    val id: String,
    val name: String,
    /** Palette key shared with [com.zaaam.zcut.feature.templates.TemplateArt]. */
    val artKey: String,
    val background: String,
    val colorTop: Long,
    val colorBottom: Long
)

object DigitalHumanCatalog {

    val avatars: List<DigitalHumanAvatar> = listOf(
        DigitalHumanAvatar(
            id = "ava",
            name = "Ava",
            artKey = "portrait",
            background = "Studio",
            colorTop = 0xFF2A1436,
            colorBottom = 0xFFA26A9E
        ),
        DigitalHumanAvatar(
            id = "mika",
            name = "Mika",
            artKey = "pink",
            background = "Loft",
            colorTop = 0xFF5A1740,
            colorBottom = 0xFFE86AA8
        ),
        DigitalHumanAvatar(
            id = "noa",
            name = "Noa",
            artKey = "blue",
            background = "Rooftop",
            colorTop = 0xFF102B4E,
            colorBottom = 0xFF4A9BE8
        ),
        DigitalHumanAvatar(
            id = "rin",
            name = "Rin",
            artKey = "gold",
            background = "Desk",
            colorTop = 0xFF7A4A12,
            colorBottom = 0xFFE8B04B
        )
    )

    val default: DigitalHumanAvatar = avatars.first()

    fun byId(id: String?): DigitalHumanAvatar =
        avatars.firstOrNull { it.id == id } ?: default
}

/**
 * What the user asked the digital-human screen for. The only local output is a
 * preparation draft (background + TTS voice + estimated subtitle lines); the
 * avatar and lip sync render is backend-only and never claimed as done.
 */
data class DigitalHumanRequest(
    val avatarId: String = DigitalHumanCatalog.default.id,
    val script: String = "",
    val voiceId: String? = null,
    val rate: Float = 1f,
    val pitch: Float = 1f,
    val subtitles: Boolean = true
) {

    val isReady: Boolean get() = script.isNotBlank()

    fun sanitized(): DigitalHumanRequest = copy(
        avatarId = DigitalHumanCatalog.byId(avatarId).id,
        script = TtsScript.sanitize(script),
        rate = rate.coerceIn(MIN_RATE, MAX_RATE),
        pitch = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
    )

    companion object {
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2f
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2f
        const val MAX_SCRIPT = TtsScript.MAX_LENGTH
    }
}

/** Pure planning rules for the local half of the pipeline. */
object DigitalHumanPlan {

    /**
     * Subtitle lines for the synthesised script. Timing is split across the
     * measured TTS duration proportionally to line length, which is an estimate:
     * the script has no word timings. The UI says so instead of presenting the
     * lines as recognised speech.
     */
    fun captions(
        script: String,
        durationMs: Long,
        maxChars: Int = CaptionSegmenter.DEFAULT_MAX_CHARS
    ): List<CaptionSegmenter.Line> {
        if (durationMs <= 0L) return emptyList()
        val text = TtsScript.sanitize(script)
        if (text.isBlank()) return emptyList()
        return CaptionSegmenter.segment(text, 0L, durationMs, maxChars)
    }
}
