package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.SpeedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedCurveTest {

    @Test
    fun constantSpeedDurationMatchesDivision() {
        assertEquals(
            500L,
            SpeedCurve.presentationDurationMs(1_000L, 2f, emptyList())
        )
        assertEquals(
            2_000L,
            SpeedCurve.presentationDurationMs(1_000L, 0.5f, emptyList())
        )
    }

    @Test
    fun curveIntegratesLinearSpeed() {
        val curve = listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(0.5f, 2f),
            SpeedPoint(1f, 1f)
        )

        // 500ms at 1x→2x + 500ms at 2x→1x, closed form: 2 * 500 * ln(2) = 693ms.
        assertEquals(693L, SpeedCurve.presentationDurationMs(1_000L, 1f, curve))
    }

    @Test
    fun inverseMappingUsesClosedFormIntegral() {
        val curve = listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(0.5f, 2f),
            SpeedPoint(1f, 1f)
        )

        val source = SpeedCurve.sourceDurationForPresentationMs(
            sourceDurationMs = 1_000L,
            speed = 1f,
            curve = curve,
            presentationMs = 333L
        )

        // 1x→2x over 500ms of source consumes ~473ms of presentation by 333ms.
        assertEquals(473L, source)
    }

    @Test
    fun inverseMappingClampsAtBothEnds() {
        val curve = listOf(SpeedPoint(0f, 1f), SpeedPoint(1f, 4f))

        assertEquals(
            0L,
            SpeedCurve.sourceDurationForPresentationMs(1_000L, 1f, curve, 0L)
        )
        assertEquals(
            1_000L,
            SpeedCurve.sourceDurationForPresentationMs(1_000L, 1f, curve, 99_999L)
        )
    }

    @Test
    fun normalizedClosesEndpointsAndDropsDuplicates() {
        val curve = SpeedCurve.normalized(
            listOf(
                SpeedPoint(0.4f, 2f),
                SpeedPoint(0.4f, 3f),
                SpeedPoint(0.8f, 1f)
            )
        )

        assertEquals(0f, curve.first().atFraction)
        assertEquals(3f, curve.first().speed)
        assertEquals(1f, curve.last().atFraction)
        assertEquals(1f, curve.last().speed)
        assertEquals(4, curve.size)
    }

    @Test
    fun speedAtInterpolatesLinearly() {
        val curve = listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(1f, 3f)
        )

        assertEquals(2f, SpeedCurve.speedAt(curve, 0.5f), 0.001f)
        assertEquals(1f, SpeedCurve.speedAt(curve, -1f), 0.001f)
        assertEquals(3f, SpeedCurve.speedAt(curve, 2f), 0.001f)
    }

    @Test
    fun subCurveRemapsAndKeepsEndpoints() {
        val curve = listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(0.5f, 2f),
            SpeedPoint(1f, 4f)
        )

        val right = SpeedCurve.subCurve(curve, 0.5f, 1f)

        assertEquals(0f, right.first().atFraction)
        assertEquals(2f, right.first().speed, 0.001f)
        assertEquals(1f, right.last().atFraction)
        assertEquals(4f, right.last().speed, 0.001f)
        assertTrue(right.size >= 2)
    }

    @Test
    fun smoothedBlendsInteriorPointsAndKeepsEndpoints() {
        val curve = listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(0.5f, 4f),
            SpeedPoint(1f, 1f)
        )

        val smoothed = SpeedCurve.smoothed(curve, 1f)

        assertEquals(1f, smoothed.first().speed, 0.001f)
        assertEquals(1f, smoothed[1].speed, 0.001f)
        assertEquals(1f, smoothed.last().speed, 0.001f)
    }

    @Test
    fun smoothedWithZeroAmountReturnsNormalizedCurve() {
        val curve = listOf(
            SpeedPoint(0.5f, 2f),
            SpeedPoint(0f, 1f)
        )

        val smoothed = SpeedCurve.smoothed(curve, 0f)

        assertEquals(0f, smoothed.first().atFraction)
        assertEquals(1f, smoothed.last().atFraction)
        assertEquals(2f, smoothed.last().speed, 0.001f)
        assertEquals(3, smoothed.size)
    }
}
