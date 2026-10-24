package com.zaaam.zcut.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.ui.theme.ZcutCyan
import com.zaaam.zcut.ui.theme.ZcutLime
import kotlinx.coroutines.delay

/**
 * Brand splash. The system splash (Theme.Zcut.Splash) shows first; this keeps
 * the same background while the graph and stored preferences warm up.
 *
 * [ready] lets the host hold the splash until preferences have been read, so a
 * slow disk can never route a returning user through onboarding again.
 */
@Composable
fun ZcutSplashScreen(
    onDone: () -> Unit,
    ready: Boolean = true
) {
    var minimumElapsed by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(700)
        minimumElapsed = true
    }
    LaunchedEffect(minimumElapsed, ready) {
        if (minimumElapsed && ready && !fired) {
            fired = true
            onDone()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ZcutLime.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "zcut.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "studio video lokal",
                style = MaterialTheme.typography.labelMedium,
                color = ZcutCyan
            )
        }
    }
}
