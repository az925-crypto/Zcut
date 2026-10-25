package com.zaaam.zcut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.zcut.core.domain.TextAnimationFrame
import com.zaaam.zcut.core.domain.TextFonts
import com.zaaam.zcut.core.domain.TextLayout
import com.zaaam.zcut.core.domain.TextStyleResolver
import com.zaaam.zcut.core.model.TextAlignment
import com.zaaam.zcut.core.model.TextConfig

/**
 * Draws one text layer with the shared style contract. The stroke is a second
 * Text pass with the exact same metrics, so outline and fill cannot drift apart.
 * Used by both the editor overlay and the text tool preview.
 */
@Composable
fun ZcutText(
    config: TextConfig,
    frameHeightDp: Float,
    modifier: Modifier = Modifier,
    frame: TextAnimationFrame = TextAnimationFrame.Idle
) {
    if (config.text.isBlank()) return
    val resolved = remember(config) { TextStyleResolver.resolve(config) }
    val fontSizeDp = TextLayout.fontSizeDp(config, frameHeightDp)
    val strokeWidthDp = TextLayout.strokeWidthDp(resolved.strokeWidth, frameHeightDp)
    val display = if (frame.visibleChars >= config.text.length) {
        config.text
    } else {
        config.text.take(frame.visibleChars.coerceAtLeast(0))
    }
    val fontSize = fontSizeDp.sp
    val family = fontFamily(config.fontFamily)
    val weight = if (resolved.bold) FontWeight.Bold else FontWeight.Normal
    val letterSpacing = TextLayout.letterSpacingDp(config, frameHeightDp).sp
    val lineHeight = TextLayout.lineHeightDp(config, frameHeightDp).sp
    val align = config.alignment.toTextAlign()
    val shadow = if (resolved.shadow) {
        Shadow(
            color = Color.Black.copy(alpha = 0.65f),
            offset = Offset(1.5f, 1.5f),
            blurRadius = 4f
        )
    } else {
        null
    }
    val gradientEnd = resolved.gradientEndArgb
    val fillStyle = if (gradientEnd == null) {
        textStyle(
            color = Color(resolved.fillColorArgb),
            fontSize = fontSize,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            textAlign = align,
            shadow = shadow
        )
    } else {
        textStyle(
            brush = Brush.linearGradient(
                listOf(Color(resolved.fillColorArgb), Color(gradientEnd))
            ),
            fontSize = fontSize,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            textAlign = align,
            shadow = shadow
        )
    }
    val strokeStyle = if (strokeWidthDp > 0f) {
        textStyle(
            color = Color(resolved.strokeColorArgb),
            fontSize = fontSize,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            textAlign = align,
            drawStyle = Stroke(width = strokeWidthDp, join = StrokeJoin.Round)
        )
    } else {
        null
    }
    val glitchX = frame.glitchXFraction * fontSizeDp
    val glitchY = frame.glitchYFraction * fontSizeDp
    val offsetY = frame.offsetYFraction * frameHeightDp + glitchY

    Box(
        modifier = modifier
            .offset(x = glitchX.dp, y = offsetY.dp)
            .graphicsLayer {
                alpha = frame.alpha.coerceIn(0f, 1f)
                scaleX = frame.scale
                scaleY = frame.scale
            }
    ) {
        val background = config.backgroundColorArgb
        if (background != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 2.dp)
                    .background(
                        color = Color(background),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Box {
            if (strokeStyle != null) {
                Text(text = display, style = strokeStyle)
            }
            Text(text = display, style = fillStyle)
        }
    }
}

/**
 * Compose 1.7's `TextStyle.copy` exposes neither `brush` nor `drawStyle`, so the
 * two variants are built with the matching public constructor instead.
 */
private fun textStyle(
    color: Color = Color.Unspecified,
    brush: Brush? = null,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    letterSpacing: TextUnit,
    lineHeight: TextUnit,
    textAlign: TextAlign,
    shadow: Shadow? = null,
    drawStyle: DrawStyle? = null
): TextStyle = if (brush == null) {
    TextStyle(
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        textAlign = textAlign,
        shadow = shadow,
        drawStyle = drawStyle
    )
} else {
    TextStyle(
        brush = brush,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        textAlign = textAlign,
        shadow = shadow,
        drawStyle = drawStyle
    )
}

private fun fontFamily(id: String): FontFamily = when (TextFonts.canonical(id)) {
    TextFonts.SERIF -> FontFamily.Serif
    TextFonts.MONO -> FontFamily.Monospace
    TextFonts.HAND -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

private fun TextAlignment.toTextAlign(): TextAlign = when (this) {
    TextAlignment.LEFT -> TextAlign.Start
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.RIGHT -> TextAlign.End
}
