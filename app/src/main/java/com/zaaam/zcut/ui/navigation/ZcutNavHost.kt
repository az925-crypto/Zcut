package com.zaaam.zcut.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.core.data.AppPreferences
import com.zaaam.zcut.feature.home.HomeScreen
import com.zaaam.zcut.feature.onboarding.OnboardingScreen
import com.zaaam.zcut.feature.placeholder.PlaceholderScreen
import com.zaaam.zcut.feature.profile.ProfileScreen
import com.zaaam.zcut.feature.projects.ProjectsScreen
import com.zaaam.zcut.feature.settings.SettingsScreen
import com.zaaam.zcut.feature.splash.ZcutSplashScreen
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
        // non-tab screen (Settings) from a tab must NOT pop that tab, or "back"
        // would land on Home instead of where the user came from.
        val openFromProfile: (String) -> Unit = remember(navController) {
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
                HomeScreen(currentRoute = currentRoute, onNavigate = navigateToTab)
            }

            composable(ZcutRoutes.Projects) {
                ProjectsScreen(currentRoute = currentRoute, onNavigate = navigateToTab)
            }

            composable(ZcutRoutes.Profile) {
                ProfileScreen(currentRoute = currentRoute, onNavigate = openFromProfile)
            }

            composable(ZcutRoutes.Settings) {
                SettingsScreen(onBack = { navController.popBackStack() })
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
