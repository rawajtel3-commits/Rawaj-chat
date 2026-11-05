package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.OutboxEntry
import com.example.data.OutboxStatus
import com.example.data.OutboxType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class OutboxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val outboxDao = db.outboxDao()
        val pendingEntries = outboxDao.getPendingEntries()

        if (pendingEntries.isEmpty()) {
            return Result.success()
        }

        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("OutboxSyncWorker", "Firestore instance unavailable: ${e.localizedMessage}")
            null
        }

        var successCount = 0

        for (entry in pendingEntries) {
            try {
                outboxDao.updateOutboxEntry(entry.copy(status = OutboxStatus.SYNCING))

                if (firestore != null) {
                    val payload = JSONObject(entry.payloadJson)
                    when (entry.entityType) {
                        OutboxType.MESSAGE -> {
                            val msgMap = mapOf(
                                "id" to entry.entityId,
                                "cabinId" to entry.cabinId,
                                "text" to payload.optString("text"),
                                "senderName" to payload.optString("senderName"),
                                "cipherText" to payload.optString("cipherText"),
                                "timestamp" to entry.createdAt
                            )
                            firestore.collection("cabins")
                                .document(entry.cabinId)
                                .collection("messages")
                                .document(entry.entityId)
                                .set(msgMap, com.google.firebase.firestore.SetOptions.merge())
                                .await()
                        }
                        OutboxType.MESSAGE_REACTION -> {
                            val reactionsObj = payload.optJSONObject("reactions") ?: JSONObject()
                            val reactionsMap = mutableMapOf<String, Any>()
                            val keys = reactionsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val arr = reactionsObj.optJSONArray(key) ?: org.json.JSONArray()
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) {
                                    val u = arr.optString(i)
                                    if (u.isNotBlank()) list.add(u)
                                }
                                reactionsMap[key] = list
                            }
                            firestore.collection("cabins")
                                .document(entry.cabinId)
                                .collection("messages")
                                .document(entry.entityId)
                                .set(
                                    mapOf(
                                        "reactions" to reactionsMap,
                                        "lastReactionAt" to entry.createdAt
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .await()
                        }
                        OutboxType.ATTACHMENT -> {
                            val attachmentMap = mapOf(
                                "id" to entry.entityId,
                                "cabinId" to entry.cabinId,
                                "attachmentName" to payload.optString("attachmentName"),
                                "attachmentType" to payload.optString("attachmentType"),
                                "mimeType" to payload.optString("mimeType"),
                                "sizeBytes" to payload.optLong("sizeBytes"),
                                "sha256Digest" to payload.optString("sha256Digest"),
                                "cloudStorageRef" to payload.optString("cloudStorageRef"),
                                "encryptionIv" to payload.optString("encryptionIv"),
                                "encryptionAlgorithm" to "AES-256-GCM",
                                "uploadedAt" to entry.createdAt
                            )
                            firestore.collection("cabins")
                                .document(entry.cabinId)
                                .collection("attachments")
                                .document(entry.entityId)
                                .set(attachmentMap, com.google.firebase.firestore.SetOptions.merge())
                                .await()

                            // Also update message upload status in Room
                            val messageDao = db.messageDao()
                            val msg = messageDao.getMessageById(entry.entityId)
                            if (msg != null) {
                                messageDao.updateMessage(
                                    msg.copy(
                                        uploadStatus = "SYNCED_CLOUD",
                                        isPendingSync = false,
                                        lastSyncedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        OutboxType.TASK_CREATE, OutboxType.TASK_UPDATE -> {
                            val taskMap = mapOf(
                                "id" to entry.entityId,
                                "cabinId" to entry.cabinId,
                                "title" to payload.optString("title"),
                                "description" to payload.optString("description"),
                                "assignee" to payload.optString("assignee"),
                                "status" to payload.optString("status"),
                                "priority" to payload.optString("priority"),
                                "updatedAt" to entry.createdAt
                            )
                            firestore.collection("cabins")
                                .document(entry.cabinId)
                                .collection("tasks")
                                .document(entry.entityId)
                                .set(taskMap)
                                .await()
                        }
                        OutboxType.TASK_DELETE -> {
                            firestore.collection("cabins")
                                .document(entry.cabinId)
                                .collection("tasks")
                                .document(entry.entityId)
                                .delete()
                                .await()
                        }
                    }
                }

                // Mark entry synced and remove from pending outbox
                outboxDao.updateOutboxEntry(entry.copy(status = OutboxStatus.SYNCED))
                outboxDao.deleteOutboxEntry(entry.id)
                successCount++
            } catch (e: Exception) {
                Log.e("OutboxSyncWorker", "Error syncing outbox entry ${entry.id}: ${e.localizedMessage}")
                val newRetry = entry.retryCount + 1
                outboxDao.updateOutboxEntry(
                    entry.copy(
                        status = if (newRetry >= 5) OutboxStatus.FAILED else OutboxStatus.PENDING,
                        retryCount = newRetry,
                        errorMessage = e.localizedMessage
                    )
                )
            }
        }

        return if (successCount > 0 || pendingEntries.all { it.retryCount >= 5 }) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "outbox_firestore_sync_work"

        fun scheduleSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            } catch (e: Exception) {
                Log.w("OutboxSyncWorker", "Could not enqueue WorkManager sync: ${e.localizedMessage}")
            }
        }
    }
}
