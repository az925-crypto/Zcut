package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.ColorAdjustments
import com.zaaam.zcut.core.model.MattingBackground
import com.zaaam.zcut.core.model.MattingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditTest {

    @Test
    fun cropWindowKeepsAMinimumSizeAndStaysInsideTheImage() {
        val collapsed = CropWindow(left = 0.5f, top = 0.5f, right = 0.5f, bottom = 0.5f)
            .sanitized()

        assertTrue(collapsed.widthRatio >= PhotoEditLimits.MIN_CROP - 1e-6f)
        assertTrue(collapsed.heightRatio >= PhotoEditLimits.MIN_CROP - 1e-6f)
        assertTrue(collapsed.right <= 1f)
        assertTrue(collapsed.bottom <= 1f)

        val outside = CropWindow(left = -3f, top = -3f, right = 9f, bottom = 9f).sanitized()

        assertEquals(0f, outside.left, 1e-6f)
        assertEquals(0f, outside.top, 1e-6f)
        assertEquals(1f, outside.right, 1e-6f)
        assertEquals(1f, outside.bottom, 1e-6f)
    }

    @Test
    fun centeredCropUsesTheTargetRatio() {
        // Wide source (2:1) toward 1:1: full height, half width, centred.
        val square = CropWindow.centered(sourceRatio = 2f, targetRatio = 1f)

        assertEquals(0.5f, square.widthRatio, 1e-5f)
        assertEquals(1f, square.heightRatio, 1e-5f)
        assertEquals(0.25f, square.left, 1e-5f)

        // Tall source (0.5) toward 1:1: full width, half height, centred.
        val tall = CropWindow.centered(sourceRatio = 0.5f, targetRatio = 1f)

        assertEquals(1f, tall.widthRatio, 1e-5f)
        assertEquals(0.5f, tall.heightRatio, 1e-5f)
        assertEquals(0.25f, tall.top, 1e-5f)
    }

    @Test
    fun centeredCropRejectsNonsenseRatios() {
        assertEquals(CropWindow(), CropWindow.centered(0f, 1f))
        assertEquals(CropWindow(), CropWindow.centered(1f, 0f))
    }

    @Test
    fun stateIsNeutralUntilSomethingChanges() {
        assertFalse(PhotoEditState().isEdited)

        assertTrue(PhotoEditState(quarterTurns = 1).isEdited)
        assertTrue(PhotoEditState(flipHorizontal = true).isEdited)
        assertTrue(PhotoEditState(filterId = "tokyo").isEdited)
        assertTrue(PhotoEditState(color = ColorAdjustments(brightness = 0.2f)).isEdited)
        assertTrue(PhotoEditState(stickerId = "trend-lol").isEdited)
        assertTrue(
            PhotoEditState(
                brush = listOf(BrushStroke(listOf(PhotoPoint(0f, 0f), PhotoPoint(1f, 1f))))
            ).isEdited
        )
        assertTrue(
            PhotoEditState(matting = MattingConfig(background = MattingBackground.BLUR)).isEdited
        )
    }

    @Test
    fun sanitizeWrapsTurnsAndClampsAdjustments() {
        val state = PhotoEditState(
            quarterTurns = -1,
            crop = CropWindow(left = 0.2f, top = 0.2f, right = 0.9f, bottom = 0.9f),
            filterIntensity = 5f,
            color = ColorAdjustments(
                brightness = 9f,
                contrast = -9f,
                vignette = -4f,
                hslBands = listOf(com.zaaam.zcut.core.model.HslBand(hue = 725f, saturation = 4f))
            )
        ).sanitized()

        assertEquals(3, state.quarterTurns)
        assertEquals(1f, state.filterIntensity, 1e-6f)
        assertEquals(1f, state.color.brightness, 1e-6f)
        assertEquals(-1f, state.color.contrast, 1e-6f)
        assertEquals(0f, state.color.vignette, 1e-6f)
        assertEquals(5f, state.color.hslBands.single().hue, 1e-3f)
        assertEquals(1f, state.color.hslBands.single().saturation, 1e-6f)
    }

    @Test
    fun sanitizeResolvesStickerAndCapsBrush() {
        val strokes = (0 until PhotoEditLimits.MAX_STROKES + 10).map { index ->
            BrushStroke(
                points = listOf(
                    PhotoPoint(index.toFloat(), -5f),
                    PhotoPoint(2f, 20f)
                ),
                widthFraction = 5f,
                opacity = -1f
            )
        }
        val state = PhotoEditState(stickerId = "not-a-sticker", brush = strokes).sanitized()

        assertNotNull(state.stickerId)
        assertEquals(StickerCatalog.canonical("not-a-sticker").id, state.stickerId)
        assertEquals(PhotoEditLimits.MAX_STROKES, state.brush.size)
        assertEquals(0.25f, state.brush.first().widthFraction, 1e-6f)
        assertEquals(0.05f, state.brush.first().opacity, 1e-6f)
        assertEquals(0f, state.brush.first().points.first().y, 1e-6f)
        assertEquals(1f, state.brush.first().points.last().y, 1e-6f)
    }

    @Test
    fun aStrokeNeedsTwoPointsToDraw() {
        assertFalse(BrushStroke(listOf(PhotoPoint(0.5f, 0.5f))).isDrawable)
        assertTrue(
            BrushStroke(listOf(PhotoPoint(0.5f, 0.5f), PhotoPoint(0.6f, 0.6f))).isDrawable
        )
    }

    @Test
    fun aspectPresetsRewriteTheCropWindow() {
        val fromWide = PhotoEditState().withAspect(PhotoAspect.SQUARE, sourceRatio = 2f)

        assertEquals(PhotoAspect.SQUARE, fromWide.aspect)
        assertEquals(0.5f, fromWide.crop.widthRatio, 1e-5f)

        val original = fromWide.withAspect(PhotoAspect.ORIGINAL, sourceRatio = 2f)

        assertEquals(CropWindow(), original.crop)
    }

    @Test
    fun exportFormatsCarryTheirMimeAndExtension() {
        assertEquals("image/jpeg", PhotoExportFormat.JPG.mimeType)
        assertEquals("jpg", PhotoExportFormat.JPG.extension)
        assertTrue(PhotoExportFormat.PNG.lossless)
        assertFalse(PhotoExportFormat.JPG.lossless)
        assertEquals("image/webp", PhotoExportFormat.WEBP.mimeType)
    }
}