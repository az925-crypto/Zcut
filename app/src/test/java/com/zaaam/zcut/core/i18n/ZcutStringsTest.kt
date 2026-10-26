package com.zaaam.zcut.core.i18n

import com.zaaam.zcut.core.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ID/EN switch is a preference, not a resource qualifier, so a missing
 * translation would silently fall back to Indonesian. These checks keep both
 * columns complete and keep placeholders aligned.
 */
class ZcutStringsTest {

    @Test
    fun everyEntryHasBothLanguages() {
        ZcutText.entries.forEach { text ->
            assertTrue("blank Indonesian copy for $text", text.id.isNotBlank())
            assertTrue("blank English copy for $text", text.en.isNotBlank())
        }
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        val placeholders = Regex("%[sd]")
        ZcutText.entries.forEach { text ->
            assertEquals(
                "placeholder mismatch for $text",
                placeholders.findAll(text.id).map { it.value }.toList(),
                placeholders.findAll(text.en).map { it.value }.toList()
            )
        }
    }

    @Test
    fun ztPicksTheActiveColumn() {
        assertEquals("Profil", zt(ZcutText.NavProfile, AppLanguage.ID))
        assertEquals("Profile", zt(ZcutText.NavProfile, AppLanguage.EN))
        // Defaults to Indonesian when no language is passed.
        assertEquals("Kembali", zt(ZcutText.Back))
    }

    @Test
    fun ztfFormatsWithoutLeakingTheDeviceLocale() {
        assertEquals("12 draft", ztf(ZcutText.DraftCount, AppLanguage.ID, 12))
        assertEquals("12 drafts", ztf(ZcutText.DraftCount, AppLanguage.EN, 12))
        // The draft name is quoted in the copy, so the format keeps the quotes.
        assertEquals(
            "\"Midnight in Tokyo\" akan dihapus permanen dari perangkat ini.",
            ztf(ZcutText.DeleteForeverMessage, AppLanguage.ID, "Midnight in Tokyo")
        )
    }

    @Test
    fun screenTextTranslatesKnownCopyOnly() {
        assertEquals("Settings", ZcutScreenText.title("Pengaturan", AppLanguage.EN))
        assertEquals("Pengaturan", ZcutScreenText.title("Pengaturan", AppLanguage.ID))
        // Dynamic titles (project/template names) pass through untouched.
        assertEquals("Midnight in Tokyo", ZcutScreenText.title("Midnight in Tokyo", AppLanguage.EN))
        assertEquals(
            "Local preferences",
            ZcutScreenText.subtitle("Preferensi lokal", AppLanguage.EN)
        )
        assertFalse(
            ZcutScreenText.subtitle("Preferensi lokal", AppLanguage.EN)
                .contains("Preferensi")
        )
    }
}
