package io.github.tanakalun.llyfr.data.repository

import android.content.Context
import io.github.tanakalun.llyfr.data.crypto.NoteCipher
import io.github.tanakalun.llyfr.data.local.ChecklistCodec
import io.github.tanakalun.llyfr.data.local.NoteDao
import io.github.tanakalun.llyfr.data.media.ImageUtils
import io.github.tanakalun.llyfr.data.model.IMAGE_PATH_REGEX
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * 笔记仓库：Room 的 suspend/Flow 门面，负责领域对象 ⇄ 实体映射与字段加解密。
 * 搜索在已解密的内存缓存上进行，避免每敲一字触发全表解密。
 */
class NoteRepository(
    private val dao: NoteDao,
    private val appContext: Context,
    scope: CoroutineScope,
) {
    private val cipher = NoteCipher()
    private val mapper = NoteMapper(cipher, ChecklistCodec(cipher))
    private val encryptionEnabled: Boolean get() = SettingsStore.encryptNotes

    /** 已解密的全量笔记（Room invalidation 自动刷新），置顶在前、新在前。 */
    val notes: StateFlow<List<Note>> = dao.observeAll()
        .map { entities -> entities.map(mapper::entityToNote) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getById(id: String): Note? = withContext(Dispatchers.IO) {
        dao.getById(id)?.let(mapper::entityToNote)
    }

    suspend fun getAllOnce(): List<Note> = withContext(Dispatchers.Default) {
        notes.value
    }

    suspend fun search(query: String): List<Note> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext notes.value
        val q = query.lowercase()
        notes.value.filter { note ->
            note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                note.checklist.any { item -> item.text.lowercase().contains(q) }
        }
    }

    suspend fun save(note: Note) {
        val entity = mapper.noteToEntity(
            note.copy(updatedAt = System.currentTimeMillis()),
            encryptionEnabled,
        )
        withContext(Dispatchers.IO) { dao.upsert(entity) }
    }

    suspend fun saveAll(notes: List<Note>) {
        val now = System.currentTimeMillis()
        val entities = notes.map { mapper.noteToEntity(it.copy(updatedAt = now), encryptionEnabled) }
        withContext(Dispatchers.IO) { dao.upsertAll(entities) }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) { deleteWithImages(listOf(id)) }
    }

    suspend fun deleteAll(ids: Set<String>) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.IO) { deleteWithImages(ids.toList()) }
    }

    private suspend fun deleteWithImages(ids: List<String>) {
        ids.forEach { id ->
            dao.getById(id)?.let { entity ->
                val note = mapper.entityToNote(entity)
                ImageUtils.deleteImages(appContext, note.images)
                IMAGE_PATH_REGEX.findAll(note.content).forEach { match ->
                    ImageUtils.deleteImage(appContext, match.groupValues[1])
                }
            }
        }
        dao.deleteByIds(ids)
    }

    suspend fun togglePinAll(ids: Set<String>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val toFlip = dao.getByIds(ids.toList())
            val updated = toFlip.map { it.copy(pinned = if (it.pinned == 0) 1 else 0, updatedAt = now) }
            dao.upsertAll(updated)
        }
    }
}
