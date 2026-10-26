package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catalogue guard for the built-in packs. The mockup renders `slotSummary` and
 * `durationLabel` verbatim, so the shape of those strings is pinned here.
 */
class TemplateCatalogTest {

    @Test
    fun everyTemplateIsUsable() {
        assertTrue(TemplateCatalog.all.isNotEmpty())
        TemplateCatalog.all.forEach { template ->
            assertTrue("${template.id} needs a name", template.name.isNotBlank())
            assertTrue("${template.id} needs art", template.artKey.isNotBlank())
            assertTrue("${template.id} needs a description", template.description.isNotBlank())
            assertTrue("${template.id} needs slots", template.slots.isNotEmpty())
            assertTrue(
                "${template.id} slots must be positive",
                template.slots.all { it.durationMs > 0L }
            )
        }
    }

    @Test
    fun idsAreUnique() {
        val ids = TemplateCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun lookupIsByIdAndNullSafe() {
        val first = TemplateCatalog.all.first()
        assertEquals(first, TemplateCatalog.byId(first.id))
        assertNull(TemplateCatalog.byId("does-not-exist"))
        assertNull(TemplateCatalog.byId(null))
    }

    @Test
    fun forYouIsTheWholeShelfAndOthersFilter() {
        assertEquals(TemplateCatalog.all, TemplateCatalog.inCategory(TemplateCategory.FOR_YOU))
        TemplateCategory.entries
            .filter { it != TemplateCategory.FOR_YOU }
            .forEach { category ->
                val filtered = TemplateCatalog.inCategory(category)
                assertTrue(filtered.all { it.category == category })
                assertTrue(category.label.isNotBlank())
            }
        assertEquals(TemplateCategory.entries, TemplateCatalog.categories)
    }

    @Test
    fun slotStartsAccumulateAndSummaryMatchesTheSlots() {
        val template = TemplateCatalog.all.first { it.slotCount > 1 }
        val starts = template.slotStartsMs()

        assertEquals(0L, starts.first())
        assertEquals(template.slotCount, starts.size)
        template.slots.forEachIndexed { index, slot ->
            assertEquals(starts[index] + slot.durationMs, starts.getOrElse(index + 1) {
                template.durationMs
            })
        }
        assertEquals(template.slots.sumOf { it.durationMs }, template.durationMs)
        assertEquals("${template.slotCount} slots · ${template.durationMs / 1_000}s", template.slotSummary)
    }

    @Test
    fun discoverPostsPointAtRealTemplates() {
        assertTrue(DiscoverFeed.posts.isNotEmpty())
        DiscoverFeed.posts.forEach { post ->
            assertTrue("${post.id} needs a title", post.title.isNotBlank())
            assertTrue("${post.id} must resolve to a template", post.template != null)
            assertTrue(
                "${post.id} must be in the feed's tag list",
                post.tag in DiscoverFeed.tags
            )
        }
    }

    @Test
    fun discoverTagsFilterFromTheCuratedDefault() {
        assertEquals(DiscoverFeed.posts, DiscoverFeed.inTag(DiscoverFeed.tags.first()))
        val other = DiscoverFeed.tags.drop(1)
        other.forEach { tag ->
            assertTrue(DiscoverFeed.inTag(tag).all { it.tag == tag })
        }
    }
}