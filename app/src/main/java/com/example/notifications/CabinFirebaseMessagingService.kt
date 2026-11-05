package com.example.notifications

import android.util.Log
import com.example.data.AppDatabase
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CabinFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CabinFcmService"
        const val TYPE_NEW_MESSAGE = "NEW_MESSAGE"
        const val TYPE_TASK_ASSIGNED = "TASK_ASSIGNED"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        FcmTokenManager.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data

        // Handle data payload
        if (data.isNotEmpty()) {
            val type = data["type"] ?: data["notification_type"] ?: ""
            val cabinId = data["cabin_id"] ?: data["cabinId"] ?: ""
            val cabinName = data["cabin_name"] ?: data["cabinName"] ?: "الكبينة المشفرة"

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val cabin = if (cabinId.isNotBlank()) db.cabinDao().getCabinById(cabinId) else null

                when (type) {
                    TYPE_TASK_ASSIGNED -> {
                        val taskTitle = data["task_title"] ?: data["title"] ?: "مهمة جديدة"
                        val assignee = data["task_assignee"] ?: data["assignee"] ?: "أنت"
                        val priority = data["task_priority"] ?: data["priority"] ?: "عالية"
                        NotificationHelper.showTaskNotification(
                            context = applicationContext,
                            cabinName = cabinName,
                            taskTitle = taskTitle,
                            assignee = assignee,
                            priority = priority,
                            cabin = cabin,
                            cabinId = cabinId
                        )
                    }

                    TYPE_NEW_MESSAGE -> {
                        val senderName = data["sender_name"] ?: data["sender"] ?: "عضو الكبينة"
                        val messageText = data["message_text"] ?: data["text"] ?: "وصلتك رسالة مشفرة جديدة"
                        NotificationHelper.showNewMessageNotification(
                            context = applicationContext,
                            cabinName = cabinName,
                            senderName = senderName,
                            messageText = messageText,
                            cabin = cabin,
                            cabinId = cabinId
                        )
                    }

                    else -> {
                        // Infer by fields if type wasn't explicitly defined
                        if (data.containsKey("task_title") || data.containsKey("task_id")) {
                            val taskTitle = data["task_title"] ?: "مهمة جديدة"
                            val assignee = data["task_assignee"] ?: "أنت"
                            val priority = data["task_priority"] ?: "عالية"
                            NotificationHelper.showTaskNotification(
                                context = applicationContext,
                                cabinName = cabinName,
                                taskTitle = taskTitle,
                                assignee = assignee,
                                priority = priority,
                                cabin = cabin,
                                cabinId = cabinId
                            )
                        } else {
                            val senderName = data["sender_name"] ?: "عضو الكبينة"
                            val messageText = data["message_text"] ?: data["body"] ?: "رسالة مشفرة جديدة"
                            NotificationHelper.showNewMessageNotification(
                                context = applicationContext,
                                cabinName = cabinName,
                                senderName = senderName,
                                messageText = messageText,
                                cabin = cabin,
                                cabinId = cabinId
                            )
                        }
                    }
                }
            }
        } else {
            // Handle standard notification payload without data
            remoteMessage.notification?.let { notification ->
                val title = notification.title ?: "تنبيه كبينة رواج"
                val body = notification.body ?: ""
                NotificationHelper.showNewMessageNotification(
                    context = applicationContext,
                    cabinName = title,
                    senderName = "شات رواج المشفر",
                    messageText = body
                )
            }
        }
    }
}
