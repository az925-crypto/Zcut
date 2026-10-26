package com.zaaam.zcut.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.core.data.AppPreferences
import com.zaaam.zcut.feature.ai.AgentScreen
import com.zaaam.zcut.feature.ai.AiClipperScreen
import com.zaaam.zcut.feature.ai.AiStudioScreen
import com.zaaam.zcut.feature.editor.EditorScreen
import com.zaaam.zcut.feature.export.ExportDoneScreen
import com.zaaam.zcut.feature.export.ExportScreen
import com.zaaam.zcut.feature.home.HomeScreen
import com.zaaam.zcut.feature.media.MediaPickerScreen
import com.zaaam.zcut.feature.onboarding.OnboardingScreen
import com.zaaam.zcut.feature.placeholder.PlaceholderScreen
import com.zaaam.zcut.feature.profile.ProfileScreen
import com.zaaam.zcut.feature.projects.ProjectsScreen
import com.zaaam.zcut.feature.settings.SettingsScreen
import com.zaaam.zcut.feature.templates.DiscoverScreen
import com.zaaam.zcut.feature.templates.TemplateDetailScreen
import com.zaaam.zcut.feature.templates.TemplatesScreen
import com.zaaam.zcut.feature.splash.ZcutSplashScreen
import com.zaaam.zcut.feature.tools.AdjustScreen
import com.zaaam.zcut.feature.tools.AiMusicScreen
import com.zaaam.zcut.feature.tools.AudioScreen
import com.zaaam.zcut.feature.tools.BeautyScreen
import com.zaaam.zcut.feature.tools.EffectsScreen
import com.zaaam.zcut.feature.tools.FiltersScreen
import com.zaaam.zcut.feature.tools.KeyframeScreen
import com.zaaam.zcut.feature.tools.MaskScreen
import com.zaaam.zcut.feature.tools.MattingScreen
import com.zaaam.zcut.feature.tools.SpeedScreen
import com.zaaam.zcut.feature.tools.CaptionScreen
import com.zaaam.zcut.feature.tools.StickerScreen
import com.zaaam.zcut.feature.tools.TextScreen
import com.zaaam.zcut.feature.tools.TransitionScreen
import com.zaaam.zcut.feature.tools.TrimScreen
import com.zaaam.zcut.feature.tools.TtsScreen
import com.zaaam.zcut.ui.LocalAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ZcutNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        val scope = rememberCoroutineScope()

        // null until the first stored emission: the splash waits for it so a slow
        // disk can never send a returning user through onboarding again.
        val preferences by produceState<AppPreferences?>(initialValue = null) {
            // A corrupt/typed-wrong store or a hung read must not trap the user
            // on the splash, so fall back to defaults after a short wait.
            value = withTimeoutOrNull(3_000) {
                runCatching { container.preferencesRepository.preferences.first() }.getOrNull()
            } ?: AppPreferences()
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ZcutRoutes.Home

        // Stable identity: reading the current route inside the lambda keeps
        // NavHost from rebuilding its 42-node graph on every navigation.
        val navigateToTab: (String) -> Unit = remember(navController) {
            { route ->
                val current = navController.currentBackStackEntry?.destination?.route
                if (route != current) {
                    navController.navigate(route) {
                        // Home is the tab anchor: splash and onboarding are popped
                        // before Home, so findStartDestination() would be a no-op.
                        popUpTo(ZcutRoutes.Home) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        // Bottom-nav tabs save/restore state and anchor at Home. Pushing a
        // non-tab screen (Settings, Editor, a tool panel) from a tab must NOT
        // pop that tab, or "back" would land on Home instead of where the user
        // came from.
        val openRoute: (String) -> Unit = remember(navController) {
            { route ->
                if (route in ZcutDestinations.bottomNav) {
                    navigateToTab(route)
                } else {
                    navController.navigate(route) { launchSingleTop = true }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = ZcutRoutes.Splash
        ) {
            composable(ZcutRoutes.Splash) {
                ZcutSplashScreen(
                    ready = preferences != null,
                    onDone = {
                        val seen = preferences?.onboardingSeen == true
                        navController.navigate(
                            if (seen) ZcutRoutes.Home else ZcutRoutes.Onboarding
                        ) {
                            popUpTo(ZcutRoutes.Splash) { inclusive = true }
                        }
                    }
                )
            }

            composable(ZcutRoutes.Onboarding) {
                OnboardingScreen(
                    onFinish = {
                        // Persisted only after the user actually finishes, and the
                        // navigation waits for the write: if it fails, the next
                        // launch shows onboarding again instead of pretending.
                        scope.launch {
                            runCatching {
                                container.preferencesRepository.setOnboardingSeen(true)
                            }
                            navController.navigate(ZcutRoutes.Home) {
                                popUpTo(ZcutRoutes.Onboarding) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(ZcutRoutes.Home) {
                HomeScreen(currentRoute = currentRoute, onNavigate = openRoute)
            }

            composable(ZcutRoutes.Projects) {
                ProjectsScreen(currentRoute = currentRoute, onNavigate = openRoute)
            }

            composable(ZcutRoutes.Profile) {
                ProfileScreen(currentRoute = currentRoute, onNavigate = openRoute)
            }

            composable(ZcutRoutes.Settings) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = ZcutRoutes.EditorPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType }
                )
            ) { entry ->
                val projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty()
                EditorScreen(
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                    onNavigate = openRoute,
                    onAddMedia = {
                        navController.navigate(ZcutRoutes.mediaPicker(projectId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = ZcutRoutes.MediaPickerPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ZcutRoutes.ArgTemplateId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId)
                MediaPickerScreen(
                    projectId = projectId,
                    templateId = entry.arguments?.getString(ZcutRoutes.ArgTemplateId),
                    onBack = { navController.popBackStack() },
                    onOpenEditor = { id ->
                        if (projectId != null) {
                            // Returning to the editor that opened the picker; the
                            // editor observes the repository and refreshes itself.
                            navController.popBackStack()
                        } else {
                            // Pop the picker first, then push the editor on top of
                            // the tab that started the draft. Using popUpTo on the
                            // optional-arg pattern would have to resolve a route
                            // string; popping the current entry cannot misfire.
                            navController.popBackStack()
                            navController.navigate(ZcutRoutes.editor(id))
                        }
                    }
                )
            }

            clipTool(ZcutRoutes.Trim) { projectId, clipId ->
                TrimScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Speed) { projectId, clipId ->
                SpeedScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Keyframe) { projectId, clipId ->
                KeyframeScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Transition) { projectId, clipId ->
                TransitionScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Filters) { projectId, clipId ->
                FiltersScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Adjust) { projectId, clipId ->
                AdjustScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() },
                    onOpenFilters = {
                        navController.navigate(ZcutRoutes.filters(projectId, clipId))
                    }
                )
            }

            clipTool(ZcutRoutes.Effects) { projectId, clipId ->
                EffectsScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Mask) { projectId, clipId ->
                MaskScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Matting) { projectId, clipId ->
                MattingScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            clipTool(ZcutRoutes.Beauty) { projectId, clipId ->
                BeautyScreen(
                    projectId = projectId,
                    clipId = clipId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ZcutRoutes.TextPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgClipId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                TextScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
                    clipId = entry.arguments?.getString(ZcutRoutes.ArgClipId),
                    atMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ZcutRoutes.AudioPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgClipId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val audioProjectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty()
                val audioAtMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L
                AudioScreen(
                    projectId = audioProjectId,
                    clipId = entry.arguments?.getString(ZcutRoutes.ArgClipId),
                    atMs = audioAtMs,
                    onBack = { navController.popBackStack() },
                    onAddAudio = {
                        navController.navigate(ZcutRoutes.mediaPicker(audioProjectId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenTts = {
                        navController.navigate(ZcutRoutes.tts(audioProjectId, audioAtMs)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = ZcutRoutes.TtsPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                TtsScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
                    atMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ZcutRoutes.StickersPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgClipId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                StickerScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
                    clipId = entry.arguments?.getString(ZcutRoutes.ArgClipId),
                    atMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ZcutRoutes.CaptionsPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                CaptionScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
                    atMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ZcutRoutes.AiMusicPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ZcutRoutes.ArgAtMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                AiMusicScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId),
                    atMs = entry.arguments?.getLong(ZcutRoutes.ArgAtMs) ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }

            // The AI Studio hub is a bottom-nav tab, so it takes the shared
            // route opener rather than a pop.
            composable(ZcutRoutes.AiStudio) {
                AiStudioScreen(currentRoute = ZcutRoutes.AiStudio, onNavigate = openRoute)
            }

            composable(ZcutRoutes.AiClipper) {
                AiClipperScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEditor = { projectId ->
                        navController.navigate(ZcutRoutes.editor(projectId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = ZcutRoutes.AiAgentPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                AgentScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId),
                    onBack = { navController.popBackStack() },
                    onOpenEditor = { projectId ->
                        navController.navigate(ZcutRoutes.editor(projectId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenRoute = openRoute
                )
            }

            composable(
                route = ZcutRoutes.ExportPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType }
                )
            ) { entry ->
                val exportProjectId = entry.arguments
                    ?.getString(ZcutRoutes.ArgProjectId)
                    .orEmpty()
                ExportScreen(
                    projectId = exportProjectId,
                    onBack = { navController.popBackStack() },
                    onExported = { result ->
                        // Pop the finished form first, then push the result: the
                        // back stack keeps the editor underneath the result.
                        navController.popBackStack()
                        navController.navigate(
                            ZcutRoutes.exportDone(
                                projectId = exportProjectId,
                                fileName = result.fileName,
                                durationMs = result.durationMs,
                                sizeBytes = result.sizeBytes,
                                coverFileName = result.coverFileName
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = ZcutRoutes.ExportDonePattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgFile) { type = NavType.StringType },
                    navArgument(ZcutRoutes.ArgDurationMs) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(ZcutRoutes.ArgSizeBytes) {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument(ZcutRoutes.ArgCover) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                ExportDoneScreen(
                    projectId = entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
                    fileName = entry.arguments?.getString(ZcutRoutes.ArgFile).orEmpty(),
                    durationMs = entry.arguments?.getLong(ZcutRoutes.ArgDurationMs) ?: 0L,
                    sizeBytes = entry.arguments?.getLong(ZcutRoutes.ArgSizeBytes) ?: 0L,
                    coverFileName = entry.arguments?.getString(ZcutRoutes.ArgCover),
                    onBackHome = {
                        navController.navigate(ZcutRoutes.Home) {
                            popUpTo(ZcutRoutes.Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(ZcutRoutes.Templates) {
                TemplatesScreen(
                    currentRoute = ZcutRoutes.Templates,
                    onNavigate = openRoute,
                    onOpenTemplate = { templateId ->
                        navController.navigate(ZcutRoutes.templateDetail(templateId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = ZcutRoutes.TemplateDetailPattern,
                arguments = listOf(
                    navArgument(ZcutRoutes.ArgTemplateId) { type = NavType.StringType }
                )
            ) { entry ->
                TemplateDetailScreen(
                    templateId = entry.arguments?.getString(ZcutRoutes.ArgTemplateId).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onUseTemplate = { templateId ->
                        // No draft exists yet: the picker creates one from the
                        // template once the user picks the slot media.
                        navController.navigate(ZcutRoutes.mediaPicker(templateId = templateId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(ZcutRoutes.Discover) {
                DiscoverScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTemplate = { templateId ->
                        navController.navigate(ZcutRoutes.templateDetail(templateId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            ZcutDestinations.placeholders.forEach { destination ->
                composable(destination.route) {
                    PlaceholderScreen(
                        destination = destination,
                        onBack = { navController.popBackStack() },
                        onNavigate = navigateToTab
                    )
                }
            }
        }
    }
}

/**
 * Registers one clip tool panel with its `projectId`/`clipId` arguments. Keeping
 * the argument wiring in one place stops a new panel from silently reading the
 * wrong key.
 */
private fun NavGraphBuilder.clipTool(
    route: String,
    screen: @Composable (String, String?) -> Unit
) {
    composable(
        route = ZcutRoutes.clipToolPattern(route),
        arguments = listOf(
            navArgument(ZcutRoutes.ArgProjectId) { type = NavType.StringType },
            navArgument(ZcutRoutes.ArgClipId) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { entry ->
        screen(
            entry.arguments?.getString(ZcutRoutes.ArgProjectId).orEmpty(),
            entry.arguments?.getString(ZcutRoutes.ArgClipId)
        )
    }
}
