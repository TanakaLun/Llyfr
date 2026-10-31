package io.github.tanakalun.llyfr.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.tanakalun.llyfr.LlyfrApplication
import io.github.tanakalun.llyfr.data.model.NoteType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 通知「删除」动作：删除该待办、取消闹钟与通知。 */
class ReminderActionsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 兼容旧版本通知上残留的 TODO_COMPLETE action。
        if (intent.action != ReminderExtras.ACTION_DELETE && intent.action != LEGACY_ACTION_COMPLETE) return
        val noteId = intent.getStringExtra(ReminderExtras.EXTRA_NOTE_ID) ?: return
        val app = context.applicationContext as? LlyfrApplication ?: return
        val container = app.container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 只处理仍然存在的待办，避免误删同 id 的其他类型笔记。
                if (container.noteRepository.getById(noteId)?.type == NoteType.Todo) {
                    container.noteRepository.delete(noteId)
                }
                container.reminderScheduler.cancel(noteId)
                ReminderNotifier.cancel(context, noteId)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val LEGACY_ACTION_COMPLETE = "llyfr.action.TODO_COMPLETE"
    }
}
