package io.github.tanakalun.mynotes.data.model

import java.util.UUID

enum class NoteType { Note, Checklist }

data class CheckItem(
    val id: String,
    val text: String,
    val checked: Boolean = false,
)

data class Note(
    val id: String,
    val type: NoteType = NoteType.Note,
    val title: String,
    val content: String,
    val checklist: List<CheckItem> = emptyList(),
    val colorIndex: Int = 0,
    val images: List<String> = emptyList(),
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val unreadable: Boolean = false,
) {
    companion object {
        const val COLOR_DEFAULT = 0

        val COLORS = listOf(
            0xFFFF5252,
            0xFFFFD740,
            0xFF69F0AE,
            0xFF40C4FF,
            0xFFB388FF,
        )

        fun colorHexOrNull(colorIndex: Int): Long? =
            COLORS.getOrNull(colorIndex - 1)

        fun createNote(title: String, content: String, colorIndex: Int = COLOR_DEFAULT, images: List<String> = emptyList()): Note {
            val now = System.currentTimeMillis()
            return Note(
                id = UUID.randomUUID().toString(),
                type = NoteType.Note,
                title = title,
                content = content,
                colorIndex = colorIndex,
                images = images,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun createChecklist(title: String, colorIndex: Int = COLOR_DEFAULT): Note {
            val now = System.currentTimeMillis()
            return Note(
                id = UUID.randomUUID().toString(),
                type = NoteType.Checklist,
                title = title,
                content = "",
                checklist = listOf(CheckItem(id = UUID.randomUUID().toString(), text = "")),
                colorIndex = colorIndex,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
