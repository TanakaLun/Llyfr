package io.github.tanakalun.llyfr.core

import android.content.Context
import io.github.tanakalun.llyfr.data.backup.BackupService
import io.github.tanakalun.llyfr.data.local.NoteDao
import io.github.tanakalun.llyfr.data.local.NotesDatabase
import io.github.tanakalun.llyfr.data.repository.NoteRepository
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 手写 DI 容器：应用级依赖的唯一创建出口。 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: NotesDatabase by lazy { NotesDatabase.build(appContext) }
    val noteDao: NoteDao by lazy { database.noteDao() }
    val noteRepository: NoteRepository by lazy {
        NoteRepository(noteDao, appContext, appScope)
    }
    val backupService: BackupService by lazy {
        BackupService(appContext, noteRepository, SettingsStore)
    }
    val errorReporter: ErrorReporter by lazy { ErrorReporter(appContext) }
}