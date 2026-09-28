package io.github.tanakalun.mynotes.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.github.tanakalun.mynotes.data.crypto.NoteCipher
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * 旧 SQLiteOpenHelper 库（notes.db，版本 1~4）的一次性 ETL 迁移。
 *
 * 策略：把旧库原地改名 `.pre_room`，让 Room 以全新空库建表，再逐行读取旧库
 * 并将字段归一化到 v1 加密格式后写入。任何一步失败都不置位迁移标志，下次启动重试；
 * 原始 `.pre_room` 始终保留作为数据兜底。
 */
object LegacyNotesMigrator {

    private const val PREFS_NAME = "mynotes_migration"
    private const val KEY_MIGRATED = "room_migrated_v1"
    private const val PRE_ROOM_SUFFIX = ".pre_room"

    fun markMigrated(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    fun isMigrated(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MIGRATED, false)

    /** 迁移前准备：改名旧库。返回遗留文件；无遗留则返回 null。 */
    fun prepare(context: Context): File? {
        if (isMigrated(context)) return null
        val dbFile = context.getDatabasePath(NotesDatabase.DB_NAME)
        if (!dbFile.exists()) return null
        val legacy = File(dbFile.parentFile, dbFile.name + PRE_ROOM_SUFFIX)
        if (legacy.exists()) legacy.delete()
        if (!dbFile.renameTo(legacy)) return null
        return legacy
    }

    fun importLegacy(context: Context, db: NotesDatabase, legacy: File) {
        val cipher = NoteCipher()
        val codec = ChecklistCodec(cipher)
        val dao = db.noteDao()
        try {
            val entities = readLegacyRows(legacy, cipher, codec)
            if (entities.isNotEmpty()) {
                runBlocking { dao.replaceAll(entities) }
            }
            markMigrated(context)
        } catch (_: Exception) {
            // 保留 .pre_room，不置位标志，下次重试
        }
    }

    private fun readLegacyRows(legacy: File, cipher: NoteCipher, codec: ChecklistCodec): List<NoteEntity> {
        val list = mutableListOf<NoteEntity>()
        val db = SQLiteDatabase.openDatabase(legacy.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            val cursor = db.rawQuery("SELECT * FROM notes", null)
            cursor.use {
                while (it.moveToNext()) {
                    val entity = rowToEntity(it, cipher, codec)
                    if (entity != null) list.add(entity)
                }
            }
        }
        return list
    }

    private fun rowToEntity(
        c: android.database.Cursor,
        cipher: NoteCipher,
        codec: ChecklistCodec,
    ): NoteEntity? {
        fun col(name: String): Int = c.getColumnIndex(name)
        fun str(name: String): String = if (col(name) >= 0) c.getString(col(name)) ?: "" else ""
        fun int(name: String): Int = if (col(name) >= 0) c.getInt(col(name)) else 0
        fun long(name: String): Long = if (col(name) >= 0) c.getLong(col(name)) else 0L
        fun bool(name: String): Boolean = int(name) != 0

        var unreadable = false

        fun normalizeField(stored: String): String {
            if (stored.isEmpty()) return stored
            return if (!cipher.isEncrypted(stored)) {
                try {
                    cipher.encrypt(cipher.decrypt(stored), enabled = true)
                } catch (_: Exception) {
                    unreadable = true
                    if (stored.isNotEmpty() && stored.all { it.isLetterOrDigit() || it == '/' || it == '+' || it == '=' }) {
                        // 旧裸密文已无法解密，打 v0 前缀标记，避免被误判为明文展示
                        NoteCipher.PREFIX_V0 + stored
                    } else {
                        stored
                    }
                }
            } else {
                stored
            }
        }

        val titleStored = normalizeField(str("title"))
        val contentStored = normalizeField(str("content"))
        val checklistJson = str("checklist")
        val (checklistStored, checklistUnreadable) = if (checklistJson.isBlank() || checklistJson == "[]") {
            "[]" to false
        } else {
            codec.migrate(checklistJson, cipher)
        }
        val imagesJson = str("images")

        return NoteEntity(
            id = str("id"),
            type = int("type"),
            title = titleStored,
            content = contentStored,
            checklist = checklistStored,
            colorIndex = int("color_index"),
            images = imagesJson,
            pinned = int("pinned"),
            createdAt = long("created_at"),
            updatedAt = long("updated_at"),
            unreadable = if (unreadable || checklistUnreadable) 1 else 0,
        )
    }
}
