package com.zaaam.zcut.feature.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Local gradient art for template cards.
 *
 * Zcut ships no CapCut artwork, so a template's look is a two-stop gradient
 * chosen per `artKey`. Purely decorative: it claims nothing about the template.
 */
internal object TemplateArt {

    private val palettes: Map<String, Pair<Color, Color>> = mapOf(
        "night" to (Color(0xFF1B1B4B) to Color(0xFF7A2E8E)),
        "gold" to (Color(0xFF7A4A12) to Color(0xFFE8B04B)),
        "pink" to (Color(0xFF5A1740) to Color(0xFFE86AA8)),
        "green" to (Color(0xFF123A2A) to Color(0xFF4FBF7A)),
        "blue" to (Color(0xFF102B4E) to Color(0xFF4A9BE8)),
        "city" to (Color(0xFF141C24) to Color(0xFF3E7C8C)),
        "beach" to (Color(0xFF0E3A44) to Color(0xFF63CFC4)),
        "portrait" to (Color(0xFF2A1436) to Color(0xFFA26A9E)),
        "food" to (Color(0xFF3A140F) to Color(0xFFE2703A))
    )

    private val fallback = Color(0xFF20242C) to Color(0xFF4A5060)

    fun brush(artKey: String): Brush {
        val (start, end) = palettes[artKey] ?: fallback
        return Brush.linearGradient(listOf(start, end))
    }
}

/** Gradient panel used by template tiles, the detail hero and discover cards. */
@Composable
internal fun TemplateArtBox(
    artKey: String,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TemplateArt.brush(artKey))
    ) {
        overlay()
    }
}