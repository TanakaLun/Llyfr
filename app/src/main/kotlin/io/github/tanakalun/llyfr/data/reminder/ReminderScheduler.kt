package io.github.tanakalun.llyfr.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** 统一放 Intent extra 键名与通知频道名。 */
object ReminderExtras {
    const val ACTION_COMPLETE = "llyfr.action.TODO_COMPLETE"
    const val EXTRA_NOTE_ID = "llyfr.extra.NOTE_ID"
    const val EXTRA_TITLE = "llyfr.extra.TITLE"
    const val EXTRA_STYLE = "llyfr.extra.STYLE"

    const val CHANNEL_ID = "todo_reminders"
    const val CHANNEL_NAME = "待办提醒"
}

/**
 * 待办提醒调度：AlarmManager 精确闹钟（不可用精确闹钟时降级 setWindow）。
 * 触发时由 [ReminderReceiver] 发通知，删除/完成由 [ReminderActionsReceiver] 处理。
 */
class ReminderScheduler(private val appContext: Context) {

    fun schedule(noteId: String, title: String, remindAt: Long, style: Int) {
        val am = appContext.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        cancel(noteId)
        if (remindAt <= now) return
        val pi = alarmPendingIntent(noteId, title, style)
        val triggerDelay = remindAt - now
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerDelay, 60_000L, pi)
            }
        } catch (_: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerDelay.coerceAtLeast(0), 60_000L, pi)
        }
    }

    fun cancel(noteId: String) {
        val am = appContext.getSystemService(AlarmManager::class.java)
        am.cancel(alarmPendingIntent(noteId, "", ReminderNotifier.STYLE_NORMAL))
    }

    private fun alarmPendingIntent(noteId: String, title: String, style: Int): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            noteId.hashCode(),
            Intent(appContext, ReminderReceiver::class.java)
                .putExtra(ReminderExtras.EXTRA_NOTE_ID, noteId)
                .putExtra(ReminderExtras.EXTRA_TITLE, title)
                .putExtra(ReminderExtras.EXTRA_STYLE, style),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}