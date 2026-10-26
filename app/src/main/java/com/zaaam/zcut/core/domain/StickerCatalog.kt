package com.zaaam.zcut.core.domain

/**
 * The app's own sticker set.
 *
 * Every sticker is a glyph (a Unicode shape or a short text badge) drawn by
 * Compose, so nothing is copied from the original app's sticker packs. The
 * categories mirror the chips in the approved sticker mockup (Trending, Emoji,
 * Text, Shapes, GIF).
 */
enum class StickerCategory {
    TRENDING,
    EMOJI,
    TEXT,
    SHAPES,
    GIF
}

/** One sticker: a stable key plus the glyph the UI paints. */
data class StickerSpec(
    val id: String,
    val glyph: String,
    val label: String,
    val category: StickerCategory
)

object StickerCatalog {

    /**
     * Glyphs only — no bitmaps, no bundling of anyone else's artwork. A "GIF"
     * entry is a text badge because animation import is out of scope; the honest
     * label is part of the entry.
     */
    val all: List<StickerSpec> = listOf(
        StickerSpec("trend-lol", "LOL", "LOL", StickerCategory.TRENDING),
        StickerSpec("trend-100", "100", "100", StickerCategory.TRENDING),
        StickerSpec("trend-fire", "\uD83D\uDD25", "Fire", StickerCategory.TRENDING),
        StickerSpec("trend-spark", "\u2726", "Spark", StickerCategory.TRENDING),
        StickerSpec("emoji-smile", "\uD83D\uDE04", "Smile", StickerCategory.EMOJI),
        StickerSpec("emoji-heart", "\u2764", "Heart", StickerCategory.EMOJI),
        StickerSpec("emoji-star", "\u2B50", "Star", StickerCategory.EMOJI),
        StickerSpec("emoji-cool", "\uD83D\uDE0E", "Cool", StickerCategory.EMOJI),
        StickerSpec("text-wow", "WOW!", "WOW", StickerCategory.TEXT),
        StickerSpec("text-yes", "YES", "YES", StickerCategory.TEXT),
        StickerSpec("text-new", "NEW", "NEW", StickerCategory.TEXT),
        StickerSpec("text-bang", "!!", "Bang", StickerCategory.TEXT),
        StickerSpec("shape-triangle", "\u25B2", "Triangle", StickerCategory.SHAPES),
        StickerSpec("shape-circle", "\u25CE", "Circle", StickerCategory.SHAPES),
        StickerSpec("shape-ring", "\u25CB", "Ring", StickerCategory.SHAPES),
        StickerSpec("shape-square", "\u25A0", "Square", StickerCategory.SHAPES),
        StickerSpec("gif-boing", "BOING", "Boing (statis)", StickerCategory.GIF),
        StickerSpec("gif-loop", "LOOP", "Loop (statis)", StickerCategory.GIF)
    )

    val categories: List<StickerCategory> = StickerCategory.entries

    fun byCategory(category: StickerCategory): List<StickerSpec> =
        all.filter { it.category == category }

    fun find(id: String): StickerSpec? = all.firstOrNull { it.id == id }

    /**
     * Never returns null: a draft that names an unknown sticker still shows
     * something rather than drawing an invisible overlay.
     */
    fun canonical(id: String): StickerSpec = find(id) ?: all.first()
}