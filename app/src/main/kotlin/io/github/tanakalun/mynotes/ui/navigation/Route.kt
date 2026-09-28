package io.github.tanakalun.mynotes.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object NotesList : Route

    @Serializable
    data class NoteEditor(val noteId: String = "") : Route

    @Serializable
    data class ChecklistEditor(val noteId: String = "") : Route

    @Serializable
    data object About : Route

    @Serializable
    data object License : Route

    @Serializable
    data class ImageViewer(val imagePath: String) : Route
}