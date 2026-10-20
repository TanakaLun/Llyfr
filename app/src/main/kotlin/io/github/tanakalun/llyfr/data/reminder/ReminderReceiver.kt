package io.github.tanakalun.llyfr.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 到达提醒时间：按设置样式发通知。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(ReminderExtras.EXTRA_NOTE_ID) ?: return
        val title = intent.getStringExtra(ReminderExtras.EXTRA_TITLE) ?: ""
        val style = intent.getIntExtra(ReminderExtras.EXTRA_STYLE, ReminderNotifier.STYLE_NORMAL)
        ReminderNotifier.post(context, noteId, title.ifBlank { "待办提醒" }, style)
    }
}