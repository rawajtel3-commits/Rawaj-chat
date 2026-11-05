package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.Cabin

object NotificationHelper {
    const val CHANNEL_MESSAGES_ID = "cabin_messages_channel"
    const val CHANNEL_MESSAGES_NAME = "رسائل الكبائن المشفرة"
    const val CHANNEL_MESSAGES_DESC = "تنبيهات فورية عند وصول رسائل مشفرة جديدة في الكبائن الخاصة"

    const val CHANNEL_TASKS_ID = "cabin_tasks_channel"
    const val CHANNEL_TASKS_NAME = "مهام وتكليفات الكبائن"
    const val CHANNEL_TASKS_DESC = "تنبيهات عند إسناد مهام جديدة أو تحديث حالة المهام داخل الكابينة"

    // Backward compatibility alias
    const val CHANNEL_ID = CHANNEL_MESSAGES_ID
    const val CHANNEL_NAME = CHANNEL_MESSAGES_NAME
    const val CHANNEL_DESC = CHANNEL_MESSAGES_DESC

    const val EXTRA_CABIN_ID = "extra_cabin_id"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_MESSAGES_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                lightColor = Color.CYAN
            }
            notificationManager.createNotificationChannel(messagesChannel)

            // Tasks channel
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                CHANNEL_TASKS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_TASKS_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200)
                enableLights(true)
                lightColor = Color.GREEN
            }
            notificationManager.createNotificationChannel(tasksChannel)
        }
    }

    // Backward compatibility call
    fun createNotificationChannel(context: Context) {
        createNotificationChannels(context)
    }

    fun showNewMessageNotification(
        context: Context,
        cabinName: String,
        senderName: String,
        messageText: String,
        cabin: Cabin? = null,
        cabinId: String = cabin?.id ?: "",
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        if (cabin?.isDndActive() == true) {
            return
        }

        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CABIN_ID, cabinId)
            putExtra(EXTRA_TARGET_TAB, "CHAT")
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔒 $cabinName")
            .setContentText("$senderName: $messageText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🔒 $cabinName")
                    .setSummaryText(senderName)
                    .bigText(messageText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun showTaskNotification(
        context: Context,
        cabinName: String,
        taskTitle: String,
        assignee: String,
        priority: String,
        cabin: Cabin? = null,
        cabinId: String = cabin?.id ?: "",
        notificationId: Int = (System.currentTimeMillis() % 10000 + 20000).toInt()
    ) {
        if (cabin?.isDndActive() == true) {
            return
        }

        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CABIN_ID, cabinId)
            putExtra(EXTRA_TARGET_TAB, "TASKS")
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryText = "المسند إليه: $assignee [أولوية $priority]"
        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 تكليف بمهمة: $cabinName")
            .setContentText("$taskTitle • $summaryText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📋 مهمة جديدة في $cabinName")
                    .setSummaryText("أولوية: $priority")
                    .bigText("المهمة: $taskTitle\nالمسند إليه: $assignee\nالأولوية: $priority")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
