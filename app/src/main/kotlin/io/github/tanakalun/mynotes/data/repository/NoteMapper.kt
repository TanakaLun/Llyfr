package io.github.tanakalun.mynotes.data.repository

import io.github.tanakalun.mynotes.data.crypto.NoteCipher
import io.github.tanakalun.mynotes.data.local.ChecklistCodec
import io.github.tanakalun.mynotes.data.local.NoteEntity
import io.github.tanakalun.mynotes.data.model.Note
import io.github.tanakalun.mynotes.data.model.NoteType
import org.json.JSONArray

/** Note 领域对象 ⇄ NoteEntity 存储对象的双向映射（含字段级加解密）。 */
class NoteMapper(
    private val cipher: NoteCipher,
    private val codec: ChecklistCodec,
) {

    fun entityToNote(e: NoteEntity): Note {
        var unreadable = e.unreadable != 0

        fun decryptField(stored: String): String {
            if (stored.isEmpty()) return ""
            if (!cipher.isEncrypted(stored)) return stored
            return try {
                cipher.decrypt(stored)
            } catch (_: Exception) {
                unreadable = true
                ""
            }
        }

        val (items, itemsUnreadable) = codec.decode(e.checklist)

        return Note(
            id = e.id,
            type = NoteType.entries.getOrElse(e.type) { NoteType.Note },
            title = decryptField(e.title),
            content = decryptField(e.content),
            checklist = items,
            colorIndex = e.colorIndex,
            images = decodeImages(e.images),
            pinned = e.pinned != 0,
            createdAt = e.createdAt,
            updatedAt = e.updatedAt,
            unreadable = unreadable || itemsUnreadable,
        )
    }

    fun noteToEntity(note: Note, encryptionEnabled: Boolean): NoteEntity = NoteEntity(
        id = note.id,
        type = note.type.ordinal,
        title = cipher.encrypt(note.title, encryptionEnabled),
        content = cipher.encrypt(note.content, encryptionEnabled),
        checklist = codec.encode(note.checklist, encryptionEnabled),
        colorIndex = note.colorIndex,
        images = encodeImages(note.images),
        pinned = if (note.pinned) 1 else 0,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
        unreadable = 0,
    )
}

private fun encodeImages(paths: List<String>): String {
    val arr = JSONArray()
    paths.forEach { arr.put(it) }
    return arr.toString()
}

private fun decodeImages(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i -> arr.optString(i, null) }
}