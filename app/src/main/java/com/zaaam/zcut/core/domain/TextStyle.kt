package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.TextConfig
import com.zaaam.zcut.core.model.TextStylePreset
import java.util.Locale
import kotlin.math.max

/** Effective paint values after applying a [TextStylePreset] on top of the config. */
data class ResolvedTextStyle(
    val fillColorArgb: Long,
    val gradientEndArgb: Long? = null,
    val strokeWidth: Float = 0f,
    val strokeColorArgb: Long = 0xFF000000,
    val shadow: Boolean = false,
    val bold: Boolean = false
)

/**
 * Pure style resolution shared by the editor overlay and the M13 export
 * renderer. Legacy fields (`strokeWidth`, `shadow`) are respected when the
 * preset is [TextStylePreset.NONE], so drafts created before M11 keep their
 * look.
 */
object TextStyleResolver {

    private const val FALLBACK_GRADIENT = 0xFF59D9FF

    fun resolve(config: TextConfig): ResolvedTextStyle {
        val base = ResolvedTextStyle(
            fillColorArgb = config.colorArgb,
            strokeWidth = config.strokeWidth.coerceAtLeast(0f),
            strokeColorArgb = config.strokeColorArgb,
            shadow = config.shadow
        )
        return when (config.stylePreset) {
            TextStylePreset.NONE -> base
            TextStylePreset.BOLD -> base.copy(bold = true)
            TextStylePreset.OUTLINE -> base.copy(strokeWidth = max(base.strokeWidth, 2f))
            TextStylePreset.SHADOW -> base.copy(shadow = true)
            TextStylePreset.NEON -> base.copy(
                strokeWidth = max(base.strokeWidth, 2f),
                strokeColorArgb = config.colorArgb,
                shadow = true
            )
            TextStylePreset.GRADIENT -> base.copy(
                gradientEndArgb = config.gradientColorArgb ?: FALLBACK_GRADIENT
            )
        }
    }
}

/** System font families. No CapCut or third-party font files are bundled. */
object TextFonts {

    const val SANS = "sans"
    const val SERIF = "serif"
    const val MONO = "monospace"
    const val HAND = "cursive"

    val systemIds: List<String> = listOf(SANS, SERIF, MONO, HAND)

    fun canonical(fontFamily: String): String = when (
        fontFamily.trim().lowercase(Locale.US)
    ) {
        "display", "sans", "sans-serif", "default" -> SANS
        "serif" -> SERIF
        "mono", "monospace", "typewriter" -> MONO
        "hand", "cursive", "script" -> HAND
        else -> SANS
    }

    fun label(fontFamily: String): String = when (canonical(fontFamily)) {
        SANS -> "Display"
        SERIF -> "Serif"
        MONO -> "Mono"
        else -> "Hand"
    }
}

/**
 * Preview/export size contract. A config's `fontSizeSp` is interpreted against
 * a 260 dp reference frame, which is the height of the ACC'd text preview card.
 * Export (M13) must use the same constant so a draft does not change size.
 */
object TextLayout {

    const val REFERENCE_FRAME_HEIGHT_DP = 260f

    fun scale(frameHeightDp: Float): Float {
        if (!frameHeightDp.isFinite() || frameHeightDp <= 0f) return 1f
        return (frameHeightDp / REFERENCE_FRAME_HEIGHT_DP).coerceIn(0.2f, 6f)
    }

    fun fontSizeDp(config: TextConfig, frameHeightDp: Float): Float {
        val size = if (config.fontSizeSp.isFinite()) config.fontSizeSp else 42f
        return (size.coerceIn(8f, 200f) * scale(frameHeightDp)).coerceAtLeast(1f)
    }

    fun letterSpacingDp(config: TextConfig, frameHeightDp: Float): Float {
        val spacing = if (config.letterSpacingEm.isFinite()) config.letterSpacingEm else 0f
        return (spacing.coerceIn(-0.05f, 1f) * fontSizeDp(config, frameHeightDp))
    }

    fun lineHeightDp(config: TextConfig, frameHeightDp: Float): Float {
        val multiplier = if (config.lineHeight.isFinite()) config.lineHeight else 1.2f
        return fontSizeDp(config, frameHeightDp) * multiplier.coerceIn(0.8f, 3f)
    }

    fun strokeWidthDp(strokeWidth: Float, frameHeightDp: Float): Float =
        (if (strokeWidth.isFinite()) strokeWidth.coerceIn(0f, 20f) else 0f) *
            scale(frameHeightDp)
}
