package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio
import com.zaaam.zcut.core.model.MediaAsset
import kotlinx.serialization.Serializable

/**
 * Request shape for the generation screen (F24 parity).
 *
 * The artifact's pipeline (`AigcGenerateClient`, `AigcWorkflowClient`,
 * `AIEffectRemoteTaskParamBuildNode`, ...) always ends in a remote task, so
 * nothing here ever runs a model. This type only describes what *would* be sent,
 * which keeps the UI honest and lets the history record what the user asked for.
 */
@Serializable
enum class GenerationType(val label: String) {
    IMAGE("Image"),
    VIDEO("Video")
}

/** The mockup's four style chips. Their model mapping is Unknown, so they are labels only. */
@Serializable
enum class GenerationStyle(val label: String) {
    CINEMATIC("Cinematic"),
    ANIME("Anime"),
    PRODUCT("Product"),
    DOCUMENTARY("Documentary")
}

data class GenerationRequest(
    val type: GenerationType = GenerationType.IMAGE,
    val prompt: String = "",
    val negativePrompt: String = "",
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val style: GenerationStyle = GenerationStyle.CINEMATIC,
    /** Only meaningful for [GenerationType.VIDEO]. */
    val durationMs: Long = DEFAULT_VIDEO_DURATION_MS,
    val seed: Long = 0L
) {

    val isReady: Boolean get() = prompt.isNotBlank()

    /** Trims and clamps every field so a backend request can never carry junk. */
    fun sanitized(): GenerationRequest = copy(
        prompt = prompt.trim().take(MAX_PROMPT),
        negativePrompt = negativePrompt.trim().take(MAX_NEGATIVE),
        durationMs = durationMs.coerceIn(MIN_VIDEO_DURATION_MS, MAX_VIDEO_DURATION_MS),
        seed = seed.coerceAtLeast(0L)
    )

    /**
     * A stable seed derived from the prompt, so the same brief keeps the same
     * seed across retries. Pure and platform-free, hence testable.
     */
    fun derivedSeed(): Long {
        var hash = 1_125_899_906_842_597L
        prompt.forEach { char -> hash = 31L * hash + char.code }
        return hash and Long.MAX_VALUE
    }

    companion object {
        const val MAX_PROMPT = 1_000
        const val MAX_NEGATIVE = 400
        const val MIN_VIDEO_DURATION_MS = 1_000L
        const val MAX_VIDEO_DURATION_MS = 10_000L
        const val DEFAULT_VIDEO_DURATION_MS = 5_000L

        /** What the mockup's pre-filled textarea represents: a prompt only. */
        fun fromPrompt(
            prompt: String,
            type: GenerationType = GenerationType.IMAGE,
            style: GenerationStyle = GenerationStyle.CINEMATIC,
            aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16
        ): GenerationRequest = GenerationRequest(
            type = type,
            prompt = prompt,
            style = style,
            aspectRatio = aspectRatio
        ).sanitized().let { it.copy(seed = it.derivedSeed()) }
    }
}

/**
 * One locally stored result. Generation itself is backend-only, but a result the
 * user imported from the gallery is a real file and is remembered like any other
 * asset, together with the brief it was imported for.
 */
@Serializable
data class GenerationEntry(
    val id: String,
    val prompt: String,
    val type: GenerationType,
    val style: GenerationStyle,
    val aspectRatio: AspectRatio,
    val asset: MediaAsset,
    val createdAtMs: Long,
    /** The deterministic seed the brief maps to; kept so a retry can reuse it. */
    val seed: Long = 0L
) {
    companion object {
        /** Old entries are pruned so a corrupt store cannot grow without bound. */
        const val MAX_ENTRIES = 60
    }
}

@Serializable
data class GenerationDocument(val entries: List<GenerationEntry> = emptyList())

/** Pure list operations behind the history store, kept out of the Android layer. */
object GenerationHistory {

    /** Newest first, de-duplicated by id, capped. */
    fun add(
        entries: List<GenerationEntry>,
        entry: GenerationEntry,
        max: Int = GenerationEntry.MAX_ENTRIES
    ): List<GenerationEntry> {
        if (max <= 0) return emptyList()
        val clean = sanitize(entries, max)
        return (listOf(entry) + clean.filterNot { it.id == entry.id })
            .take(max)
    }

    /**
     * Drops entries that cannot be opened (no id, no usable asset) and clamps
     * text so a hostile payload cannot blow up the list. Unknown entries are
     * dropped rather than rendered as broken tiles; a blank prompt is allowed
     * because the display name still identifies the result.
     */
    fun sanitize(
        entries: List<GenerationEntry>,
        max: Int = GenerationEntry.MAX_ENTRIES
    ): List<GenerationEntry> {
        if (max <= 0) return emptyList()
        return entries
            .asSequence()
            .filter { it.id.isNotBlank() && it.asset.isUsable }
            .map { entry ->
                entry.copy(
                    prompt = entry.prompt.trim().take(GenerationRequest.MAX_PROMPT),
                    createdAtMs = entry.createdAtMs.coerceAtLeast(0L),
                    seed = entry.seed.coerceAtLeast(0L)
                )
            }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMs }
            .take(max)
            .toList()
    }
}
