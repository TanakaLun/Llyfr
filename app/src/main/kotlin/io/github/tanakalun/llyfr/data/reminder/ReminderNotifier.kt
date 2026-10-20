package io.github.tanakalun.llyfr.data.reminder

import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
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

    fun post(context: Context, noteId: String, title: String, style: Int) {
        ensureChannel(context)
        val useLive = style == STYLE_LIVE && Build.VERSION.SDK_INT >= 36
        val nm = context.getSystemService(NotificationManager::class.java)
        if (useLive && !nm.canPostPromotedNotifications()) return normalPost(context, noteId, title)

        val builder = NotificationCompat.Builder(context, ReminderExtras.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, noteId))
            .addAction(R.drawable.ic_notification, context.getString(R.string.mark_done), completeIntent(context, noteId))

        if (useLive) {
            // 实时动态约束：ongoing + BigText 样式 + contentTitle + 请求提升
            builder
                .setOngoing(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reminder_reached, title)))
                .setRequestPromotedOngoing(true)
        } else {
            builder
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reminder_reached, title)))
        }

        val notification = builder.build()
        // hasPromotableCharacteristics 仅框架可查；不满足则降级普通（此处不再追加逻辑，已在 canPostPromoted 层门控）
        NotificationManagerCompat.from(context).notify(notificationId(noteId), notification)
    }

    fun cancel(context: Context, noteId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(noteId))
    }

    fun notificationId(noteId: String): Int = noteId.hashCode()

    private fun normalPost(context: Context, noteId: String, title: String) {
        val builder = NotificationCompat.Builder(context, ReminderExtras.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_reached, title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.reminder_reached, title)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, noteId))
            .addAction(R.drawable.ic_notification, context.getString(R.string.mark_done), completeIntent(context, noteId))
        NotificationManagerCompat.from(context).notify(notificationId(noteId), builder.build())
    }

    private fun openAppIntent(context: Context, noteId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId(noteId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun completeIntent(context: Context, noteId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            notificationId(noteId) xor 0x7FFF,
            Intent(context, ReminderActionsReceiver::class.java)
                .setAction(ReminderExtras.ACTION_COMPLETE)
                .putExtra(ReminderExtras.EXTRA_NOTE_ID, noteId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                ReminderExtras.CHANNEL_ID,
                ReminderExtras.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)
        }
    }
}