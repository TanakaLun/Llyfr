package io.github.tanakalun.llyfr.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/** 统一放 Intent extra 键名与通知频道名。 */
object ReminderExtras {
    const val ACTION_DELETE = "llyfr.action.TODO_DELETE"
    const val EXTRA_NOTE_ID = "llyfr.extra.NOTE_ID"
    const val EXTRA_TITLE = "llyfr.extra.TITLE"
    const val EXTRA_STYLE = "llyfr.extra.STYLE"

    const val CHANNEL_ID = "todo_reminders"
}

/**
 * 待办提醒调度：AlarmManager 精确闹钟（不可用精确闹钟时降级 setWindow）。
 * 触发时由 [ReminderReceiver] 发通知，删除由 [ReminderActionsReceiver] 处理。
 */
class ReminderScheduler(private val appContext: Context) {

    fun schedule(noteId: String, title: String, remindAt: Long, style: Int) {
        val am = appContext.getSystemService(AlarmManager::class.java)
        cancel(noteId)
        if (remindAt <= System.currentTimeMillis()) return
        val pi = alarmPendingIntent(noteId, title, style)
        try {
            if (canScheduleExactAlarms(appContext)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
            } else {
                // 注意：setWindow 的第二个参数是绝对触发时间（UTC 毫秒），不是相对延迟。
                am.setWindow(AlarmManager.RTC_WAKEUP, remindAt, WINDOW_LENGTH_MS, pi)
            }
        } catch (_: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, remindAt, WINDOW_LENGTH_MS, pi)
        }
    }

    fun cancel(noteId: String) {
        val pi = existingAlarmPendingIntent(noteId) ?: return
        appContext.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    private fun alarmPendingIntent(noteId: String, title: String, style: Int): PendingIntent =
        PendingIntent.getBroadcast(
            // PendingIntent 身份由 requestCode + filterEquals(Intent) 决定；用 data 携带 noteId，
            // 避免 hashCode 碰撞时两个闹钟互相覆盖。
            appContext,
            noteId.hashCode(),
            alarmIntent(noteId)
                .putExtra(ReminderExtras.EXTRA_NOTE_ID, noteId)
                .putExtra(ReminderExtras.EXTRA_TITLE, title)
                .putExtra(ReminderExtras.EXTRA_STYLE, style),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun existingAlarmPendingIntent(noteId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            noteId.hashCode(),
            alarmIntent(noteId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmIntent(noteId: String): Intent =
        Intent(appContext, ReminderReceiver::class.java)
            .setData(reminderUri(noteId))

    companion object {
        private const val WINDOW_LENGTH_MS = 60_000L

        /** Android 12+ 精确闹钟需用户在系统设置中授权，未授权时只能降级为窗口闹钟。 */
        fun canScheduleExactAlarms(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else {
                true
            }

        /** 每个待办唯一的 data URI，保证 PendingIntent 身份不冲突。 */
        fun reminderUri(noteId: String): Uri = Uri.Builder()
            .scheme("llyfr")
            .authority("reminder")
            .appendPath(noteId)
            .build()
    }
}
