package com.zaaam.zcut.ui.navigation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity guard between the approved Stage 2 mockup (42 screens) and the
 * Android navigation graph. If a screen exists in one but not the other, the
 * build fails instead of silently drifting.
 */
class ZcutDestinationsTest {

    @Test
    fun mockupRegistryIsReadable() {
        assertEquals(
            "mockup/assets/screens.js should declare exactly 42 screens",
            42,
            MOCKUP_IDS.size
        )
    }

    @Test
    fun androidGraphCoversEveryMockupScreen() {
        val android = ZcutDestinations.all.map { it.route }.toSet()
        val missing = MOCKUP_IDS - android
        assertTrue("Routes missing from the Android graph: $missing", missing.isEmpty())
    }

    @Test
    fun androidGraphHasNoPhantomScreens() {
        val extra = ZcutDestinations.all.map { it.route }.toSet() - MOCKUP_IDS
        assertTrue("Routes with no mockup screen: $extra", extra.isEmpty())
    }

    @Test
    fun routesAreUnique() {
        val routes = ZcutDestinations.all.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun everyScreenHasATitleAndUniqueRoute() {
        ZcutDestinations.all.forEach { destination ->
            assertTrue("Blank title for ${destination.route}", destination.title.isNotBlank())
            assertNotNull(ZcutDestinations.get(destination.route))
        }
    }

    @Test
    fun bottomNavRoutesAreDeclaredScreens() {
        val routes = ZcutDestinations.all.map { it.route }.toSet()
        ZcutDestinations.bottomNav.forEach { route ->
            assertTrue("Bottom nav route '$route' has no destination", route in routes)
        }
        assertEquals(5, ZcutDestinations.bottomNav.size)
        assertEquals(5, ZcutDestinations.bottomNav.toSet().size)
    }

    @Test
    fun stageThreeFoundationScreensAreImplemented() {
        val implemented = ZcutDestinations.all.filter { it.implemented }.map { it.route }.toSet()
        assertTrue(implemented.containsAll(
            listOf(ZcutRoutes.Splash, ZcutRoutes.Onboarding, ZcutRoutes.Home, ZcutRoutes.Projects)
        ))
    }

    @Test
    fun textRouteCarriesProjectClipAndPlayhead() {
        val route = ZcutRoutes.text("p 1", "c/1", 1_234L)

        assertTrue(route.startsWith("text?projectId=p%201"))
        assertTrue(route.contains("atMs=1234"))
        assertTrue(route.contains("clipId=c%2F1"))
        assertTrue(ZcutDestinations.get(ZcutRoutes.Text)?.implemented == true)
    }

    @Test
    fun audioRouteCarriesProjectClipAndPlayhead() {
        val route = ZcutRoutes.audio("p 1", "c/1", 1_234L)

        assertTrue(route.startsWith("audio?projectId=p%201"))
        assertTrue(route.contains("atMs=1234"))
        assertTrue(route.contains("clipId=c%2F1"))
        assertTrue(ZcutDestinations.get(ZcutRoutes.Audio)?.implemented == true)
    }

    @Test
    fun audioRouteOmitsAnEmptyClip() {
        val route = ZcutRoutes.audio("p1", null, 0L)

        assertTrue(route.startsWith("audio?projectId=p1"))
        assertTrue(!route.contains("clipId"))
    }

    @Test
    fun ttsRouteCarriesProjectAndPlayhead() {
        val route = ZcutRoutes.tts("p 1", 1_234L)

        assertTrue(route.startsWith("tts?projectId=p%201"))
        assertTrue(route.contains("atMs=1234"))
        assertTrue(!route.contains("clipId"))
        assertTrue(ZcutDestinations.get(ZcutRoutes.Tts)?.implemented == true)
    }

    @Test
    fun mattingRouteIsAClipTool() {
        assertTrue(ZcutDestinations.get(ZcutRoutes.Matting)?.implemented == true)
        assertEquals(
            "matting?projectId={projectId}&clipId={clipId}",
            ZcutRoutes.clipToolPattern(ZcutRoutes.Matting)
        )
    }

    @Test
    fun beautyRouteIsAClipTool() {
        assertTrue(ZcutDestinations.get(ZcutRoutes.Beauty)?.implemented == true)
        assertEquals(
            "beauty?projectId={projectId}&clipId={clipId}",
            ZcutRoutes.clipToolPattern(ZcutRoutes.Beauty)
        )
    }

    /**
     * Regression for the M14/M15 bug where a panel was registered in the NavHost
     * with `clipTool(...)` but missing from `argToolRoutes`: the editor then
     * navigated to the bare route and the panel could never open. Pinning the set
     * forces a conscious update here whenever a clip tool is added or removed.
     */
    @Test
    fun clipToolRoutesAreExactlyTheOnesTheEditorPassesArgumentsTo() {
        val clipTools = ZcutDestinations.all
            .map { it.route }
            .filter { ZcutRoutes.isClipTool(it) }
            .toSet()

        assertEquals(
            setOf(
                ZcutRoutes.Trim, ZcutRoutes.Speed, ZcutRoutes.Keyframe,
                ZcutRoutes.Transition, ZcutRoutes.Filters, ZcutRoutes.Adjust,
                ZcutRoutes.Effects, ZcutRoutes.Mask, ZcutRoutes.Matting,
                ZcutRoutes.Beauty
            ),
            clipTools
        )
    }

    @Test
    fun everyClipToolCarriesTheProjectAndTheSelectedClip() {
        ZcutDestinations.all
            .map { it.route }
            .filter { ZcutRoutes.isClipTool(it) }
            .forEach { route ->
                val target = ZcutRoutes.toolRoute(route, "p 1", "c/1")
                assertTrue("$route did not carry the project", target.startsWith("$route?projectId="))
                assertTrue("$route did not carry the clip", target.contains("clipId=c%2F1"))

                // A clip tool opened without a selection still carries the draft.
                val withoutClip = ZcutRoutes.toolRoute(route, "p1", null)
                assertTrue(withoutClip.startsWith("$route?projectId=p1"))
                assertTrue("$route should omit an absent clip", !withoutClip.contains("clipId"))
            }
    }

    @Test
    fun exportRoutesAreImplementedAndCarryTheProject() {
        val route = ZcutRoutes.export("p 1")

        assertTrue(route.startsWith("export?projectId=p%201"))
        assertTrue(ZcutDestinations.get(ZcutRoutes.Export)?.implemented == true)
        assertTrue(ZcutDestinations.get(ZcutRoutes.ExportDone)?.implemented == true)
    }

    @Test
    fun exportDoneRouteCarriesTheFileNameAndNeverAPath() {
        val route = ZcutRoutes.exportDone(
            projectId = "p1",
            fileName = "Draft-123.mp4",
            durationMs = 24_000L,
            sizeBytes = 1_234_567L,
            coverFileName = "Draft-123-cover.jpg"
        )

        assertTrue(route.startsWith("export-done?projectId=p1"))
        assertTrue(route.contains("file=Draft-123.mp4"))
        assertTrue(route.contains("durationMs=24000"))
        assertTrue(route.contains("sizeBytes=1234567"))
        assertTrue(route.contains("cover=Draft-123-cover.jpg"))
        assertTrue("a path leaked into the route: $route", !route.contains("/"))
    }

    @Test
    fun exportDoneRouteOmitsAnAbsentCover() {
        val route = ZcutRoutes.exportDone("p1", "a.mp4", 0L, 1L)

        assertTrue(!route.contains("cover="))
    }

    private companion object {
        private val ID_PATTERN = Regex("""id:'([a-z0-9-]+)'""")

        /** Read once per test class. */
        val MOCKUP_IDS: Set<String> by lazy { readMockupScreenIds() }

        /**
         * Unit tests run with the module directory as the working directory, so
         * the mockup lives one or more levels up. Walk up instead of hardcoding.
         */
        fun readMockupScreenIds(): Set<String> {
            val file = findScreensJs()
            return ID_PATTERN.findAll(file.readText())
                .map { it.groupValues[1] }
                .toSet()
        }

        fun findScreensJs(): File {
            var dir: File? = File("").absoluteFile
            repeat(6) {
                val candidate = File(dir, "mockup/assets/screens.js")
                if (candidate.isFile) return candidate
                dir = dir?.parentFile
            }
            throw AssertionError(
                "Could not locate mockup/assets/screens.js walking up from ${File("").absolutePath}"
            )
        }
    }
}
