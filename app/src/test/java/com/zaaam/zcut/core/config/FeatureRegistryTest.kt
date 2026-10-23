package com.zaaam.zcut.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureRegistryTest {

    @Test
    fun registryCoversAll31FeatureAreasOnce() {
        assertEquals(31, FeatureRegistry.all.size)
        assertEquals(31, FeatureRegistry.all.map { it.id }.distinct().size)
        assertTrue(FeatureRegistry.get("F01") != null)
        assertTrue(FeatureRegistry.get("F31") != null)
    }

    @Test
    fun onlyGenerativeBackendFeaturesAreRequired() {
        assertEquals(
            setOf("F24", "F25"),
            FeatureRegistry.backendRequired().map { it.id }.toSet()
        )
    }

    @Test
    fun backendFlagsMatchRequirement() {
        FeatureRegistry.all.forEach { feature ->
            when (feature.requirement) {
                BackendRequirement.REQUIRED ->
                    assertTrue(
                        "${feature.id} must not claim a local fallback",
                        !feature.localFallback
                    )
                BackendRequirement.OPTIONAL, BackendRequirement.NONE ->
                    assertTrue(
                        "${feature.id} should keep a local path",
                        feature.localFallback
                    )
            }
        }
    }
}
