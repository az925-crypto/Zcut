package com.zaaam.zcut

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zaaam.zcut.ui.navigation.ZcutNavHost
import com.zaaam.zcut.ui.theme.ZcutTheme

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
            ZcutTheme {
                ZcutNavHost(container = container)
            }
        }
    }
}
