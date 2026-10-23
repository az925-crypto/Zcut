package com.zaaam.zcut.feature.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zaaam.zcut.ui.components.BackendBadge
import com.zaaam.zcut.ui.components.EmptyState
import com.zaaam.zcut.ui.components.ZcutBottomNav
import com.zaaam.zcut.ui.components.ZcutIconButton
import com.zaaam.zcut.ui.components.ZcutScaffold
import com.zaaam.zcut.ui.components.ZcutTopBar
import com.zaaam.zcut.ui.navigation.ZcutDestination
import com.zaaam.zcut.ui.navigation.ZcutDestinations

/**
 * Honest stand-in for a screen whose milestone has not landed yet. Nothing is
 * locked or paywalled: the copy says when it will be built, not that it costs.
 */
@Composable
fun PlaceholderScreen(
    destination: ZcutDestination,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val isTab = destination.route in ZcutDestinations.bottomNav

    ZcutScaffold(
        topBar = {
            ZcutTopBar(
                title = destination.title,
                subtitle = destination.featureId?.let { "$it · segera di tahap ini" } ?: "Segera di tahap ini",
                navigationIcon = if (isTab) {
                    null
                } else {
                    ({ ZcutIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack
                    ) })
                },
                actions = { BackendBadge(featureId = destination.featureId) }
            )
        },
        bottomBar = if (isTab) {
            { ZcutBottomNav(currentRoute = destination.route, onNavigate = onNavigate) }
        } else {
            null
        }
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                title = "Layar ${destination.title}",
                message = "Layar ini dibangun pada milestone berikutnya. Tidak ada fitur yang dikunci, " +
                    "jadi begitu selesai langsung tersedia penuh."
            )
        }
    }
}
