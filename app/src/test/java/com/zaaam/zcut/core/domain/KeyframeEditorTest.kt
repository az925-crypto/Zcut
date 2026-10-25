package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.AnimatableProperty
import com.zaaam.zcut.core.model.Clip
import com.zaaam.zcut.core.model.Easing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeEditorTest {

    private fun clip(): Clip = Clip(startMs = 0L, durationMs = 1_000L)

    @Test
    fun upsertAddsKeyframeAndCoercesValue() {
        val withFrame = KeyframeEditor.upsert(
            clip = clip(),
            property = AnimatableProperty.OPACITY,
            timeMs = 500L,
            value = 4f
        )

        val frame = withFrame.keyframes.single()
        assertEquals(500L, frame.timeMs)
        assertEquals(AnimatableProperty.OPACITY, frame.property)
        assertEquals(1f, frame.value, 0.001f)
    }

    @Test
    fun upsertWithinToleranceReplacesInsteadOfDuplicating() {
        val once = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.SCALE,
            timeMs = 500L,
            value = 1.5f
        )
        val twice = KeyframeEditor.upsert(
            once,
            AnimatableProperty.SCALE,
            timeMs = 520L,
            value = 2f
        )

        assertEquals(1, twice.keyframes.size)
        assertEquals(2f, twice.keyframes.single().value, 0.001f)
    }

    @Test
    fun upsertKeepsExistingEasingOnSameFrame() {
        val eased = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.SCALE,
            timeMs = 500L,
            value = 1.5f,
            easing = Easing.EASE_OUT
        )
        val updated = KeyframeEditor.upsert(
            eased,
            AnimatableProperty.SCALE,
            timeMs = 500L,
            value = 2f,
            easing = Easing.LINEAR
        )

        assertEquals(Easing.EASE_OUT, updated.keyframes.single().easing)
    }

    @Test
    fun upsertClampsTimeToClipDuration() {
        val frame = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.ROTATION,
            timeMs = 5_000L,
            value = 45f
        ).keyframes.single()

        assertEquals(1_000L, frame.timeMs)
    }

    @Test
    fun sampleInterpolatesAndHoldsEndpoints() {
        val animated = KeyframeEditor.upsert(
            KeyframeEditor.upsert(
                clip(),
                AnimatableProperty.POSITION_X,
                timeMs = 200L,
                value = 0f
            ),
            AnimatableProperty.POSITION_X,
            timeMs = 800L,
            value = 100f
        )

        assertEquals(0f, KeyframeEditor.sample(animated, AnimatableProperty.POSITION_X, 0L), 0.001f)
        assertEquals(50f, KeyframeEditor.sample(animated, AnimatableProperty.POSITION_X, 500L), 0.001f)
        assertEquals(100f, KeyframeEditor.sample(animated, AnimatableProperty.POSITION_X, 900L), 0.001f)
    }

    @Test
    fun sampleAppliesEasingOfLeftFrame() {
        val animated = KeyframeEditor.upsert(
            KeyframeEditor.upsert(
                clip(),
                AnimatableProperty.SCALE,
                timeMs = 0L,
                value = 1f,
                easing = Easing.EASE_IN
            ),
            AnimatableProperty.SCALE,
            timeMs = 1_000L,
            value = 2f
        )

        // EASE_IN is t², so halfway through the segment the value is 1.25.
        assertEquals(1.25f, KeyframeEditor.sample(animated, AnimatableProperty.SCALE, 500L), 0.001f)
    }

    @Test
    fun sampleIgnoresOtherProperties() {
        val animated = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.VOLUME,
            timeMs = 500L,
            value = 0.25f
        )

        assertEquals(1f, KeyframeEditor.sample(animated, AnimatableProperty.OPACITY, 500L), 0.001f)
    }

    @Test
    fun keyframeAtFindsNearestWithinTolerance() {
        val animated = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.ROTATION,
            timeMs = 500L,
            value = 10f
        )

        assertNotNull(KeyframeEditor.keyframeAt(animated, AnimatableProperty.ROTATION, 520L))
        assertNull(KeyframeEditor.keyframeAt(animated, AnimatableProperty.ROTATION, 700L))
        assertNull(KeyframeEditor.keyframeAt(animated, AnimatableProperty.SCALE, 500L))
    }

    @Test
    fun removeOnlyDropsMatchingPropertyAndTime() {
        val animated = KeyframeEditor.upsert(
            KeyframeEditor.upsert(
                clip(),
                AnimatableProperty.SCALE,
                timeMs = 500L,
                value = 1.5f
            ),
            AnimatableProperty.ROTATION,
            timeMs = 500L,
            value = 10f
        )

        val remaining = KeyframeEditor.remove(animated, AnimatableProperty.SCALE, 500L)

        assertEquals(1, remaining.keyframes.size)
        assertEquals(AnimatableProperty.ROTATION, remaining.keyframes.single().property)
    }

    @Test
    fun setEasingOnlyUpdatesTargetFrame() {
        val animated = KeyframeEditor.upsert(
            KeyframeEditor.upsert(
                clip(),
                AnimatableProperty.SCALE,
                timeMs = 0L,
                value = 1f
            ),
            AnimatableProperty.SCALE,
            timeMs = 1_000L,
            value = 2f
        )

        val eased = KeyframeEditor.setEasing(
            animated,
            AnimatableProperty.SCALE,
            1_000L,
            Easing.EASE_OUT
        )

        assertEquals(Easing.LINEAR, eased.keyframes.first().easing)
        assertEquals(Easing.EASE_OUT, eased.keyframes.last().easing)
    }

    @Test
    fun setPropertyWritesTransformOpacityAndVolume() {
        val moved = KeyframeEditor.setProperty(
            clip(),
            AnimatableProperty.POSITION_X,
            40f
        )
        val scaled = KeyframeEditor.setProperty(moved, AnimatableProperty.SCALE, 4f)
        val dimmed = KeyframeEditor.setProperty(scaled, AnimatableProperty.OPACITY, 0.4f)
        val muted = KeyframeEditor.setProperty(dimmed, AnimatableProperty.VOLUME, 0.2f)

        assertEquals(40f, muted.transform.positionX, 0.001f)
        assertEquals(4f, muted.transform.scale, 0.001f)
        assertEquals(0.4f, muted.opacity, 0.001f)
        assertEquals(0.2f, muted.audio.volume, 0.001f)
    }

    @Test
    fun autoKeyOnlyWritesPlayheadValueForAnimatedProperty() {
        val staticClip = KeyframeEditor.setPropertyAndAutoKey(
            clip(),
            AnimatableProperty.SCALE,
            1.4f,
            500L
        )
        assertTrue(staticClip.keyframes.isEmpty())
        assertEquals(1.4f, staticClip.transform.scale, 0.001f)

        val animated = KeyframeEditor.upsert(
            clip(),
            AnimatableProperty.SCALE,
            timeMs = 0L,
            value = 1f
        )
        val keyed = KeyframeEditor.setPropertyAndAutoKey(
            animated,
            AnimatableProperty.SCALE,
            1.4f,
            500L
        )

        assertEquals(2, keyed.keyframes.size)
        assertEquals(1.4f, KeyframeEditor.sample(keyed, AnimatableProperty.SCALE, 500L), 0.001f)
    }

    @Test
    fun rangeClampsPositionScaleRotationOpacityAndVolume() {
        val ranged = KeyframeEditor.upsert(
            KeyframeEditor.upsert(
                KeyframeEditor.upsert(
                    KeyframeEditor.upsert(
                        KeyframeEditor.upsert(
                            clip(),
                            AnimatableProperty.POSITION_X,
                            timeMs = 100L,
                            value = -500f
                        ),
                        AnimatableProperty.SCALE,
                        timeMs = 100L,
                        value = 0.01f
                    ),
                    AnimatableProperty.ROTATION,
                    timeMs = 100L,
                    value = 400f
                ),
                AnimatableProperty.OPACITY,
                timeMs = 100L,
                value = -1f
            ),
            AnimatableProperty.VOLUME,
            timeMs = 100L,
            value = 9f
        )

        assertEquals(-100f, ranged.keyframes.first { it.property == AnimatableProperty.POSITION_X }.value, 0.001f)
        assertEquals(0.1f, ranged.keyframes.first { it.property == AnimatableProperty.SCALE }.value, 0.001f)
        assertEquals(180f, ranged.keyframes.first { it.property == AnimatableProperty.ROTATION }.value, 0.001f)
        assertEquals(0f, ranged.keyframes.first { it.property == AnimatableProperty.OPACITY }.value, 0.001f)
        assertEquals(2f, ranged.keyframes.first { it.property == AnimatableProperty.VOLUME }.value, 0.001f)
    }

    @Test
    fun emptyClipRejectsKeyframes() {
        val empty = KeyframeEditor.upsert(
            Clip(startMs = 0L, durationMs = 0L),
            AnimatableProperty.SCALE,
            timeMs = 0L,
            value = 2f
        )

        assertTrue(empty.keyframes.isEmpty())
    }
}
