package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.util.EncryptedStorageManager
import com.example.worker.OutboxSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.json.JSONObject
import java.io.File
import java.util.UUID

class CabinsRepository(
    private val cabinDao: CabinDao,
    private val messageDao: MessageDao,
    private val taskDao: TaskDao,
    private val outboxDao: OutboxDao? = null,
    private val appContext: Context? = null
) {
    val allCabins: Flow<List<Cabin>> = cabinDao.getAllCabins()
    val allTasksAcrossCabins: Flow<List<CabinTask>> = taskDao.getAllTasksFlow()
    val pendingOutboxCount: Flow<Int> = outboxDao?.getPendingCountFlow() ?: flowOf(0)

    fun getMessagesForCabin(cabinId: String): Flow<List<CabinMessage>> {
        return messageDao.getMessagesForCabin(cabinId)
    }

    fun getTasksForCabin(cabinId: String): Flow<List<CabinTask>> {
        return taskDao.getTasksForCabin(cabinId)
    }

    suspend fun getCabinById(id: String): Cabin? {
        return cabinDao.getCabinById(id)
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = cabinDao.getAllCabins().first()
        if (existing.isNotEmpty()) return

        val initialCabins = listOf(
            Cabin(
                id = "cabin_work_1",
                name = "كبينة مشروع الذكاء الاصطناعي",
                description = "مساحة خاصة مشفرة لتطوير المعمارية والتصميم الهندسي",
                type = CabinType.WORK,
                memberCount = 8,
                unreadCount = 2,
                securityLevel = "Signal Double Ratchet E2EE",
                isAudioPodActive = true,
                activePodMembers = 3,
                participantTags = "@أحمد, @سارة, @طارق, #هندسة, #ذكاء_اصطناعي"
            ),
            Cabin(
                id = "cabin_family_1",
                name = "كبينة العائلة الخاصة",
                description = "مساحة التواصل العائلية المشفرة لمشاركة الأنشطة واللحظات",
                type = CabinType.FAMILY,
                memberCount = 5,
                unreadCount = 0,
                securityLevel = "AES-256-GCM Hardware Sealed",
                isAudioPodActive = false,
                activePodMembers = 0,
                participantTags = "@نورة, @خالد, @أم_أحمد, #عائلة, #رحلات"
            ),
            Cabin(
                id = "cabin_secret_1",
                name = "كبينة الخزنة السرية (Zero Cloud)",
                description = "تواصل محلي بحت بدون أي حفظ على السحابة مع التدمير الذاتي للرسائل",
                type = CabinType.SECRET_VAULT,
                memberCount = 2,
                unreadCount = 1,
                securityLevel = "Ephemeral E2EE + Local Keystore",
                isAudioPodActive = false,
                activePodMembers = 0,
                participantTags = "@أنت, #خزنة_صفرية, #تشفير_محلي"
            ),
            Cabin(
                id = "cabin_public_1",
                name = "كبينة هندسة البرمجيات العامة",
                description = "نقاشات مفتوحة حول Flutter و Jetpack Compose والمستجدات التقنية",
                type = CabinType.PUBLIC,
                memberCount = 42,
                unreadCount = 0,
                securityLevel = "Standard E2EE Group Protocol",
                isAudioPodActive = true,
                activePodMembers = 7,
                participantTags = "@المجتمع, @مطور_عام, #flutter, #compose"
            )
        )
        cabinDao.insertCabins(initialCabins)

        // Seed messages for Work Cabin
        val workMessages = listOf(
            CabinMessage(
                id = "msg_1",
                cabinId = "cabin_work_1",
                senderName = "مهندس أحمد (Architect)",
                isMe = false,
                text = "مرحباً بالجميع! تم اعتماد هيكل Clean Architecture ونظام التشفير الشامل E2EE للكبائن.",
                cipherText = "A8f9x2/Q1zLLm903+vXq9a8B841zKls19xZZ90A812K=",
                signature = "SIG_ED25519_8F2A10B39C",
                ratchetIndex = 140,
                nonceIv = "IV_0x89C13A7F",
                authTag = "TAG_GCM_0x3F8A",
                timestamp = "10:15 AM",
                reactionsJson = CabinMessage.formatReactionsJson(
                    mapOf(
                        "👍" to listOf("أنت", "مهندسة سارة (Crypto Lead)"),
                        "🔥" to listOf("مهندس طارق (DevOps)")
                    )
                )
            ),
            CabinMessage(
                id = "msg_2",
                cabinId = "cabin_work_1",
                senderName = "أنت",
                isMe = true,
                text = "ممتاز جداً! سنبدأ ببرمجة خدمة WebRTC Encrypted Audio Pods ونظام إدارة المهام المستقل لكل كابينة.",
                cipherText = "K90xzzL/31aM+0021Lxz49a0a19022xZ091A82==",
                signature = "SIG_ED25519_9921BC018A",
                ratchetIndex = 141,
                nonceIv = "IV_0x44A10B99",
                authTag = "TAG_GCM_0x7721",
                timestamp = "10:18 AM",
                reactionsJson = CabinMessage.formatReactionsJson(
                    mapOf(
                        "❤️" to listOf("مهندس أحمد (Architect)")
                    )
                )
            ),
            CabinMessage(
                id = "msg_3",
                cabinId = "cabin_work_1",
                senderName = "مهندسة سارة (Crypto Lead)",
                isMe = false,
                text = "تم التحقق من تطابق الرقم الأمني (Safety Number) لمفاتيح Signal Protocol بنجاح 🔒.",
                cipherText = "91aXXzz91023aLs/302910aZ88912x90A11B2==",
                signature = "SIG_ED25519_771A02C911",
                ratchetIndex = 142,
                nonceIv = "IV_0x11B82A44",
                authTag = "TAG_GCM_0x8832",
                timestamp = "10:22 AM"
            ),
            CabinMessage(
                id = "msg_4",
                cabinId = "cabin_work_1",
                senderName = "نظام الكابينة",
                isMe = false,
                text = "تم إنشاء مهمة جديدة: إعداد اختبارات التشفير الشامل",
                cipherText = "Encrypted Task Payload [0x3912A88]",
                signature = "SIG_ED25519_SYSTEM_01",
                ratchetIndex = 143,
                nonceIv = "IV_0x00A192B2",
                authTag = "TAG_GCM_0x9911",
                timestamp = "10:25 AM",
                isTaskAttachment = true,
                taskTitle = "إعداد اختبارات التشفير الشامل"
            ),
            CabinMessage(
                id = "msg_5",
                cabinId = "cabin_work_1",
                senderName = "مهندس طارق (DevOps)",
                isMe = false,
                text = "مقطع صوتي مشفر: ملخص جلسة المراجعة المعمارية",
                cipherText = "AUDIO_STREAM_AES256_GCM_PAYLOAD_E2EE_0x99A",
                signature = "SIG_ED25519_3310AC881A",
                ratchetIndex = 144,
                nonceIv = "IV_0x8812A99B",
                authTag = "TAG_GCM_0x1102",
                timestamp = "10:30 AM",
                isAudioClip = true,
                audioDuration = "0:45"
            ),
            CabinMessage(
                id = "msg_doc_1",
                cabinId = "cabin_work_1",
                senderName = "مهندسة سارة (Crypto Lead)",
                isMe = false,
                text = "مستند مشفر: مواصفات تشفير AES-256 وقواعد عزل التخزين المحلي",
                cipherText = "DOC_CIPHER_AES256_GCM_SHA256_0x44B19022A_SEALED",
                signature = "SIG_ED25519_5521AC911B",
                ratchetIndex = 145,
                nonceIv = "IV_0x8F023AB1",
                authTag = "TAG_GCM_0x33A1",
                timestamp = "10:35 AM",
                isAttachment = true,
                attachmentId = "att_seed_doc_1",
                attachmentType = "DOCUMENT",
                attachmentName = "E2EE_Security_Architecture_v2.pdf",
                attachmentMimeType = "application/pdf",
                attachmentSizeFormatted = "418 KB",
                attachmentSizeBytes = 428032L,
                encryptedLocalPath = "/data/user/0/com.example/files/cabin_vault/cabin_work_1/attachments/att_seed_doc_1.enc",
                encryptionIvHex = "8F023AB1908234CD8811AABB",
                sha256DigestHex = "3A88F01B92C4819D20C3848B560193BB474130F4019A284E910283B7458190AB",
                uploadStatus = "SYNCED_CLOUD",
                cloudStorageRef = "gs://private-cabins.appspot.com/cabins/cabin_work_1/attachments/att_seed_doc_1.enc",
                reactionsJson = CabinMessage.formatReactionsJson(
                    mapOf("🔒" to listOf("أنت", "مهندس أحمد (Architect)"))
                )
            ),
            CabinMessage(
                id = "msg_img_1",
                cabinId = "cabin_work_1",
                senderName = "أنت",
                isMe = true,
                text = "صورة مشفرة: مخطط طوبولوجيا شبكة الكبائن وخوادم المفاتيح",
                cipherText = "IMG_CIPHER_AES256_GCM_SHA256_0x992B1044A_SEALED",
                signature = "SIG_ED25519_99A021CC77",
                ratchetIndex = 146,
                nonceIv = "IV_0x77A199BB",
                authTag = "TAG_GCM_0x99A0",
                timestamp = "10:40 AM",
                isAttachment = true,
                attachmentId = "att_seed_img_1",
                attachmentType = "IMAGE",
                attachmentName = "Cabin_Topology_Schematic.png",
                attachmentMimeType = "image/png",
                attachmentSizeFormatted = "1.2 MB",
                attachmentSizeBytes = 1258291L,
                encryptedLocalPath = "/data/user/0/com.example/files/cabin_vault/cabin_work_1/attachments/att_seed_img_1.enc",
                encryptionIvHex = "77A199BB442008E199220044",
                sha256DigestHex = "7B44A80291FC22D8819AB0041289CF0088192A472910BB429001BA889100FA12",
                uploadStatus = "SYNCED_CLOUD",
                cloudStorageRef = "gs://private-cabins.appspot.com/cabins/cabin_work_1/attachments/att_seed_img_1.enc",
                reactionsJson = CabinMessage.formatReactionsJson(
                    mapOf("🚀" to listOf("مهندس طارق (DevOps)"))
                )
            )
        )
        messageDao.insertMessages(workMessages)

        // Seed Tasks for Work Cabin
        val workTasks = listOf(
            CabinTask(
                id = "task_1",
                cabinId = "cabin_work_1",
                title = "تطبيق بروتوكول Signal ومزدوج التسنن (Double Ratchet)",
                description = "إنشاء وحدة التشفير وتخزين المفاتيح بأمان في Android Keystore",
                assignee = "مهندسة سارة",
                dueDate = "غداً - 05:00 PM",
                priority = TaskPriority.HIGH,
                status = TaskStatus.IN_PROGRESS
            ),
            CabinTask(
                id = "task_2",
                cabinId = "cabin_work_1",
                title = "إعداد غرف WebRTC Audio Pods المشفرة",
                description = "ربط شبكة Peer-to-Mesh مع التشفير المباشر ومنع الحفظ على الخادم",
                assignee = "أنت",
                dueDate = "خلال يومين",
                priority = TaskPriority.HIGH,
                status = TaskStatus.TODO
            ),
            CabinTask(
                id = "task_3",
                cabinId = "cabin_work_1",
                title = "تصميم القائمة الجانبية للتنقل السريع بين الكبائن",
                description = "استخدام Material 3 Drawer مع شارات التنبيه ومؤشرات البث الصوتي",
                assignee = "فريق UI/UX",
                dueDate = "اليوم",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.DONE
            )
        )
        for (t in workTasks) {
            taskDao.insertTask(t)
        }

        // Seed messages & tasks for Family Cabin
        val familyMessages = listOf(
            CabinMessage(
                id = "msg_fam_1",
                cabinId = "cabin_family_1",
                senderName = "الأم",
                isMe = false,
                text = "لا تنسوا موعد اجتماع العائلة الأسبوعي في الغرفة الصوتية المشفرة الليلة ❤️",
                cipherText = "Fam_Cipher_902319022X==_AES_GCM",
                signature = "SIG_ED25519_FAM_019A",
                ratchetIndex = 88,
                nonceIv = "IV_0x7721A001",
                authTag = "TAG_GCM_0x5532",
                timestamp = "09:00 AM",
                reactionsJson = CabinMessage.formatReactionsJson(
                    mapOf(
                        "❤️" to listOf("أنت", "الأب")
                    )
                )
            )
        )
        messageDao.insertMessages(familyMessages)

        val familyTasks = listOf(
            CabinTask(
                id = "task_fam_1",
                cabinId = "cabin_family_1",
                title = "التخطيط لرحلة نهاية الأسبوع",
                description = "تحديد الأنشطة وتوزيع المهام العائلية",
                assignee = "الجميع",
                dueDate = "الجمعة القادم",
                priority = TaskPriority.LOW,
                status = TaskStatus.TODO
            )
        )
        for (t in familyTasks) {
            taskDao.insertTask(t)
        }
    }

    suspend fun addCabin(
        name: String,
        description: String,
        type: CabinType,
        colorHex: String = "",
        iconEmoji: String = "",
        participantTags: String = ""
    ): Cabin {
        val newCabin = Cabin(
            id = "cabin_" + UUID.randomUUID().toString().take(8),
            name = name,
            description = description,
            type = type,
            memberCount = 1,
            unreadCount = 0,
            colorHex = colorHex,
            iconEmoji = iconEmoji,
            participantTags = participantTags,
            securityLevel = when (type) {
                CabinType.SECRET_VAULT -> "Ephemeral Zero-Trace E2EE"
                CabinType.WORK -> "Signal Double Ratchet Enterprise"
                CabinType.FAMILY -> "AES-256 Family Vault"
                CabinType.PUBLIC -> "Public Group E2EE"
            }
        )
        cabinDao.insertCabin(newCabin)
        return newCabin
    }

    suspend fun sendMessage(
        cabinId: String,
        text: String,
        senderName: String = "أنت",
        isMe: Boolean = true,
        isTaskAttachment: Boolean = false,
        taskTitle: String? = null
    ): CabinMessage {
        val hexHash = text.hashCode().toString(16).uppercase()
        val ratchetIdx = (145..200).random()
        val newMsg = CabinMessage(
            id = "msg_" + System.currentTimeMillis(),
            cabinId = cabinId,
            senderName = senderName,
            isMe = isMe,
            text = text,
            cipherText = "E2EE_AES256_" + hexHash + "_ENC_PAYLOAD==",
            signature = "SIG_ED25519_" + hexHash.take(8),
            ratchetIndex = ratchetIdx,
            nonceIv = "IV_0x" + UUID.randomUUID().toString().take(8).uppercase(),
            authTag = "TAG_GCM_0x" + hexHash.take(4),
            isSignatureVerified = true,
            timestamp = "الآن",
            isPendingSync = true,
            isTaskAttachment = isTaskAttachment,
            taskTitle = taskTitle
        )
        messageDao.insertMessage(newMsg)

        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.MESSAGE,
                entityId = newMsg.id,
                cabinId = cabinId,
                payloadJson = JSONObject().apply {
                    put("text", text)
                    put("senderName", senderName)
                    put("cipherText", newMsg.cipherText)
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }

        return newMsg
    }

    suspend fun toggleMessageReaction(
        cabinId: String,
        messageId: String,
        emoji: String,
        userName: String = "أنت"
    ): CabinMessage? {
        val message = messageDao.getMessageById(messageId) ?: return null
        val currentMap = message.getReactionsMap().toMutableMap()
        val currentUsers = (currentMap[emoji] ?: emptyList()).toMutableList()

        if (currentUsers.contains(userName)) {
            currentUsers.remove(userName)
        } else {
            currentUsers.add(userName)
        }

        if (currentUsers.isEmpty()) {
            currentMap.remove(emoji)
        } else {
            currentMap[emoji] = currentUsers
        }

        val updatedReactionsJson = CabinMessage.formatReactionsJson(currentMap)
        val updatedMessage = message.copy(
            reactionsJson = updatedReactionsJson,
            isPendingSync = true
        )
        messageDao.updateMessage(updatedMessage)

        // Queue in Outbox for Firestore sync
        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.MESSAGE_REACTION,
                entityId = messageId,
                cabinId = cabinId,
                payloadJson = JSONObject().apply {
                    put("messageId", messageId)
                    put("emoji", emoji)
                    put("userName", userName)
                    put("reactions", JSONObject(updatedReactionsJson))
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }

        // Attempt direct Firestore update if online
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val firestoreReactions = mutableMapOf<String, Any>()
            currentMap.forEach { (k, v) -> firestoreReactions[k] = v }
            firestore.collection("cabins")
                .document(cabinId)
                .collection("messages")
                .document(messageId)
                .set(
                    mapOf(
                        "reactions" to firestoreReactions,
                        "lastReactionAt" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        } catch (e: Exception) {
            // Ignored; Outbox handles background sync
        }

        return updatedMessage
    }

    suspend fun sendMediaAttachment(
        cabinId: String,
        uri: Uri,
        fallbackIsImage: Boolean = false,
        senderName: String = "أنت"
    ): CabinMessage {
        val context = appContext ?: throw IllegalStateException("appContext is required to process attachments")
        val result = EncryptedStorageManager.encryptFromUri(context, cabinId, uri, fallbackIsImage)

        val msgId = "msg_att_" + System.currentTimeMillis()
        val typeStr = if (result.isImage) "IMAGE" else "DOCUMENT"
        val displayText = if (result.isImage) "صورة مشفرة: ${result.fileName}" else "مستند مشفر: ${result.fileName}"

        val newMsg = CabinMessage(
            id = msgId,
            cabinId = cabinId,
            senderName = senderName,
            isMe = true,
            text = displayText,
            cipherText = "CIPHER_AES256_GCM_" + result.sha256Hex.take(16).uppercase() + "==",
            signature = "SIG_ED25519_" + result.sha256Hex.take(8).uppercase(),
            ratchetIndex = (150..220).random(),
            nonceIv = "IV_0x" + result.ivHex.take(8),
            authTag = "TAG_GCM_0x" + result.sha256Hex.take(4),
            isSignatureVerified = true,
            timestamp = "الآن",
            isPendingSync = true,
            isAttachment = true,
            attachmentId = result.attachmentId,
            attachmentType = typeStr,
            attachmentName = result.fileName,
            attachmentMimeType = result.mimeType,
            attachmentSizeFormatted = result.formattedSize,
            attachmentSizeBytes = result.fileSizeBytes,
            encryptedLocalPath = result.encryptedFilePath,
            encryptionIvHex = result.ivHex,
            sha256DigestHex = result.sha256Hex,
            uploadStatus = "ENCRYPTED_LOCAL",
            cloudStorageRef = result.cloudStorageRef
        )
        messageDao.insertMessage(newMsg)

        // Queue in Outbox for Cloud Synchronization
        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.ATTACHMENT,
                entityId = newMsg.id,
                cabinId = cabinId,
                payloadJson = JSONObject().apply {
                    put("messageId", newMsg.id)
                    put("attachmentId", result.attachmentId)
                    put("attachmentName", result.fileName)
                    put("attachmentType", typeStr)
                    put("mimeType", result.mimeType)
                    put("sizeBytes", result.fileSizeBytes)
                    put("sha256Digest", result.sha256Hex)
                    put("cloudStorageRef", result.cloudStorageRef)
                    put("encryptionIv", result.ivHex)
                }.toString()
            )
        )
        OutboxSyncWorker.scheduleSync(context)

        return newMsg
    }

    fun decryptAttachmentBytes(message: CabinMessage): ByteArray? {
        val path = message.encryptedLocalPath ?: return null
        val ivHex = message.encryptionIvHex ?: return null
        val file = File(path)
        return EncryptedStorageManager.decryptAttachment(file, ivHex, message.cabinId)
    }

    fun decryptAttachmentBitmap(message: CabinMessage): Bitmap? {
        val path = message.encryptedLocalPath ?: return null
        val ivHex = message.encryptionIvHex ?: return null
        val file = File(path)
        return EncryptedStorageManager.decryptToBitmap(file, ivHex, message.cabinId)
    }

    suspend fun addTask(
        cabinId: String,
        title: String,
        description: String,
        assignee: String,
        priority: TaskPriority,
        dueDate: String = "اليوم"
    ): CabinTask {
        val newTask = CabinTask(
            id = "task_" + UUID.randomUUID().toString().take(8),
            cabinId = cabinId,
            title = title,
            description = description,
            assignee = assignee,
            dueDate = dueDate.ifBlank { "اليوم" },
            priority = priority,
            status = TaskStatus.TODO,
            isPendingSync = true
        )
        taskDao.insertTask(newTask)

        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.TASK_CREATE,
                entityId = newTask.id,
                cabinId = cabinId,
                payloadJson = JSONObject().apply {
                    put("title", title)
                    put("description", description)
                    put("assignee", assignee)
                    put("dueDate", newTask.dueDate)
                    put("priority", priority.name)
                    put("status", newTask.status.name)
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }
        return newTask
    }

    suspend fun updateTask(task: CabinTask) {
        val updated = task.copy(isPendingSync = true)
        taskDao.updateTask(updated)

        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.TASK_UPDATE,
                entityId = task.id,
                cabinId = task.cabinId,
                payloadJson = JSONObject().apply {
                    put("title", task.title)
                    put("description", task.description)
                    put("assignee", task.assignee)
                    put("dueDate", task.dueDate)
                    put("priority", task.priority.name)
                    put("status", task.status.name)
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }
    }

    suspend fun updateTaskStatus(task: CabinTask, newStatus: TaskStatus) {
        val updated = task.copy(status = newStatus, isPendingSync = true)
        taskDao.updateTask(updated)

        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.TASK_UPDATE,
                entityId = task.id,
                cabinId = task.cabinId,
                payloadJson = JSONObject().apply {
                    put("title", task.title)
                    put("description", task.description)
                    put("assignee", task.assignee)
                    put("dueDate", task.dueDate)
                    put("priority", task.priority.name)
                    put("status", newStatus.name)
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)

        outboxDao?.insertOutboxEntry(
            OutboxEntry(
                entityType = OutboxType.TASK_DELETE,
                entityId = taskId,
                cabinId = "",
                payloadJson = JSONObject().apply {
                    put("taskId", taskId)
                }.toString()
            )
        )
        appContext?.let { OutboxSyncWorker.scheduleSync(it) }
    }


    suspend fun archiveCabin(cabin: Cabin) {
        cabinDao.updateCabin(cabin.copy(isArchived = true))
    }

    suspend fun unarchiveCabin(cabin: Cabin) {
        cabinDao.updateCabin(cabin.copy(isArchived = false))
    }

    suspend fun setDndMode(cabin: Cabin, enabled: Boolean, durationHours: Int = 0) {
        val until = if (enabled && durationHours > 0) {
            System.currentTimeMillis() + (durationHours * 3600000L)
        } else 0L

        cabinDao.updateCabin(
            cabin.copy(
                isDndEnabled = enabled,
                dndUntilMillis = until
            )
        )
    }

    suspend fun updateCabinAppearance(cabin: Cabin, colorHex: String, iconEmoji: String) {
        cabinDao.updateCabin(
            cabin.copy(
                colorHex = colorHex,
                iconEmoji = iconEmoji
            )
        )
    }

    suspend fun exportBackup(password: String): String {
        val cabins = cabinDao.getAllCabinsList()
        val messages = messageDao.getAllMessagesList()
        val tasks = taskDao.getAllTasksList()

        val cabinsJsonArray = org.json.JSONArray()
        cabins.forEach { cabin ->
            val obj = org.json.JSONObject().apply {
                put("id", cabin.id)
                put("name", cabin.name)
                put("description", cabin.description)
                put("type", cabin.type.name)
                put("memberCount", cabin.memberCount)
                put("unreadCount", cabin.unreadCount)
                put("securityLevel", cabin.securityLevel)
                put("isAudioPodActive", cabin.isAudioPodActive)
                put("activePodMembers", cabin.activePodMembers)
                put("isArchived", cabin.isArchived)
                put("isDndEnabled", cabin.isDndEnabled)
                put("dndUntilMillis", cabin.dndUntilMillis)
                put("colorHex", cabin.colorHex)
                put("iconEmoji", cabin.iconEmoji)
                put("participantTags", cabin.participantTags)
                put("createdAt", cabin.createdAt)
            }
            cabinsJsonArray.put(obj)
        }

        val messagesJsonArray = org.json.JSONArray()
        messages.forEach { msg ->
            val obj = org.json.JSONObject().apply {
                put("id", msg.id)
                put("cabinId", msg.cabinId)
                put("senderName", msg.senderName)
                put("isMe", msg.isMe)
                put("text", msg.text)
                put("cipherText", msg.cipherText)
                put("timestamp", msg.timestamp)
                put("signature", msg.signature)
                put("ratchetIndex", msg.ratchetIndex)
                put("nonceIv", msg.nonceIv)
                put("authTag", msg.authTag)
                put("isSignatureVerified", msg.isSignatureVerified)
                put("isEncrypted", msg.isEncrypted)
                put("isAudioClip", msg.isAudioClip)
                put("audioDuration", msg.audioDuration ?: "")
                put("isTaskAttachment", msg.isTaskAttachment)
                put("taskTitle", msg.taskTitle ?: "")
                put("reactionsJson", msg.reactionsJson)
            }
            messagesJsonArray.put(obj)
        }

        val tasksJsonArray = org.json.JSONArray()
        tasks.forEach { task ->
            val obj = org.json.JSONObject().apply {
                put("id", task.id)
                put("cabinId", task.cabinId)
                put("title", task.title)
                put("description", task.description)
                put("assignee", task.assignee)
                put("dueDate", task.dueDate)
                put("priority", task.priority.name)
                put("status", task.status.name)
                put("createdAt", task.createdAt)
            }
            tasksJsonArray.put(obj)
        }

        val dbDumpJson = org.json.JSONObject().apply {
            put("cabins", cabinsJsonArray)
            put("messages", messagesJsonArray)
            put("tasks", tasksJsonArray)
        }

        return com.example.util.BackupCryptoManager.encryptBackupPayload(dbDumpJson.toString(), password)
    }

    suspend fun restoreBackup(encryptedBackupContainer: String, password: String): Pair<Boolean, String> {
        return try {
            val decryptedJson = com.example.util.BackupCryptoManager.decryptBackupPayload(encryptedBackupContainer, password)
            val dbDump = org.json.JSONObject(decryptedJson)

            val cabinsArray = dbDump.optJSONArray("cabins") ?: org.json.JSONArray()
            val restoredCabins = mutableListOf<Cabin>()
            for (i in 0 until cabinsArray.length()) {
                val obj = cabinsArray.getJSONObject(i)
                restoredCabins.add(
                    Cabin(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        type = try { CabinType.valueOf(obj.getString("type")) } catch (e: Exception) { CabinType.WORK },
                        memberCount = obj.optInt("memberCount", 1),
                        unreadCount = obj.optInt("unreadCount", 0),
                        securityLevel = obj.optString("securityLevel", "Signal Double Ratchet E2EE"),
                        isAudioPodActive = obj.optBoolean("isAudioPodActive", false),
                        activePodMembers = obj.optInt("activePodMembers", 0),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDndEnabled = obj.optBoolean("isDndEnabled", false),
                        dndUntilMillis = obj.optLong("dndUntilMillis", 0L),
                        colorHex = obj.optString("colorHex", ""),
                        iconEmoji = obj.optString("iconEmoji", ""),
                        participantTags = obj.optString("participantTags", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val messagesArray = dbDump.optJSONArray("messages") ?: org.json.JSONArray()
            val restoredMessages = mutableListOf<CabinMessage>()
            for (i in 0 until messagesArray.length()) {
                val obj = messagesArray.getJSONObject(i)
                restoredMessages.add(
                    CabinMessage(
                        id = obj.getString("id"),
                        cabinId = obj.getString("cabinId"),
                        senderName = obj.getString("senderName"),
                        isMe = obj.optBoolean("isMe", false),
                        text = obj.getString("text"),
                        cipherText = obj.optString("cipherText", ""),
                        timestamp = obj.optString("timestamp", ""),
                        signature = obj.optString("signature", "SIG_ED25519_RESTORED"),
                        ratchetIndex = obj.optInt("ratchetIndex", 1),
                        nonceIv = obj.optString("nonceIv", "IV_RESTORED"),
                        authTag = obj.optString("authTag", "TAG_RESTORED"),
                        isSignatureVerified = obj.optBoolean("isSignatureVerified", true),
                        isEncrypted = obj.optBoolean("isEncrypted", true),
                        isAudioClip = obj.optBoolean("isAudioClip", false),
                        audioDuration = if (obj.has("audioDuration") && obj.getString("audioDuration").isNotBlank()) obj.getString("audioDuration") else null,
                        isTaskAttachment = obj.optBoolean("isTaskAttachment", false),
                        taskTitle = if (obj.has("taskTitle") && obj.getString("taskTitle").isNotBlank()) obj.getString("taskTitle") else null,
                        reactionsJson = obj.optString("reactionsJson", "{}")
                    )
                )
            }

            val tasksArray = dbDump.optJSONArray("tasks") ?: org.json.JSONArray()
            val restoredTasks = mutableListOf<CabinTask>()
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                restoredTasks.add(
                    CabinTask(
                        id = obj.getString("id"),
                        cabinId = obj.getString("cabinId"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        assignee = obj.optString("assignee", ""),
                        dueDate = obj.optString("dueDate", ""),
                        priority = try { TaskPriority.valueOf(obj.getString("priority")) } catch (e: Exception) { TaskPriority.MEDIUM },
                        status = try { TaskStatus.valueOf(obj.getString("status")) } catch (e: Exception) { TaskStatus.TODO },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            if (restoredCabins.isNotEmpty()) cabinDao.insertCabins(restoredCabins)
            if (restoredMessages.isNotEmpty()) messageDao.insertMessages(restoredMessages)
            if (restoredTasks.isNotEmpty()) taskDao.insertTasks(restoredTasks)

            Pair(true, "تمت الاستعادة بنجاح! تم استرجاع ${restoredCabins.size} كبينات و ${restoredMessages.size} رسائل و ${restoredTasks.size} مهام.")
        } catch (e: Exception) {
            Pair(false, "فشل فك التشفير أو الاستعادة: كلمة المرور غير صحيحة أو البيانات تالفة. (${e.localizedMessage})")
        }
    }
}
