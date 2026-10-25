package com.zaaam.zcut.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zaaam.zcut.core.AppContainer
import com.zaaam.zcut.feature.editor.EditorViewModel
import com.zaaam.zcut.feature.home.HomeViewModel
import com.zaaam.zcut.feature.media.MediaPickerViewModel
import com.zaaam.zcut.feature.profile.ProfileViewModel
import com.zaaam.zcut.feature.projects.ProjectsViewModel
import com.zaaam.zcut.feature.settings.SettingsViewModel
import com.zaaam.zcut.feature.tools.ClipToolViewModel

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

/** Editor is scoped to one draft, so its factory carries the id. */
@Composable
fun editorViewModelFactory(
    container: AppContainer,
    projectId: String
): ViewModelProvider.Factory = remember(container, projectId) {
    viewModelFactory {
        initializer {
            EditorViewModel(
                projectId = projectId,
                repository = container.projectRepository,
                previewEngine = container.newPreviewEngine()
            )
        }
    }
}

/** Clip tool panels share one ViewModel; the key includes the route and clip. */
@Composable
fun clipToolViewModelFactory(
    container: AppContainer,
    projectId: String,
    clipId: String?
): ViewModelProvider.Factory = remember(container, projectId, clipId) {
    viewModelFactory {
        initializer {
            ClipToolViewModel(
                projectId = projectId,
                clipId = clipId,
                repository = container.projectRepository
            )
        }
    }
}

/** `projectId` is null when the picker starts a new draft. */
@Composable
fun mediaPickerViewModelFactory(
    container: AppContainer,
    projectId: String?
): ViewModelProvider.Factory = remember(container, projectId) {
    viewModelFactory {
        initializer {
            MediaPickerViewModel(
                projectId = projectId,
                repository = container.projectRepository,
                importer = container.mediaImporter
            )
        }
    }
}
