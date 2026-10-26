package com.zaaam.zcut.core.domain

/**
 * Ad Maker (F31) script rules.
 *
 * The shipped artifact splits a script into scenes (`AdScriptToScriptVideoConfig`,
 * `scripttovideo/repo`) but the private scene JSON could not be read, so the
 * three roles here are the ones the approved mockup draws: hook, benefit, CTA.
 * Timing is length-weighted (same reading-speed estimate as TTS) and replaced by
 * measured TTS durations when a voiceover is actually synthesised.
 */
enum class AdSceneRole(val label: String) {
    HOOK("Hook"),
    BENEFIT("Benefit"),
    CTA("CTA")
}

/** One script line with its slot on the ad timeline. */
data class AdScene(
    val index: Int,
    val role: AdSceneRole,
    val text: String,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** The mockup's three CTA chips. */
enum class AdCta(val label: String) {
    SHOP_NOW("Shop now"),
    LEARN_MORE("Learn more"),
    GET_OFFER("Get offer")
}

/**
 * What the Ad Maker screen collects. `music` and `voiceover` cannot both be on:
 * a draft has one audio track and the preview engine plays only the first
 * non-empty one, so mixing them would silently drop a bed. The screen turns
 * music off when the voiceover is enabled and says why.
 */
data class AdBrief(
    val script: String = DEFAULT_SCRIPT,
    val product: String = DEFAULT_PRODUCT,
    val cta: AdCta = AdCta.SHOP_NOW,
    val voiceover: Boolean = true,
    val music: Boolean = false
) {
    fun sanitized(): AdBrief = copy(
        script = script.take(AdScript.MAX_SCRIPT_CHARS),
        product = product.trim().take(AdScript.MAX_PRODUCT_CHARS),
        music = music && !voiceover
    )

    val scenes: List<AdScene> get() = AdScript.scenes(script)

    val isReady: Boolean get() = scenes.isNotEmpty()

    companion object {
        /** The approved mockup's example brief, kept verbatim so the screen matches ACC. */
        const val DEFAULT_SCRIPT = "Meet the bottle that keeps up with your night.\n" +
            "Cold for 24 hours. No plastic aftertaste.\n" +
            "Built for the city. Ready when you are."
        const val DEFAULT_PRODUCT = "Orbit Bottle · Matte Black"
    }
}

object AdScript {

    const val MAX_LINES = 6
    const val MAX_LINE_CHARS = 160
    const val MAX_SCRIPT_CHARS = 900
    const val MAX_PRODUCT_CHARS = 60

    /** A scene never gets shorter than this, even for a two-word line. */
    const val MIN_SCENE_MS = 1_200L

    /** Only applied to estimates; a measured voiceover is never truncated. */
    const val MAX_ESTIMATED_SCENE_MS = 20_000L

    /** Splits on newlines (bullets tolerated) and drops blank lines. */
    fun lines(script: String): List<String> = script
        .lineSequence()
        .map { it.trim().removePrefix("-").trim() }
        .filter { it.isNotEmpty() }
        .map { it.take(MAX_LINE_CHARS) }
        .take(MAX_LINES)
        .toList()

    fun roleFor(index: Int, count: Int): AdSceneRole = when {
        count <= 1 -> AdSceneRole.HOOK
        index <= 0 -> AdSceneRole.HOOK
        index >= count - 1 -> AdSceneRole.CTA
        else -> AdSceneRole.BENEFIT
    }

    /**
     * Lays the lines out back to back. [voiceDurationsMs] (when present and
     * positive per line) wins over the reading-speed estimate.
     */
    fun scenes(script: String, voiceDurationsMs: List<Long>? = null): List<AdScene> {
        val lines = lines(script)
        if (lines.isEmpty()) return emptyList()
        var cursor = 0L
        return lines.mapIndexed { index, line ->
            val measured = voiceDurationsMs?.getOrNull(index)?.takeIf { it > 0L }
            val duration = measured ?: TtsScript.estimateDurationMs(line)
                .coerceIn(MIN_SCENE_MS, MAX_ESTIMATED_SCENE_MS)
            val scene = AdScene(
                index = index,
                role = roleFor(index, lines.size),
                text = line,
                startMs = cursor,
                endMs = cursor + duration
            )
            cursor += duration
            scene
        }
    }

    fun totalDurationMs(scenes: List<AdScene>): Long = scenes.maxOfOrNull { it.endMs } ?: 0L
}
