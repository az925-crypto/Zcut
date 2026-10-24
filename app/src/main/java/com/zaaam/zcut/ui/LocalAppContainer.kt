package com.zaaam.zcut.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.feature.home.HomeViewModel
import com.zaaam.zcut.feature.profile.ProfileViewModel
import com.zaaam.zcut.feature.projects.ProjectsViewModel
import com.zaaam.zcut.feature.settings.SettingsViewModel

/**
 * Manual dependency injection, in the same spirit as [AppContainer]: one
 * composition local and one factory, no DI framework.
 */
val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the UI in ZcutApp().")
}

@Composable
fun zcutViewModelFactory(container: AppContainer): ViewModelProvider.Factory = remember(container) {
    viewModelFactory {
        initializer { HomeViewModel(container.projectRepository) }
        initializer { ProjectsViewModel(container.projectRepository) }
        initializer {
            ProfileViewModel(
                repository = container.projectRepository,
                storage = container.storage
            )
        }
        initializer {
            SettingsViewModel(
                preferencesRepository = container.preferencesRepository,
                repository = container.projectRepository,
                storage = container.storage
            )
        }
    }
}
