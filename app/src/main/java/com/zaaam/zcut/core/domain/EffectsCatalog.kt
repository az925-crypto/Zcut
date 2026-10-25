package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.EffectConfig

/**
 * Local effect catalog. Each entry is a Zcut shader/matrix definition — none of
 * them is a CapCut effect package. Effects that genuinely need face/AR models
 * say so through [Effect.backendRequired] and are still selectable: the preview
 * falls back to the local colour approximation instead of hiding them.
 *
 * ponytail: colour effects are 4×5 matrices so the preview is exact; spatial
 * effects (blur, pixelation) carry a radius and are approximated in the preview
 * until the export pipeline (M19) adds real shaders.
 */
object EffectsCatalog {

    enum class Category(val label: String) {
        TRENDING("Trending"),
        RETRO("Retro"),
        LIGHT("Light"),
        AR("AR"),
        DISTORT("Distort")
    }

    data class Effect(
        val id: String,
        val label: String,
        val category: Category,
        val description: String,
        val backendRequired: Boolean = false,
        val matrix: FloatArray? = null,
        val blurRadius: Float = 0f
    )

    val all: List<Effect> = listOf(
        Effect(
            id = "glow",
            label = "Glow",
            category = Category.TRENDING,
            description = "glow lembut",
            matrix = ColorPipeline.pipe(
                ColorPipeline.brightnessScale(1.1f),
                ColorPipeline.saturation(1.2f)
            )
        ),
        Effect(
            id = "vhs",
            label = "VHS",
            category = Category.TRENDING,
            description = "kaset retro",
            matrix = ColorPipeline.pipe(
                ColorPipeline.contrast(1.2f),
                ColorPipeline.sepia(0.2f)
            )
        ),
        Effect(
            id = "chromatic",
            label = "Chromatic",
            category = Category.DISTORT,
            description = "aberasi warna",
            matrix = ColorPipeline.pipe(
                ColorPipeline.hueRotation(20f),
                ColorPipeline.saturation(1.5f)
            )
        ),
        Effect(
            id = "dream",
            label = "Dream",
            category = Category.TRENDING,
            description = "blur halus",
            matrix = ColorPipeline.pipe(
                ColorPipeline.brightnessScale(1.12f),
                ColorPipeline.saturation(1.05f)
            ),
            blurRadius = 6f
        ),
        Effect(
            id = "pixel",
            label = "Pixel",
            category = Category.DISTORT,
            description = "blok piksel",
            matrix = ColorPipeline.contrast(1.4f),
            blurRadius = 0f
        ),
        Effect(
            id = "ar-mask",
            label = "AR Mask",
            category = Category.AR,
            description = "warna wajah",
            backendRequired = true,
            matrix = ColorPipeline.saturation(1.3f)
        ),
        Effect(
            id = "light-leak",
            label = "Light leak",
            category = Category.LIGHT,
            description = "kebocoran cahaya",
            matrix = ColorPipeline.pipe(
                ColorPipeline.brightnessScale(1.15f),
                ColorPipeline.sepia(0.12f)
            )
        ),
        Effect(
            id = "grain",
            label = "Grain",
            category = Category.RETRO,
            description = "butiran film",
            matrix = ColorPipeline.contrast(1.1f)
        ),
        Effect(
            id = "prism",
            label = "Prism",
            category = Category.LIGHT,
            description = "geser spektrum",
            matrix = ColorPipeline.pipe(
                ColorPipeline.hueRotation(-20f),
                ColorPipeline.saturation(1.1f)
            )
        )
    )

    fun byCategory(category: Category): List<Effect> = all.filter { it.category == category }

    fun find(id: String?): Effect? = all.firstOrNull { it.id == id }

    /** Colour matrix of one effect scaled by its intensity. */
    fun previewMatrix(effect: Effect?, intensity: Float): FloatArray {
        val matrix = effect?.matrix ?: return ColorPipeline.identity()
        return ColorPipeline.blend(
            ColorPipeline.identity(),
            matrix,
            intensity.coerceIn(0f, 1f)
        )
    }

    /** Stacks every active effect in list order; identity when none is active. */
    fun previewMatrix(effects: List<EffectConfig>): FloatArray {
        val steps = effects.mapNotNull { config ->
            find(config.id)?.let { effect -> previewMatrix(effect, config.intensity) }
        }
        return if (steps.isEmpty()) ColorPipeline.identity() else ColorPipeline.pipe(*steps.toTypedArray())
    }

    fun blurRadius(effects: List<EffectConfig>): Float =
        effects.maxOfOrNull { config ->
            (find(config.id)?.blurRadius ?: 0f) * config.intensity.coerceIn(0f, 1f)
        } ?: 0f
}
