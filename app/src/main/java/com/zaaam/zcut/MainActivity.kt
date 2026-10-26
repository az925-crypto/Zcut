package com.zaaam.zcut

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaaam.zcut.core.data.AppPreferences
import com.zaaam.zcut.core.data.ThemeMode
import com.zaaam.zcut.core.i18n.LocalZcutLanguage
import com.zaaam.zcut.ui.navigation.ZcutNavHost
import com.zaaam.zcut.ui.theme.ZcutTheme
import com.zaaam.zcut.ui.theme.systemPrefersDark

/**
 * Single-activity Compose host. The whole Stage 3 UI lives behind
 * [ZcutNavHost]; the app never talks to a first-party server.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val container = (application as ZcutApplication).container

        setContent {
            val preferences by remember(container) {
                container.preferencesRepository.preferences
            }.collectAsStateWithLifecycle(initialValue = AppPreferences())

            val darkTheme = when (preferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemPrefersDark()
            }

            // Bar icons must follow the effective theme, not the system one.
            LaunchedEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            CompositionLocalProvider(LocalZcutLanguage provides preferences.language) {
                ZcutTheme(darkTheme = darkTheme) {
                    ZcutNavHost(container = container)
                }
            }
        }
    }
}
