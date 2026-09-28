package io.github.tanakalun.mynotes.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [NoteEntity::class],
    version = 1,
)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        const val DB_NAME = "notes.db"

        fun build(context: Context): NotesDatabase {
            val legacy = LegacyNotesMigrator.prepare(context)
            val db = Room.databaseBuilder<NotesDatabase>(context, DB_NAME)
                .setDriver(AndroidSQLiteDriver())
                .build()
            if (legacy != null) {
                LegacyNotesMigrator.importLegacy(context, db, legacy)
            } else {
                LegacyNotesMigrator.markMigrated(context)
            }
            return db
        }
    }
}