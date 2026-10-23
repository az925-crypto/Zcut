package com.zaaam.zcut.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.feature.home.HomeScreen
import com.zaaam.zcut.feature.onboarding.OnboardingScreen
import com.zaaam.zcut.feature.placeholder.PlaceholderScreen
import com.zaaam.zcut.feature.projects.ProjectsScreen
import com.zaaam.zcut.feature.splash.ZcutSplashScreen
import com.zaaam.zcut.ui.LocalAppContainer

/**
 * In-memory launch flag. M1 replaces this with a persisted DataStore value; for
 * now onboarding shows once per process instead of on every recomposition.
 */
private object LaunchState {
    var onboardingSeen: Boolean = false
}

@Composable
fun ZcutNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    CompositionLocalProvider(LocalAppContainer provides container) {
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

        NavHost(
            navController = navController,
            startDestination = ZcutRoutes.Splash
        ) {
            composable(ZcutRoutes.Splash) {
                ZcutSplashScreen(
                    onDone = {
                        val next = if (LaunchState.onboardingSeen) {
                            ZcutRoutes.Home
                        } else {
                            LaunchState.onboardingSeen = true
                            ZcutRoutes.Onboarding
                        }
                        navController.navigate(next) {
                            popUpTo(ZcutRoutes.Splash) { inclusive = true }
                        }
                    }
                )
            }

            composable(ZcutRoutes.Onboarding) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(ZcutRoutes.Home) {
                            popUpTo(ZcutRoutes.Onboarding) { inclusive = true }
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
