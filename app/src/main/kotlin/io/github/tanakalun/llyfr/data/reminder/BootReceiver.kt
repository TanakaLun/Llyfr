package io.github.tanakalun.llyfr.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.tanakalun.llyfr.LlyfrApplication
import io.github.tanakalun.llyfr.data.model.NoteType
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking

/** 开机后重排未过期的待办提醒（AlarmManager 闹钟不会跨重启保留）。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        try {
            val container = (context.applicationContext as LlyfrApplication).container
            runBlocking {
                val now = System.currentTimeMillis()
                container.noteRepository.getAllOnce()
                    .filter { it.type == NoteType.Todo && it.reminderAt > now }
                    .forEach {
                        container.reminderScheduler.schedule(
                            it.id, it.title, it.reminderAt, SettingsStore.reminderNotifStyle,
                        )
                    }
            }
        } finally {
            pending.finish()
        }
    }
}