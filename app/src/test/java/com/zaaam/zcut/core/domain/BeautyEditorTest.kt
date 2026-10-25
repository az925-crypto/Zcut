package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.BeautyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the contract between the approved retouch mockup and the shipped
 * local-beauty config (`assets/local_beauty_all.json` →
 * `beauty_all_info.category[].items[]`). Keys and ranges are asserted against the
 * evidence, so a rename in the artifact would show up here instead of quietly
 * changing what a saved draft means.
 */
class BeautyEditorTest {

    @Test
    fun everySliderUsesTheShippedKeyAndRange() {
        val expected = mapOf(
            BeautySlider.SMOOTH to Triple("auto_buffing", 0f, 0.8f),
            BeautySlider.WHITEN to Triple("auto_balance_skin", 0f, 0.8f),
            BeautySlider.FACE_SLIM to Triple("thin_face", 0.5f, 0.8f),
            BeautySlider.EYES to Triple("eye_size", 0.5f, 0.8f),
            BeautySlider.NOSE to Triple("nose_wing", 0.5f, 0.6f),
            BeautySlider.LIPS to Triple("type_lips", 0f, 1f)
        )

        assertEquals(6, BeautyEditor.FACE_SLIDERS.size)
        BeautyEditor.FACE_SLIDERS.forEach { slider ->
            val (key, min, max) = expected.getValue(slider)
            assertEquals(key, slider.key)
            assertEquals(min, slider.relativeMin, 0.0001f)
            assertEquals(max, slider.relativeMax, 0.0001f)
        }
        // The six mockup rows must not share a key.
        assertEquals(6, BeautyEditor.FACE_SLIDERS.map { it.key }.toSet().size)
    }

    @Test
    fun autoBeautyAndMakeupSendTheSliderPosition() {
        // Shipped config stores `_ui_value: 40` with `_sdk_value: 0.4`.
        assertEquals(0.4f, BeautySlider.SMOOTH.toSdk(40f), 0.0001f)
        // Capped by the shipped maximum of the key.
        assertEquals(0.8f, BeautySlider.SMOOTH.toSdk(100f), 0.0001f)
        assertEquals(0.7f, BeautySlider.LIPS.toSdk(70f), 0.0001f)
        assertEquals(1f, BeautySlider.LIPS.toSdk(140f), 0.0001f)
        assertEquals(0f, BeautySlider.LIPS.toSdk(-5f), 0.0001f)
    }

    @Test
    fun shapeSlidersInterpolateFromTheNeutralBaseline() {
        // 0 is "no reshape": the shipped range starts at the neutral baseline.
        assertEquals(0.5f, BeautySlider.FACE_SLIM.toSdk(0f), 0.0001f)
        assertEquals(0.5f, BeautySlider.NOSE.toSdk(0f), 0.0001f)
        // 100 is the shipped maximum.
        assertEquals(0.8f, BeautySlider.FACE_SLIM.toSdk(100f), 0.0001f)
        assertEquals(0.6f, BeautySlider.NOSE.toSdk(100f), 0.0001f)
        // Halfway is halfway across the shipped span, for the narrower key too.
        assertEquals(0.65f, BeautySlider.FACE_SLIM.toSdk(50f), 0.0001f)
        assertEquals(0.55f, BeautySlider.NOSE.toSdk(50f), 0.0001f)
    }

    @Test
    fun neutralBaselineIsDerivedFromTheShippedRangeNotHardcoded() {
        assertFalse(BeautySlider.SMOOTH.neutralBaseline)
        assertFalse(BeautySlider.LIPS.neutralBaseline)
        assertTrue(BeautySlider.FACE_SLIM.neutralBaseline)
        assertTrue(BeautySlider.EYES.neutralBaseline)
        assertTrue(BeautySlider.NOSE.neutralBaseline)
    }

    @Test
    fun strengthIsTheSliderFractionUsedByThePixelPasses() {
        assertEquals(0.4f, BeautySlider.SMOOTH.strength(40f), 0.0001f)
        assertEquals(1f, BeautySlider.SMOOTH.strength(100f), 0.0001f)
        assertEquals(0f, BeautySlider.SMOOTH.strength(0f), 0.0001f)
        // A shape slider at its neutral 0 position does nothing at all.
        assertEquals(0f, BeautySlider.FACE_SLIM.strength(0f), 0.0001f)
    }

    @Test
    fun normalizeClampsAnUntrustedConfig() {
        val hostile = BeautyConfig(
            smooth = 250f,
            whiten = -30f,
            faceSlim = Float.NaN,
            eyes = Float.POSITIVE_INFINITY,
            nose = 50f,
            lips = 150f
        )

        val safe = BeautyEditor.normalize(hostile)

        assertEquals(100f, safe.smooth, 0.0001f)
        assertEquals(0f, safe.whiten, 0.0001f)
        assertEquals(0f, safe.faceSlim, 0.0001f)
        // Infinity is not NaN; it clamps to the top of the range.
        assertEquals(100f, safe.eyes, 0.0001f)
        assertEquals(50f, safe.nose, 0.0001f)
        assertEquals(100f, safe.lips, 0.0001f)
        // The switch is not a slider and must survive untouched.
        assertTrue(safe.preserveTexture)
    }

    @Test
    fun normalizeLeavesAnHonestConfigAlone() {
        val honest = BeautyConfig(smooth = 12f, nose = 88f, preserveTexture = false)

        assertEquals(honest, BeautyEditor.normalize(honest))
    }

    @Test
    fun activeCountOnlyCountsMovedSliders() {
        assertFalse(BeautyEditor.isActive(BeautyConfig()))
        assertEquals(0, BeautyEditor.activeCount(BeautyConfig()))

        // A shape slider left at its neutral baseline is still a slider the user
        // moved, so it counts; zero means untouched everywhere.
        assertEquals(1, BeautyEditor.activeCount(BeautyConfig(faceSlim = 100f)))
        assertEquals(2, BeautyEditor.activeCount(BeautyConfig(smooth = 10f, lips = 5f)))
    }

    @Test
    fun preserveTextureSoftensTheSmoothingBlend() {
        val full = BeautyEditor.smoothMix(1f, preserveTexture = false)
        val preserved = BeautyEditor.smoothMix(1f, preserveTexture = true)

        assertTrue("preserveTexture must blend less", preserved < full)
        assertEquals(0f, BeautyEditor.smoothMix(0f, preserveTexture = false), 0.0001f)
        // Out-of-range strengths cannot escape the 0..1 blend window.
        assertEquals(full, BeautyEditor.smoothMix(9f, preserveTexture = false), 0.0001f)
    }

    @Test
    fun whitenAndLipMixStayInRange() {
        assertEquals(0f, BeautyEditor.whitenMix(0f), 0.0001f)
        assertEquals(0.5f, BeautyEditor.whitenMix(1f), 0.0001f)
        assertEquals(0.5f, BeautyEditor.whitenMix(4f), 0.0001f)
        assertEquals(0f, BeautyEditor.lipMix(-1f), 0.0001f)
        assertEquals(0.55f, BeautyEditor.lipMix(1f), 0.0001f)
    }

    @Test
    fun faceLabelIsHonestAboutAnEmptyResult() {
        assertEquals("TIDAK ADA WAJAH", BeautyEditor.faceLabel(0))
        assertEquals("TIDAK ADA WAJAH", BeautyEditor.faceLabel(-1))
        assertEquals("FACE DETECTED · 1", BeautyEditor.faceLabel(1))
        assertEquals("FACE DETECTED · 2", BeautyEditor.faceLabel(2))
    }

    @Test
    fun summarySaysWhenNothingIsSelected() {
        assertEquals("Belum ada slider yang digeser", BeautyEditor.summary(BeautyConfig()))
        assertEquals(
            "1 slider aktif · tekstur dijaga",
            BeautyEditor.summary(BeautyConfig(smooth = 10f))
        )
        assertEquals(
            "2 slider aktif · kulit halus penuh",
            BeautyEditor.summary(BeautyConfig(smooth = 10f, lips = 5f, preserveTexture = false))
        )
    }

    @Test
    fun tabsMatchTheApprovedMockup() {
        assertEquals(listOf("Face", "Body", "Makeup"), BeautyEditor.TABS)
        assertNull(BeautyEditor.tabNote("Face"))
    }

    @Test
    fun tabNotesNameTheShippedEvidenceInsteadOfInventingSliders() {
        val body = BeautyEditor.tabNote("Body").orEmpty()
        assertTrue(body.contains("body_reshapes"))
        assertTrue(body.contains("MANUAL_RESHAPE_INTENSITY_SLIM"))

        val makeup = BeautyEditor.tabNote("Makeup").orEmpty()
        // Every shipped makeup key not yet exposed is named for the next milestone.
        BeautyEditor.SHIPPED_KEYS
            .filter { it.group == BeautyGroup.MAKEUP }
            .forEach { assertTrue(makeup.contains(it.key)) }
        assertNotEquals("", makeup)
    }

    @Test
    fun shippedKeysAreTheOnesTheMockupHasNoSlotForYet() {
        assertEquals(
            listOf("auto_dark_circle", "auto_eye_bright", "auto_oil_remove",
                "face_little_head", "type_makeup_all", "type_blush"),
            BeautyEditor.SHIPPED_KEYS.map { it.key }
        )
        // A shipped key never doubles as an active slider.
        val sliderKeys = BeautyEditor.FACE_SLIDERS.map { it.key }.toSet()
        assertTrue(BeautyEditor.SHIPPED_KEYS.none { it.key in sliderKeys })
    }
}
