package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CabinType {
    WORK,
    FAMILY,
    PUBLIC,
    SECRET_VAULT
}

enum class TaskPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

@Entity(tableName = "cabins")
data class Cabin(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val type: CabinType,
    val memberCount: Int,
    val unreadCount: Int = 0,
    val securityLevel: String = "256-bit AES-GCM + Signal Protocol",
    val isAudioPodActive: Boolean = false,
    val activePodMembers: Int = 0,
    val isArchived: Boolean = false,
    val isDndEnabled: Boolean = false,
    val dndUntilMillis: Long = 0L,
    val colorHex: String = "",
    val iconEmoji: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val participantTags: String = ""
) {
    fun isDndActive(): Boolean {
        if (!isDndEnabled) return false
        return dndUntilMillis == 0L || System.currentTimeMillis() < dndUntilMillis
    }

    fun getParticipantTagsList(): List<String> {
        if (participantTags.isBlank()) return emptyList()
        return participantTags.split(",", " ").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun matchesSearch(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        if (name.lowercase().contains(q)) return true
        if (description.lowercase().contains(q)) return true
        if (participantTags.lowercase().contains(q)) return true
        val tags = getParticipantTagsList()
        return tags.any { tag ->
            tag.lowercase().contains(q) ||
            tag.removePrefix("@").removePrefix("#").lowercase().contains(q.removePrefix("@").removePrefix("#"))
        }
    }
}

@Entity(tableName = "cabin_messages")
data class CabinMessage(
    @PrimaryKey val id: String,
    val cabinId: String,
    val senderName: String,
    val isMe: Boolean,
    val text: String,
    val cipherText: String,
    val timestamp: String,
    val signature: String = "SIG_ED25519_8F2A10B39C",
    val ratchetIndex: Int = 143,
    val nonceIv: String = "IV_0x89C13A7F",
    val authTag: String = "TAG_GCM_0x3F8A",
    val isSignatureVerified: Boolean = true,
    val isEncrypted: Boolean = true,
    val isAudioClip: Boolean = false,
    val audioDuration: String? = null,
    val isTaskAttachment: Boolean = false,
    val taskTitle: String? = null,
    val isAttachment: Boolean = false,
    val attachmentId: String? = null,
    val attachmentType: String? = null, // "IMAGE", "DOCUMENT"
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeFormatted: String? = null,
    val attachmentSizeBytes: Long = 0L,
    val encryptedLocalPath: String? = null,
    val encryptionIvHex: String? = null,
    val sha256DigestHex: String? = null,
    val uploadStatus: String = "SYNCED_CLOUD", // "ENCRYPTED_LOCAL", "UPLOADING", "SYNCED_CLOUD", "FAILED"
    val cloudStorageRef: String? = null,
    val reactionsJson: String = "{}",
    val isPendingSync: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun getReactionsMap(): Map<String, List<String>> {
        if (reactionsJson.isBlank() || reactionsJson == "{}") return emptyMap()
        return try {
            val json = org.json.JSONObject(reactionsJson)
            val map = mutableMapOf<String, List<String>>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = json.optJSONArray(key) ?: org.json.JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val user = array.optString(i)
                    if (user.isNotBlank()) list.add(user)
                }
                if (list.isNotEmpty()) {
                    map[key] = list
                }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun formatReactionsJson(map: Map<String, List<String>>): String {
            val json = org.json.JSONObject()
            map.forEach { (emoji, users) ->
                val activeUsers = users.filter { it.isNotBlank() }.distinct()
                if (activeUsers.isNotEmpty()) {
                    val array = org.json.JSONArray()
                    activeUsers.forEach { array.put(it) }
                    json.put(emoji, array)
                }
            }
            return json.toString()
        }
    }
}

typealias Message = CabinMessage

@Entity(tableName = "cabin_tasks")
data class CabinTask(
    @PrimaryKey val id: String,
    val cabinId: String,
    val title: String,
    val description: String,
    val assignee: String,
    val dueDate: String,
    val priority: TaskPriority,
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

typealias Task = CabinTask

@Entity(tableName = "audio_participants")
data class AudioParticipant(
    @PrimaryKey val id: String,
    val name: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val pingMs: Int = 24,
    val isHandshakeVerified: Boolean = true
)

data class KeyVaultDetails(
    val identityKeyFingerprint: String = "4A8B-9E3F-7C21-1D0A-88EF-09B2-31C4",
    val registrationId: Int = 89432,
    val doubleRatchetIndex: Int = 142,
    val preKeysRemaining: Int = 94,
    val keystoreBacked: Boolean = true,
    val safetyNumber: String = "89201 44190 23109 88120 77103 49012"
)

enum class OutboxType {
    MESSAGE,
    MESSAGE_REACTION,
    ATTACHMENT,
    TASK_CREATE,
    TASK_UPDATE,
    TASK_DELETE
}

enum class OutboxStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

@Entity(tableName = "outbox_entries")
data class OutboxEntry(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val entityType: OutboxType,
    val entityId: String,
    val cabinId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: OutboxStatus = OutboxStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

