package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeLayoutTest {

    @Test
    fun everyLayoutFillsTheCanvasWithoutOverlap() {
        SplitLayout.entries.forEach { layout ->
            val rects = SplitGeometry.cellRects(layout, 1_200, 1_200, gapPx = 24)

            assertEquals(layout.cells, rects.size)
            rects.forEach { rect ->
                assertTrue(rect.left >= 0)
                assertTrue(rect.top >= 0)
                assertTrue(rect.right <= 1_200)
                assertTrue(rect.bottom <= 1_200)
                assertTrue(rect.width > 0)
                assertTrue(rect.height > 0)
            }
            for (first in rects.indices) {
                for (second in first + 1 until rects.size) {
                    val a = rects[first]
                    val b = rects[second]
                    val overlaps = a.left < b.right && b.left < a.right &&
                        a.top < b.bottom && b.top < a.bottom
                    assertFalse("${layout.label}: cell $first overlaps $second", overlaps)
                }
            }
        }
    }

    @Test
    fun centerCropKeepsTheTargetAspectAndCentresTheWindow() {
        val wide = SplitGeometry.centerCropRect(2_000, 1_000, 500, 500)

        assertEquals(1_000, wide.width)
        assertEquals(1_000, wide.height)
        assertEquals(500, wide.left)
        assertEquals(0, wide.top)

        val tall = SplitGeometry.centerCropRect(1_000, 2_000, 500, 500)

        assertEquals(1_000, tall.width)
        assertEquals(1_000, tall.height)
        assertEquals(0, tall.left)
        assertEquals(500, tall.top)
    }

    @Test
    fun configSanitizersClampTheSliders() {
        val split = SplitScreenConfig(borderPercent = 99, cornerPercent = -4).sanitize()

        assertEquals(SplitScreenConfig.MAX_BORDER_PERCENT, split.borderPercent)
        assertEquals(0, split.cornerPercent)
        assertEquals(0xFF000000L, split.backgroundArgb and 0xFF000000L)

        val gameplay = GameplayConfig(sizePercent = 99, borderPercent = -1).sanitize()

        assertEquals(GameplayConfig.MAX_SIZE_PERCENT, gameplay.sizePercent)
        assertEquals(0, gameplay.borderPercent)
    }

    @Test
    fun facecamStaysInsideEveryCorner() {
        FacecamPosition.entries.forEach { position ->
            val rect = GameplayGeometry.facecamRect(
                GameplayConfig(position = position, sizePercent = 60).sanitize(),
                width = 1_080,
                height = 1_920
            )

            assertTrue(rect.left >= 0)
            assertTrue(rect.top >= 0)
            assertTrue(rect.right <= 1_080)
            assertTrue(rect.bottom <= 1_920)
            assertTrue(rect.width > 0)
            assertEquals(rect.width, rect.height)
        }
    }

    @Test
    fun livePhotoOnlyExportsVideo() {
        val gif = LivePhotoConfig(durationMs = 50, export = LivePhotoExport.GIF).sanitize()

        assertEquals(LivePhotoConfig.MIN_DURATION_MS, gif.durationMs)
        assertEquals(LivePhotoExport.VIDEO, gif.export)

        val long = LivePhotoConfig(durationMs = 60_000, motionStrengthPercent = 300).sanitize()

        assertEquals(LivePhotoConfig.MAX_DURATION_MS, long.durationMs)
        assertEquals(100, long.motionStrengthPercent)
    }
}
