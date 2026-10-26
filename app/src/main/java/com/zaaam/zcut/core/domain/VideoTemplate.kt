package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AspectRatio

/**
 * Categories the approved Templates mockup exposes as chips.
 *
 * [FOR_YOU] is the curated shelf (every local pack), the rest filter by a
 * template's own category.
 */
enum class TemplateCategory(val label: String) {
    FOR_YOU("For you"),
    TRENDING("Trending"),
    VLOG("Vlog"),
    PHOTO("Photo"),
    BUSINESS("Business")
}

/**
 * One media slot in a template: how long the clip that fills it should be.
 *
 * The artifact's cut-same packages carry slot definitions somewhere inside
 * `template.json`/`config.json`, but the inspected `split_screen_*.zip` samples
 * contain no explicit slot list (`docs/features/21-templates-cutsame.md` marks
 * the format Unknown), so this is Zcut's own minimal slot shape: a fixed
 * duration per slot, centre-cropped on apply.
 */
data class TemplateSlot(val durationMs: Long)

/**
 * A local template: a half-finished draft plus the slots a user's media fills.
 * Everything here is authored for Zcut — no CapCut package, asset or branding is
 * used, and the description/values are Zcut's own (the artifact exposes no
 * readable template metadata).
 */
data class VideoTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    /** Key into the local gradient art shown by the templates screens. */
    val artKey: String,
    val ratio: AspectRatio,
    val slots: List<TemplateSlot>,
    /** Tempo the beat-synced cuts follow; null means the pack has no markers. */
    val bpm: Int? = null,
    val captionStyle: Boolean = true,
    val description: String = "",
    val frameRate: Int = 30
) {
    val slotCount: Int get() = slots.size

    val durationMs: Long get() = slots.sumOf { it.durationMs }

    val durationLabel: String get() = "${durationMs / 1_000}s"

    /** `6 slots · 12s`, the copy the mockup renders on every tile. */
    val slotSummary: String get() = "$slotCount slots · $durationLabel"

    /** Timeline start of every slot, cumulative from zero. */
    fun slotStartsMs(): List<Long> {
        var time = 0L
        return slots.map { slot ->
            val start = time
            time += slot.durationMs
            start
        }
    }
}

/**
 * The built-in packs. Zcut ships these inside the APK: no download, no
 * marketplace, no license check, and no CapCut asset is included.
 */
object TemplateCatalog {

    val all: List<VideoTemplate> = listOf(
        VideoTemplate(
            id = "neon-nights",
            name = "Neon Nights",
            category = TemplateCategory.FOR_YOU,
            artKey = "night",
            ratio = AspectRatio.PORTRAIT_9_16,
            slots = List(6) { TemplateSlot(2_000L) },
            bpm = 128,
            description = "A high-contrast night template with beat-synced cuts, " +
                "glow titles, and a local caption style. Replace each slot with " +
                "your own clips."
        ),
        VideoTemplate(
            id = "golden-hour",
            name = "Golden Hour",
            category = TemplateCategory.FOR_YOU,
            artKey = "gold",
            ratio = AspectRatio.PORTRAIT_9_16,
            slots = listOf(4_000L, 4_000L, 3_500L, 3_500L).map(::TemplateSlot),
            bpm = 90,
            description = "Warm four-beat pacing for travel and portraits. Long " +
                "holds, soft captions, no watermark."
        ),
        VideoTemplate(
            id = "soft-pop",
            name = "Soft Pop",
            category = TemplateCategory.FOR_YOU,
            artKey = "pink",
            ratio = AspectRatio.SQUARE_1_1,
            slots = List(8) { TemplateSlot(2_500L) },
            bpm = 112,
            description = "Square social cut with quick eight-slot rhythm and a " +
                "bright caption style."
        ),
        VideoTemplate(
            id = "match-cut",
            name = "Match Cut",
            category = TemplateCategory.FOR_YOU,
            artKey = "green",
            ratio = AspectRatio.LANDSCAPE_16_9,
            slots = List(5) { TemplateSlot(2_000L) },
            bpm = 120,
            description = "Landscape match-cut skeleton: five equal beats for " +
                "shape-to-shape transitions."
        ),
        VideoTemplate(
            id = "clean-product",
            name = "Clean Product",
            category = TemplateCategory.FOR_YOU,
            artKey = "blue",
            ratio = AspectRatio.SQUARE_1_1,
            slots = listOf(3_000L, 2_500L, 2_500L).map(::TemplateSlot),
            bpm = 100,
            captionStyle = false,
            description = "Three-slot product skeleton with a clean hold, a detail " +
                "beat and a closing beat. Captions stay off."
        ),
        VideoTemplate(
            id = "city-walk",
            name = "City Walk",
            category = TemplateCategory.FOR_YOU,
            artKey = "city",
            ratio = AspectRatio.LANDSCAPE_16_9,
            slots = listOf(3_000L, 2_500L, 2_500L, 2_500L, 2_500L, 2_000L, 3_000L)
                .map(::TemplateSlot),
            bpm = 96,
            description = "Seven-slot walk-through with a mixed rhythm and beat " +
                "markers from the pack's own tempo."
        ),
        VideoTemplate(
            id = "beat-drop",
            name = "Beat Drop",
            category = TemplateCategory.TRENDING,
            artKey = "pink",
            ratio = AspectRatio.PORTRAIT_9_16,
            slots = List(6) { TemplateSlot(1_500L) },
            bpm = 140,
            description = "Fast six-slot drop for trend edits: 1.5 s per slot at " +
                "140 BPM."
        ),
        VideoTemplate(
            id = "slow-diary",
            name = "Slow Diary",
            category = TemplateCategory.VLOG,
            artKey = "portrait",
            ratio = AspectRatio.PORTRAIT_9_16,
            slots = listOf(3_500L, 3_500L, 3_500L, 3_500L, 3_000L).map(::TemplateSlot),
            bpm = 84,
            description = "Vlog diary pacing: long holds with a soft close, built " +
                "for a voiceover on top."
        ),
        VideoTemplate(
            id = "photo-grid",
            name = "Photo Grid",
            category = TemplateCategory.PHOTO,
            artKey = "beach",
            ratio = AspectRatio.SQUARE_1_1,
            slots = List(4) { TemplateSlot(2_500L) },
            bpm = null,
            captionStyle = false,
            description = "Four stills at 2.5 s each. Stills keep their own " +
                "duration when it is shorter than the slot."
        ),
        VideoTemplate(
            id = "product-promo",
            name = "Product Promo",
            category = TemplateCategory.BUSINESS,
            artKey = "food",
            ratio = AspectRatio.LANDSCAPE_16_9,
            slots = listOf(2_000L, 2_000L, 3_000L, 3_000L).map(::TemplateSlot),
            bpm = 110,
            description = "Business skeleton: hook, detail, proof, call to action " +
                "in four slots."
        )
    )

    private val index: Map<String, VideoTemplate> = all.associateBy { it.id }

    fun byId(id: String?): VideoTemplate? = id?.let(index::get)

    /** [FOR_YOU] shows the whole curated shelf; other categories filter. */
    fun inCategory(category: TemplateCategory): List<VideoTemplate> =
        if (category == TemplateCategory.FOR_YOU) all
        else all.filter { it.category == category }

    val categories: List<TemplateCategory> = TemplateCategory.entries
}

/** One card in the locally-seeded Discover feed. */
data class DiscoverPost(
    val id: String,
    val title: String,
    val author: String,
    val artKey: String,
    val tag: String,
    val templateId: String
) {
    val template: VideoTemplate? get() = TemplateCatalog.byId(templateId)
}

/**
 * Discover is a local, offline showcase: every card points at a built-in
 * template. Nothing is fetched, published or uploaded — community publishing is
 * marked BUTUH BACKEND in the UI.
 */
object DiscoverFeed {

    val tags: List<String> = listOf("For you", "Minimal", "Cinematic", "Tutorial")

    val posts: List<DiscoverPost> = listOf(
        DiscoverPost("sunset-loop", "Sunset loop", "@maya", "beach", "Cinematic", "photo-grid"),
        DiscoverPost("soft-focus", "Soft focus", "@ren", "portrait", "Minimal", "slow-diary"),
        DiscoverPost("match-cut", "Match cut", "@studio", "green", "Tutorial", "match-cut"),
        DiscoverPost("golden-hour", "Golden hour", "@nina", "gold", "Cinematic", "golden-hour")
    )

    fun inTag(tag: String): List<DiscoverPost> =
        if (tag == tags.first()) posts else posts.filter { it.tag == tag }
}