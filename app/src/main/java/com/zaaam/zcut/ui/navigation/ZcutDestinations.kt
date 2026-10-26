package com.zaaam.zcut.ui.navigation

import com.zaaam.zcut.core.config.FeatureRegistry

/** Route constants for the destinations referenced from code. */
object ZcutRoutes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Projects = "projects"
    const val Drafts = "drafts"
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
    const val Captions = "captions"
    const val Effects = "effects"
    const val Filters = "filters"
    const val Adjust = "adjust"
    const val Mask = "mask"
    const val Matting = "matting"
    const val Beauty = "beauty"
    const val Export = "export"
    const val ExportDone = "export-done"
    const val Share = "share"
    const val SplitScreen = "split-screen"
    const val Gameplay = "gameplay"
    const val LivePhoto = "live-photo"
    const val Templates = "templates"
    const val TemplateDetail = "template-detail"
    const val Discover = "discover"
    const val AiStudio = "ai-studio"
    const val AiClipper = "ai-clipper"
    const val AiAgent = "ai-agent"
    const val AiMusic = "ai-music"
    const val AdMaker = "ad-maker"
    const val AiGenerate = "ai-generate"
    const val DigitalHuman = "digital-human"
    const val Camera = "camera"
    const val PhotoEditor = "photo-editor"
    const val Profile = "profile"
    const val Settings = "settings"

    /** NavHost pattern; the destination id itself stays `editor` for parity. */
    const val EditorPattern = "editor/{projectId}"

    const val ArgProjectId = "projectId"
    const val ArgTemplateId = "templateId"

    /**
     * `projectId` is absent when the picker starts a brand-new draft and
     * `templateId` is absent when the import is not filling a template's slots;
     * both are optional query arguments.
     */
    const val MediaPickerPattern = "$MediaPicker?$ArgProjectId={$ArgProjectId}" +
        "&$ArgTemplateId={$ArgTemplateId}"

    /** A template detail is addressed by its catalogue id, nothing else. */
    const val TemplateDetailPattern = "$TemplateDetail?$ArgTemplateId={$ArgTemplateId}"

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

    /** Stickers can create a clip too, so they carry the selected clip and playhead. */
    const val StickersPattern = "$Stickers?$ArgProjectId={$ArgProjectId}" +
        "&$ArgClipId={$ArgClipId}&$ArgAtMs={$ArgAtMs}"

    /** Captions belong to the draft, not a clip; the playhead anchors new cues. */
    const val CaptionsPattern = "$Captions?$ArgProjectId={$ArgProjectId}" +
        "&$ArgAtMs={$ArgAtMs}"

    /** AI Music renders a loop for the draft; the playhead anchors the new clip. */
    const val AiMusicPattern = "$AiMusic?$ArgProjectId={$ArgProjectId}" +
        "&$ArgAtMs={$ArgAtMs}"

    /**
     * The AI Studio hub opens the agent with no draft, so its `projectId` is
     * optional: without one the agent targets the most recent draft.
     */
    const val AiAgentPattern = "$AiAgent?$ArgProjectId={$ArgProjectId}"

    /** Export renders one draft; the result screen is a separate destination. */
    const val ExportPattern = "$Export?$ArgProjectId={$ArgProjectId}"

    /**
     * The photo editor's `file` is a bare name inside the app's photo folder
     * (a captured photo); without it the editor asks for one.
     */
    const val PhotoEditorPattern = "$PhotoEditor?$ArgFile={$ArgFile}"

    /** The share screen only needs the finished file's name in the export folder. */
    const val SharePattern = "$Share?$ArgFile={$ArgFile}"

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
     *
     * Every route registered with `clipTool(...)` in the NavHost must be listed
     * here, otherwise the editor navigates to the bare route and the panel can
     * never open (regression covered by `ZcutDestinationsTest`).
     */
    private val argToolRoutes: Set<String> =
        setOf(Trim, Speed, Keyframe, Transition, Filters, Adjust, Effects, Mask, Matting, Beauty)

    fun isClipTool(route: String): Boolean = route in argToolRoutes

    fun editor(projectId: String): String = "$Editor/${encode(projectId)}"

    fun mediaPicker(projectId: String? = null, templateId: String? = null): String {
        val project = projectId?.takeIf { it.isNotBlank() }?.let(::encode)
        val template = templateId?.takeIf { it.isNotBlank() }?.let(::encode)
        return when {
            project == null && template == null -> MediaPicker
            project == null -> "$MediaPicker?$ArgTemplateId=$template"
            template == null -> "$MediaPicker?$ArgProjectId=$project"
            else -> "$MediaPicker?$ArgProjectId=$project&$ArgTemplateId=$template"
        }
    }

    fun templateDetail(templateId: String): String =
        "$TemplateDetail?$ArgTemplateId=${encode(templateId)}"

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

    fun stickers(projectId: String, clipId: String?, atMs: Long = 0L): String {
        val base = "$Stickers?$ArgProjectId=${encode(projectId)}" +
            "&$ArgAtMs=${atMs.coerceAtLeast(0L)}"
        val clip = clipId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (clip == null) base else "$base&$ArgClipId=$clip"
    }

    fun captions(projectId: String, atMs: Long = 0L): String =
        "$Captions?$ArgProjectId=${encode(projectId)}&$ArgAtMs=${atMs.coerceAtLeast(0L)}"

    fun aiMusic(projectId: String? = null, atMs: Long = 0L): String {
        val at = atMs.coerceAtLeast(0L)
        val id = projectId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (id == null) {
            "$AiMusic?$ArgAtMs=$at"
        } else {
            "$AiMusic?$ArgProjectId=$id&$ArgAtMs=$at"
        }
    }

    /** No draft id: the hub opened the agent standalone. */
    fun aiAgent(projectId: String? = null): String {
        val id = projectId?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (id == null) AiAgent else "$AiAgent?$ArgProjectId=$id"
    }

    fun export(projectId: String): String = "$Export?$ArgProjectId=${encode(projectId)}"

    /** Photo editor with an optional captured-photo name. */
    fun photoEditor(fileName: String? = null): String {
        val file = fileName?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (file == null) PhotoEditor else "$PhotoEditor?$ArgFile=$file"
    }

    /** Share screen for one finished export; without a file it shows an empty state. */
    fun share(fileName: String? = null): String {
        val file = fileName?.takeIf { it.isNotBlank() }?.let(::encode)
        return if (file == null) Share else "$Share?$ArgFile=$file"
    }

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
     * Tool routes carry the draft and clip as query arguments. Routes with their
     * own builder (text/audio/tts/stickers, which can create a clip) return the
     * bare route here; they are not clip tools.
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
        ZcutDestination("templates", "Template", "F21", implemented = true),
        ZcutDestination("template-detail", "Detail template", "F21", implemented = true),
        ZcutDestination("discover", "Discover", "F21", implemented = true),
        ZcutDestination("ai-studio", "AI Studio", "F23", implemented = true),
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
        ZcutDestination("beauty", "Retouch", "F12", implemented = true),
        ZcutDestination("stickers", "Sticker", "F13", implemented = true),
        ZcutDestination("text", "Teks", "F14", implemented = true),
        ZcutDestination("captions", "Caption", "F15", implemented = true),
        ZcutDestination("audio", "Audio", "F16", implemented = true),
        ZcutDestination("tts", "Text to speech", "F17", implemented = true),
        ZcutDestination("ai-music", "AI Music", "F18", implemented = true),
        ZcutDestination("export", "Export", "F19", implemented = true),
        ZcutDestination("export-done", "Export selesai", "F19", implemented = true),
        ZcutDestination("drafts", "Kelola draft", "F20", implemented = true),
        ZcutDestination("ai-clipper", "AI Clipper", "F22", implemented = true),
        ZcutDestination("ai-agent", "Zcut Agent", "F23", implemented = true),
        ZcutDestination("ai-generate", "Generate", "F24", implemented = true),
        ZcutDestination("digital-human", "Digital Human", "F25", implemented = true),
        ZcutDestination("camera", "Kamera", "F26", implemented = true),
        ZcutDestination("photo-editor", "Photo editor", "F26", implemented = true),
        ZcutDestination("share", "Bagikan", "F27", implemented = true),
        ZcutDestination("profile", "Profil", "F28", implemented = true),
        ZcutDestination("settings", "Pengaturan", "F29", implemented = true),
        ZcutDestination("split-screen", "Split screen", "F30", implemented = true),
        ZcutDestination("gameplay", "Gameplay", "F30", implemented = true),
        ZcutDestination("live-photo", "Live photo", "F30", implemented = true),
        ZcutDestination("ad-maker", "Ad Maker", "F31", implemented = true)
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
