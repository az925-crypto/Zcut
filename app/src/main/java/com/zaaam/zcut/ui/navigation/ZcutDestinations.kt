package com.zaaam.zcut.ui.navigation

import com.zaaam.zcut.core.config.FeatureRegistry

/** Route constants for the destinations referenced from code. */
object ZcutRoutes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Projects = "projects"
    const val Editor = "editor"
    const val MediaPicker = "media-picker"
    const val Trim = "trim"
    const val Speed = "speed"
    const val Keyframe = "keyframe"
    const val Transition = "transition"
    const val Audio = "audio"
    const val Tts = "tts"
    const val Text = "text"
    const val Stickers = "stickers"
    const val Effects = "effects"
    const val Filters = "filters"
    const val Adjust = "adjust"
    const val Mask = "mask"
    const val Matting = "matting"
    const val Export = "export"
    const val ExportDone = "export-done"
    const val Templates = "templates"
    const val AiStudio = "ai-studio"
    const val Profile = "profile"
    const val Settings = "settings"

    /** NavHost pattern; the destination id itself stays `editor` for parity. */
    const val EditorPattern = "editor/{projectId}"

    /** `projectId` is absent when the picker starts a brand-new draft. */
    const val MediaPickerPattern = "media-picker?projectId={projectId}"

    const val ArgProjectId = "projectId"
    const val ArgClipId = "clipId"
    const val ArgAtMs = "atMs"
    const val ArgFile = "file"
    const val ArgCover = "cover"
    const val ArgDurationMs = "durationMs"
    const val ArgSizeBytes = "sizeBytes"

    /** Text is not a clip tool: it can create a clip, so it carries `atMs`. */
    const val TextPattern = "$Text?$ArgProjectId={$ArgProjectId}" +
        "&$ArgClipId={$ArgClipId}&$ArgAtMs={$ArgAtMs}"

    /** Audio can create clips too, so it carries the selected clip and playhead. */
    const val AudioPattern = "$Audio?$ArgProjectId={$ArgProjectId}" +
        "&$ArgClipId={$ArgClipId}&$ArgAtMs={$ArgAtMs}"

    /** TTS only inserts at a position; the clip is always new. */
    const val TtsPattern = "$Tts?$ArgProjectId={$ArgProjectId}&$ArgAtMs={$ArgAtMs}"

    /** Export renders one draft; the result screen is a separate destination. */
    const val ExportPattern = "$Export?$ArgProjectId={$ArgProjectId}"

    /**
     * The render result carries the output file name (never a path: the file
     * always lives in the app export folder, so no slash has to survive a URL).
     */
    const val ExportDonePattern = "$ExportDone?$ArgProjectId={$ArgProjectId}" +
        "&$ArgFile={$ArgFile}&$ArgDurationMs={$ArgDurationMs}" +
        "&$ArgSizeBytes={$ArgSizeBytes}&$ArgCover={$ArgCover}"

    /**
     * Tool panels whose route pattern already carries `projectId`/`clipId`.
     * Grows one milestone at a time so a half-built panel still falls through to
     * the honest placeholder instead of navigating to an unregistered route.
     */
    private val argToolRoutes: Set<String> =
        setOf(Trim, Speed, Keyframe, Transition, Filters, Adjust, Effects, Mask)

    fun isClipTool(route: String): Boolean = route in argToolRoutes

    fun editor(projectId: String): String = "$Editor/${encode(projectId)}"

    fun mediaPicker(projectId: String? = null): String =
        if (projectId.isNullOrBlank()) {
            MediaPicker
        } else {
            "$MediaPicker?$ArgProjectId=${encode(projectId)}"
        }

    fun trim(projectId: String, clipId: String?): String =
        toolRoute(Trim, projectId, clipId)

    fun speed(projectId: String, clipId: String?): String =
        toolRoute(Speed, projectId, clipId)

    fun keyframe(projectId: String, clipId: String?): String =
        toolRoute(Keyframe, projectId, clipId)

    fun transition(projectId: String, clipId: String?): String =
        toolRoute(Transition, projectId, clipId)

    fun effects(projectId: String, clipId: String?): String =
        toolRoute(Effects, projectId, clipId)

    fun filters(projectId: String, clipId: String?): String =
        toolRoute(Filters, projectId, clipId)

    fun adjust(projectId: String, clipId: String?): String =
        toolRoute(Adjust, projectId, clipId)

    fun mask(projectId: String, clipId: String?): String =
        toolRoute(Mask, projectId, clipId)

    fun text(projectId: String, clipId: String?, atMs: Long = 0L): String {
        val base = "$Text?$ArgProjectId=${encode(projectId)}" +
            "&$ArgAtMs=${atMs.coerceAtLeast(0L)}"
        val clip = clipId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (clip == null) base else "$base&$ArgClipId=$clip"
    }

    fun audio(projectId: String, clipId: String?, atMs: Long = 0L): String {
        val base = "$Audio?$ArgProjectId=${encode(projectId)}" +
            "&$ArgAtMs=${atMs.coerceAtLeast(0L)}"
        val clip = clipId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (clip == null) base else "$base&$ArgClipId=$clip"
    }

    fun tts(projectId: String, atMs: Long = 0L): String =
        "$Tts?$ArgProjectId=${encode(projectId)}&$ArgAtMs=${atMs.coerceAtLeast(0L)}"

    fun export(projectId: String): String = "$Export?$ArgProjectId=${encode(projectId)}"

    /**
     * `cover` is optional: without it the done screen simply shows no cover
     * line. The file name is encoded like any other argument.
     */
    fun exportDone(
        projectId: String,
        fileName: String,
        durationMs: Long,
        sizeBytes: Long,
        coverFileName: String? = null
    ): String {
        val base = "$ExportDone?$ArgProjectId=${encode(projectId)}" +
            "&$ArgFile=${encode(fileName)}" +
            "&$ArgDurationMs=${durationMs.coerceAtLeast(0L)}" +
            "&$ArgSizeBytes=${sizeBytes.coerceAtLeast(0L)}"
        val cover = coverFileName?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (cover == null) base else "$base&$ArgCover=$cover"
    }

    /**
     * Tool routes carry the draft and clip as query arguments. Non-tool routes
     * (Stickers) are returned unchanged until their milestone adds
     * a real screen.
     */
    fun toolRoute(route: String, projectId: String, clipId: String?): String {
        if (!isClipTool(route)) return route
        val base = "$route?$ArgProjectId=${encode(projectId)}"
        val clip = clipId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (clip == null) base else "$base&$ArgClipId=$clip"
    }

    fun clipToolPattern(route: String): String =
        "$route?$ArgProjectId={$ArgProjectId}&$ArgClipId={$ArgClipId}"

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

/**
 * One entry per approved Stage 2 mockup screen. Routes are the mockup ids, so
 * the parity test can assert that the Android graph covers all 42 screens.
 *
 * `implemented` flips to true as each milestone lands; everything else is
 * routed to an honest "coming in a later milestone" placeholder rather than
 * being hidden or paywalled.
 */
data class ZcutDestination(
    val route: String,
    val title: String,
    val featureId: String?,
    val implemented: Boolean = false
)

object ZcutDestinations {

    val all: List<ZcutDestination> = listOf(
        ZcutDestination("splash", "Zcut", null, implemented = true),
        ZcutDestination("onboarding", "Selamat datang", null, implemented = true),
        ZcutDestination("home", "Home", "F01", implemented = true),
        ZcutDestination("projects", "Draft", "F20", implemented = true),
        ZcutDestination("templates", "Template", "F21"),
        ZcutDestination("template-detail", "Detail template", "F21"),
        ZcutDestination("discover", "Discover", "F21"),
        ZcutDestination("ai-studio", "AI Studio", "F23"),
        ZcutDestination("editor", "Editor", "F01", implemented = true),
        ZcutDestination("media-picker", "Tambah media", "F02", implemented = true),
        ZcutDestination("trim", "Trim & split", "F04", implemented = true),
        ZcutDestination("speed", "Speed & curve", "F05", implemented = true),
        ZcutDestination("keyframe", "Keyframe", "F06", implemented = true),
        ZcutDestination("transition", "Transisi", "F07", implemented = true),
        ZcutDestination("filters", "Filter", "F08", implemented = true),
        ZcutDestination("adjust", "Adjust", "F08", implemented = true),
        ZcutDestination("effects", "Efek", "F09", implemented = true),
        ZcutDestination("mask", "Mask & chroma", "F10", implemented = true),
        ZcutDestination("matting", "Cutout", "F11", implemented = true),
        ZcutDestination("beauty", "Retouch", "F12"),
        ZcutDestination("stickers", "Sticker", "F13"),
        ZcutDestination("text", "Teks", "F14", implemented = true),
        ZcutDestination("captions", "Caption", "F15"),
        ZcutDestination("audio", "Audio", "F16", implemented = true),
        ZcutDestination("tts", "Text to speech", "F17", implemented = true),
        ZcutDestination("ai-music", "AI Music", "F18"),
        ZcutDestination("export", "Export", "F19", implemented = true),
        ZcutDestination("export-done", "Export selesai", "F19", implemented = true),
        ZcutDestination("drafts", "Kelola draft", "F20"),
        ZcutDestination("ai-clipper", "AI Clipper", "F22"),
        ZcutDestination("ai-agent", "Zcut Agent", "F23"),
        ZcutDestination("ai-generate", "Generate", "F24"),
        ZcutDestination("digital-human", "Digital Human", "F25"),
        ZcutDestination("camera", "Kamera", "F26"),
        ZcutDestination("photo-editor", "Photo editor", "F26"),
        ZcutDestination("share", "Bagikan", "F27"),
        ZcutDestination("profile", "Profil", "F28", implemented = true),
        ZcutDestination("settings", "Pengaturan", "F29", implemented = true),
        ZcutDestination("split-screen", "Split screen", "F30"),
        ZcutDestination("gameplay", "Gameplay", "F30"),
        ZcutDestination("live-photo", "Live photo", "F30"),
        ZcutDestination("ad-maker", "Ad Maker", "F31")
    )

    private val byRoute: Map<String, ZcutDestination> = all.associateBy { it.route }

    fun get(route: String): ZcutDestination? = byRoute[route]

    val placeholders: List<ZcutDestination> = all.filterNot { it.implemented }

    /** Routes shown in the bottom navigation bar, in order. */
    val bottomNav: List<String> = listOf(
        ZcutRoutes.Home,
        ZcutRoutes.Templates,
        ZcutRoutes.Projects,
        ZcutRoutes.AiStudio,
        ZcutRoutes.Profile
    )

    fun bottomNavTitle(route: String): String =
        when (route) {
            ZcutRoutes.Templates -> "Template"
            ZcutRoutes.Projects -> "Draft"
            ZcutRoutes.AiStudio -> "AI"
            ZcutRoutes.Profile -> "Profil"
            else -> "Home"
        }

    /** Feature requirement for a route, used by [com.zaaam.zcut.ui.components.BackendBadge]. */
    fun backendRequirement(route: String) =
        get(route)?.featureId?.let { FeatureRegistry.get(it)?.requirement }
}
