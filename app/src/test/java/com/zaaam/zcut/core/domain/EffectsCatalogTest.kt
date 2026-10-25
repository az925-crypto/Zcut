package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.EffectConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectsCatalogTest {

    @Test
    fun catalogIdsAreUniqueAndCoverEveryCategory() {
        val ids = EffectsCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        EffectsCatalog.Category.entries.forEach { category ->
            assertTrue(
                "Category ${category.label} is empty",
                EffectsCatalog.byCategory(category).isNotEmpty()
            )
        }
    }

    @Test
    fun findReturnsNullForUnknownId() {
        assertNotNull(EffectsCatalog.find("glow"))
        assertNull(EffectsCatalog.find("does-not-exist"))
        assertNull(EffectsCatalog.find(null))
    }

    @Test
    fun zeroIntensityKeepsIdentity() {
        assertArrayEquals(
            ColorPipeline.identity(),
            EffectsCatalog.previewMatrix(EffectsCatalog.find("glow"), 0f),
            0.0001f
        )
    }

    @Test
    fun fullIntensityEqualsEffectMatrix() {
        val glow = EffectsCatalog.find("glow")!!
        assertArrayEquals(
            glow.matrix!!,
            EffectsCatalog.previewMatrix(glow, 1f),
            0.0001f
        )
    }

    @Test
    fun stackingFiltersComposesMatrices() {
        val single = EffectsCatalog.previewMatrix(listOf(EffectConfig("glow", 1f)))
        val stacked = EffectsCatalog.previewMatrix(
            listOf(EffectConfig("glow", 1f), EffectConfig("grain", 1f))
        )
        val expected = ColorPipeline.pipe(
            EffectsCatalog.find("glow")!!.matrix!!,
            EffectsCatalog.find("grain")!!.matrix!!
        )

        assertArrayEquals(expected, stacked, 0.0001f)
        assertTrue(!single.contentEquals(stacked))
    }

    @Test
    fun emptyListIsIdentityAndUnknownIdsAreIgnored() {
        assertArrayEquals(
            ColorPipeline.identity(),
            EffectsCatalog.previewMatrix(emptyList()),
            0.0001f
        )
        assertArrayEquals(
            ColorPipeline.identity(),
            EffectsCatalog.previewMatrix(listOf(EffectConfig("nope", 1f))),
            0.0001f
        )
    }

    @Test
    fun blurRadiusScalesWithIntensity() {
        assertEquals(6f, EffectsCatalog.blurRadius(listOf(EffectConfig("dream", 1f))), 0.001f)
        assertEquals(3f, EffectsCatalog.blurRadius(listOf(EffectConfig("dream", 0.5f))), 0.001f)
        assertEquals(0f, EffectsCatalog.blurRadius(listOf(EffectConfig("glow", 1f))), 0.001f)
    }

    @Test
    fun arEffectIsMarkedAsBackendRequiredButStillSelectable() {
        val ar = EffectsCatalog.find("ar-mask")!!

        assertTrue(ar.backendRequired)
        assertNotNull(ar.matrix)
    }
}
