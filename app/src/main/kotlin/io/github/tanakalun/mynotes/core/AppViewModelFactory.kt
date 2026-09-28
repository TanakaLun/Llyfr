package io.github.tanakalun.mynotes.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.tanakalun.mynotes.MyNotesApplication
import io.github.tanakalun.mynotes.ui.about.LicenseViewModel
import io.github.tanakalun.mynotes.ui.editor.ChecklistViewModel
import io.github.tanakalun.mynotes.ui.editor.NoteEditorViewModel
import io.github.tanakalun.mynotes.ui.notes.NotesListViewModel
import io.github.tanakalun.mynotes.ui.settings.SettingsViewModel

/** ViewModel 工厂：按类型从 [AppContainer] 装配依赖。 */
object AppViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = requireNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val container = (app as MyNotesApplication).container
        return when {
            modelClass.isAssignableFrom(NotesListViewModel::class.java) ->
                NotesListViewModel(container.noteRepository, extras.createSavedStateHandle())

            modelClass.isAssignableFrom(NoteEditorViewModel::class.java) ->
                NoteEditorViewModel(
                    container.noteRepository,
                    container.appContext,
                    container.appScope,
                    extras.createSavedStateHandle(),
                )

            modelClass.isAssignableFrom(ChecklistViewModel::class.java) ->
                ChecklistViewModel(container.noteRepository, container.appScope)

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.backupService, container.errorReporter, container.appContext)

            modelClass.isAssignableFrom(LicenseViewModel::class.java) ->
                LicenseViewModel(container.appContext)

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}