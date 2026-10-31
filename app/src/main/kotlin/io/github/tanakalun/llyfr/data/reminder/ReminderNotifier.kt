package io.github.tanakalun.llyfr.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.tanakalun.llyfr.MainActivity
import io.github.tanakalun.llyfr.R

/**
 * 待办提醒通知：普通样式 / Android 16+ 实时动态（Live Update）。
 * 实时样式仅在 API≥36 且 `canPostPromotedNotifications()` 成立时启用，否则自动降级普通通知。
 */
object ReminderNotifier {

    const val STYLE_NORMAL = 0
    const val STYLE_LIVE = 1

    private const val LOG_TAG = "ReminderNotifier"

    /** 通知以 noteId 作为 tag，id 固定，避免 hashCode 碰撞导致两条提醒互相覆盖。 */
    private const val NOTIFICATION_ID = 0

    private fun tag(noteId: String): String = "llyfr.reminder:$noteId"

    /** 通知权限 / 用户是否关闭了本应用通知。 */
    fun hasPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(context: Context, noteId: String, title: String, style: Int): Boolean {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.w(LOG_TAG, "通知未开启，提醒 $noteId 未显示")
            return false
        }

        val text = context.getString(R.string.reminder_reached, title)
        val useLive = style == STYLE_LIVE &&
            Build.VERSION.SDK_INT >= 36 &&
            context.getSystemService(NotificationManager::class.java)?.canPostPromotedNotifications() == true

        val builder = NotificationCompat.Builder(context, ReminderExtras.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, noteId))
            .addAction(R.drawable.ic_notification, context.getString(R.string.mark_done), deleteIntent(context, noteId))

        if (useLive) {
            // 实时动态约束：ongoing + BigText 样式 + contentTitle。
            builder
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
        } else {
            builder.setAutoCancel(true)
        }

        return try {
            manager.notify(tag(noteId), NOTIFICATION_ID, builder.build())
            true
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "缺少通知权限，提醒 $noteId 未显示", e)
            false
        }
    }

    fun cancel(context: Context, noteId: String) {
        NotificationManagerCompat.from(context).cancel(tag(noteId), NOTIFICATION_ID)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ReminderExtras.CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            ReminderExtras.CHANNEL_ID,
            context.getString(R.string.channel_todo_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_todo_reminders_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun openAppIntent(context: Context, noteId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            Intent(context, MainActivity::class.java)
                .setData(ReminderScheduler.reminderUri(noteId))
                .putExtra(ReminderExtras.EXTRA_NOTE_ID, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun deleteIntent(context: Context, noteId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            Intent(context, ReminderActionsReceiver::class.java)
                .setData(ReminderScheduler.reminderUri(noteId))
                .setAction(ReminderExtras.ACTION_DELETE)
                .putExtra(ReminderExtras.EXTRA_NOTE_ID, noteId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
