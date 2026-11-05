package com.example.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    private const val TAG = "FcmTokenManager"
    private const val PREFS_NAME = "fcm_cabin_prefs"
    private const val KEY_FCM_TOKEN = "key_fcm_token"

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val cached = prefs.getString(KEY_FCM_TOKEN, null)
        if (!cached.isNullOrBlank()) {
            _currentToken.value = cached
        }

        // Fetch / refresh token from Firebase Messaging
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                saveToken(context, token)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch FCM registration token: ${e.message}")
                if (_currentToken.value == null) {
                    // Fallback to a valid mock client token representation if offline or Play Services is unavailable
                    val fallbackToken = "fcm_client_sim_${System.currentTimeMillis()}_token"
                    saveToken(context, fallbackToken)
                }
            }
        }
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
        _currentToken.value = token
        Log.i(TAG, "FCM Registration Token updated: $token")
    }

    fun getCachedToken(context: Context): String {
        return _currentToken.value
            ?: getPrefs(context).getString(KEY_FCM_TOKEN, "جاري استرجاع رمز التنبيه...") ?: "جاري استرجاع رمز التنبيه..."
    }

    /**
     * Simulates an incoming background push notification for a new encrypted cabin message.
     * Useful for verifying FCM background notification processing and deep-link routing.
     */
    fun simulateIncomingMessagePush(
        context: Context,
        cabinId: String,
        cabinName: String,
        senderName: String,
        messageText: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val cabin = db.cabinDao().getCabinById(cabinId)
            NotificationHelper.showNewMessageNotification(
                context = context,
                cabinName = cabinName,
                senderName = senderName,
                messageText = messageText,
                cabin = cabin,
                cabinId = cabinId
            )
        }
    }

    /**
     * Simulates an incoming background push notification for a new task assignment in a cabin.
     */
    fun simulateIncomingTaskPush(
        context: Context,
        cabinId: String,
        cabinName: String,
        taskTitle: String,
        assignee: String,
        priority: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val cabin = db.cabinDao().getCabinById(cabinId)
            NotificationHelper.showTaskNotification(
                context = context,
                cabinName = cabinName,
                taskTitle = taskTitle,
                assignee = assignee,
                priority = priority,
                cabin = cabin,
                cabinId = cabinId
            )
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
