package io.github.tanakalun.llyfr.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.tanakalun.llyfr.LlyfrApplication
import kotlinx.coroutines.runBlocking

/** 通知「完成」动作：删除该待办、取消闹钟与通知。 */
class ReminderActionsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderExtras.ACTION_COMPLETE) return
        val noteId = intent.getStringExtra(ReminderExtras.EXTRA_NOTE_ID) ?: return
        val pending = goAsync()
        try {
            val container = (context.applicationContext as LlyfrApplication).container
            runBlocking { container.noteRepository.delete(noteId) }
            container.reminderScheduler.cancel(noteId)
            ReminderNotifier.cancel(context, noteId)
        } finally {
            pending.finish()
        }
    }
}