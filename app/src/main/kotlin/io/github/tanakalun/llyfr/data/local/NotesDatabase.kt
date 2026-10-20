package io.github.tanakalun.llyfr.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [NoteEntity::class],
    version = 2,
)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        const val DB_NAME = "notes.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare("ALTER TABLE notes ADD COLUMN reminder_at INTEGER NOT NULL DEFAULT 0").step()
            }
        }

        fun build(context: Context): NotesDatabase {
            val legacy = LegacyNotesMigrator.prepare(context)
            val db = Room.databaseBuilder<NotesDatabase>(context, DB_NAME)
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(MIGRATION_1_2)
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