package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AudioParticipant
import com.example.data.Cabin
import com.example.data.CabinMessage
import com.example.data.CabinTask
import com.example.data.CabinType
import com.example.data.CabinsRepository
import com.example.data.KeyVaultDetails
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.notifications.FcmTokenManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CabinTabType {
    CHAT,
    TASKS,
    AUDIO_POD,
    SECURITY_VAULT
}

enum class WebRtcConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class AudioPodState(
    val isConnected: Boolean = false,
    val connectionStatus: WebRtcConnectionStatus = WebRtcConnectionStatus.DISCONNECTED,
    val isMuted: Boolean = false,
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val activeSpeakerName: String = "مهندس أحمد",
    val latencyMs: Int = 24,
    val bitrateKbps: Int = 64,
    val iceServerStatus: String = "STUN/TURN Direct P2P Connected",
    val participants: List<AudioParticipant> = listOf(
        AudioParticipant("1", "أنت (المسؤول)", isSpeaking = false, isMuted = false),
        AudioParticipant("2", "مهندس أحمد (Architect)", isSpeaking = true, isMuted = false, pingMs = 18),
        AudioParticipant("3", "مهندسة سارة (Crypto Lead)", isSpeaking = false, isMuted = true, pingMs = 22),
        AudioParticipant("4", "مهندس طارق (DevOps)", isSpeaking = false, isMuted = false, pingMs = 31)
    )
)

data class SyncState(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncedTime: String = "الآن",
    val syncMessage: String = "بياناتك متزامنة مع Cloud Firestore"
)

class CabinsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CabinsRepository

    val allCabins: StateFlow<List<Cabin>>

    private val _selectedCabin = MutableStateFlow<Cabin?>(null)
    val selectedCabin: StateFlow<Cabin?> = _selectedCabin.asStateFlow()

    private val _selectedTab = MutableStateFlow(CabinTabType.CHAT)
    val selectedTab: StateFlow<CabinTabType> = _selectedTab.asStateFlow()

    private val _isDecryptedView = MutableStateFlow(true)
    val isDecryptedView: StateFlow<Boolean> = _isDecryptedView.asStateFlow()

    private val _audioPodState = MutableStateFlow(AudioPodState())
    val audioPodState: StateFlow<AudioPodState> = _audioPodState.asStateFlow()

    private val _keyVault = MutableStateFlow(KeyVaultDetails())
    val keyVault: StateFlow<KeyVaultDetails> = _keyVault.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val fcmToken: StateFlow<String?> = FcmTokenManager.currentToken

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagesForSelectedCabin: StateFlow<List<CabinMessage>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedCabin: StateFlow<List<CabinTask>>

    val allTasksAcrossCabins: StateFlow<List<CabinTask>>

    init {
        FcmTokenManager.init(application)
        val db = AppDatabase.getDatabase(application)
        repository = CabinsRepository(
            cabinDao = db.cabinDao(),
            messageDao = db.messageDao(),
            taskDao = db.taskDao(),
            outboxDao = db.outboxDao(),
            appContext = application
        )

        allCabins = repository.allCabins.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.pendingOutboxCount.collect { count ->
                _syncState.value = _syncState.value.copy(pendingCount = count)
            }
        }

        messagesForSelectedCabin = _selectedCabin.flatMapLatest { cabin ->
            if (cabin != null) repository.getMessagesForCabin(cabin.id) else flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


        tasksForSelectedCabin = _selectedCabin.flatMapLatest { cabin ->
            if (cabin != null) repository.getTasksForCabin(cabin.id) else flowOf(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTasksAcrossCabins = repository.allTasksAcrossCabins.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            allCabins.collect { cabins ->
                if (_selectedCabin.value == null && cabins.isNotEmpty()) {
                    _selectedCabin.value = cabins.firstOrNull { !it.isArchived } ?: cabins.first()
                }
            }
        }
    }

    fun selectCabin(cabin: Cabin) {
        if (_selectedCabin.value?.id != cabin.id) {
            _isDecryptedView.value = false
        }
        _selectedCabin.value = cabin
    }

    fun selectCabinById(cabinId: String, targetTab: CabinTabType? = null) {
        viewModelScope.launch {
            val cabin = allCabins.value.find { it.id == cabinId }
                ?: repository.getCabinById(cabinId)
            if (cabin != null) {
                selectCabin(cabin)
                targetTab?.let { selectTab(it) }
            }
        }
    }

    fun simulatePushMessage(cabinId: String? = null, customText: String? = null) {
        val cabin = if (!cabinId.isNullOrBlank()) {
            allCabins.value.find { it.id == cabinId } ?: _selectedCabin.value
        } else {
            _selectedCabin.value
        } ?: return
        FcmTokenManager.simulateIncomingMessagePush(
            context = getApplication(),
            cabinId = cabin.id,
            cabinName = cabin.name,
            senderName = "مهندس أحمد (FCM Push)",
            messageText = customText ?: "تنبيه دفع مشفر E2EE: تم إرسال تحديث أمني جديد إلى كابينتك."
        )
    }

    fun simulatePushTask(cabinId: String? = null, title: String? = null, assignee: String? = null) {
        val cabin = if (!cabinId.isNullOrBlank()) {
            allCabins.value.find { it.id == cabinId } ?: _selectedCabin.value
        } else {
            _selectedCabin.value
        } ?: return
        FcmTokenManager.simulateIncomingTaskPush(
            context = getApplication(),
            cabinId = cabin.id,
            cabinName = cabin.name,
            taskTitle = title ?: "مراجعة تشفير WebRTC عبر التنبيهات الحية",
            assignee = assignee ?: "أنت (Lead Developer)",
            priority = "عالية"
        )
    }

    fun selectTab(tab: CabinTabType) {
        _selectedTab.value = tab
    }

    fun setDecryptedView(decrypted: Boolean) {
        _isDecryptedView.value = decrypted
    }

    fun toggleCipherView() {
        _isDecryptedView.value = !_isDecryptedView.value
    }

    fun toggleConnectivity() {
        val current = _syncState.value
        val newOnline = !current.isOnline
        _syncState.value = current.copy(isOnline = newOnline)
        if (newOnline && current.pendingCount > 0) {
            triggerManualSync()
        }
    }

    fun triggerManualSync() {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isOnline = true,
                isSyncing = true,
                syncMessage = "جاري رفع التغييرات المحلية إلى Firestore عبر Outbox Worker..."
            )
            com.example.worker.OutboxSyncWorker.scheduleSync(getApplication())
            delay(1600)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncedTime = "منذ لحظات",
                syncMessage = "تمت مزامنة البيانات السحابية بنجاح"
            )
        }
    }

    fun sendMessage(text: String) {
        val currentCabin = _selectedCabin.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(currentCabin.id, text, isMe = true)

            if (!_syncState.value.isOnline) {
                _syncState.value = _syncState.value.copy(
                    pendingCount = _syncState.value.pendingCount + 1
                )
            } else {
                _syncState.value = _syncState.value.copy(
                    lastSyncedTime = "الآن"
                )
            }

            // Simulate incoming peer response & trigger local notification
            delay(2000)
            val peerName = if (currentCabin.type == CabinType.WORK) "مهندس أحمد (Architect)" else "عضو الكبينة المشفرة"
            val replyText = "تم استلام الرسالة المشفرة بنجاح وتوثيق التوقيع الرقمي E2EE."
            val incomingMsg = repository.sendMessage(
                cabinId = currentCabin.id,
                text = replyText,
                senderName = peerName,
                isMe = false
            )

            NotificationHelper.showNewMessageNotification(
                context = getApplication(),
                cabinName = currentCabin.name,
                senderName = incomingMsg.senderName,
                messageText = replyText,
                cabin = currentCabin,
                cabinId = currentCabin.id
            )
        }
    }

    fun toggleReaction(message: CabinMessage, emoji: String) {
        val currentCabin = _selectedCabin.value ?: return
        viewModelScope.launch {
            repository.toggleMessageReaction(
                cabinId = currentCabin.id,
                messageId = message.id,
                emoji = emoji,
                userName = "أنت"
            )
            _syncState.value = _syncState.value.copy(
                lastSyncedTime = "الآن",
                syncMessage = "تم حفظ التفاعل وتوثيقه في السجل المشفر"
            )
        }
    }

    fun shareAttachment(uri: Uri, fallbackIsImage: Boolean) {
        val currentCabin = _selectedCabin.value ?: return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                syncMessage = "جاري تشفير الملف محلياً (AES-256-GCM)..."
            )
            try {
                val msg = repository.sendMediaAttachment(
                    cabinId = currentCabin.id,
                    uri = uri,
                    fallbackIsImage = fallbackIsImage,
                    senderName = "أنت"
                )
                _syncState.value = _syncState.value.copy(
                    lastSyncedTime = "الآن",
                    syncMessage = "تم التشفير المحلي وحفظ الملف في الخزنة. جاري رفع البيانات إلى التخزين السحابي الآمن..."
                )

                // Simulate peer confirmation
                delay(2500)
                val peerName = if (currentCabin.type == CabinType.WORK) "مهندس أحمد (Architect)" else "عضو الكبينة"
                val replyText = "تم استلام المرفق المشفر (${msg.attachmentName}) والتحقق من التوقيع الرقمي بنجاح 🔒."
                val incomingMsg = repository.sendMessage(
                    cabinId = currentCabin.id,
                    text = replyText,
                    senderName = peerName,
                    isMe = false
                )

                NotificationHelper.showNewMessageNotification(
                    context = getApplication(),
                    cabinName = currentCabin.name,
                    senderName = incomingMsg.senderName,
                    messageText = replyText,
                    cabin = currentCabin,
                    cabinId = currentCabin.id
                )
            } catch (e: Exception) {
                _syncState.value = _syncState.value.copy(
                    syncMessage = "خطأ أثناء تشفير المرفق: ${e.localizedMessage}"
                )
            }
        }
    }

    fun decryptAttachmentBitmap(message: CabinMessage): Bitmap? {
        return repository.decryptAttachmentBitmap(message)
    }

    fun createCabin(
        name: String,
        description: String,
        type: CabinType,
        colorHex: String = "",
        iconEmoji: String = "",
        participantTags: String = ""
    ) {
        viewModelScope.launch {
            val newCabin = repository.addCabin(name, description, type, colorHex, iconEmoji, participantTags)
            _selectedCabin.value = newCabin
            if (!_syncState.value.isOnline) {
                _syncState.value = _syncState.value.copy(
                    pendingCount = _syncState.value.pendingCount + 1
                )
            }
        }
    }

    fun joinCabinByQrPayload(
        payload: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var cabinName = "كبينة مشفرة جديدة"
                var cabinDesc = "تم الانضمام المباشر عبر مسح رمز QR"
                var cabinType = CabinType.WORK
                var colorHex = "#2196F3"
                var iconEmoji = "🛡️"

                if (payload.trim().startsWith("{")) {
                    val json = org.json.JSONObject(payload)
                    cabinName = json.optString("name", cabinName)
                    val typeStr = json.optString("cabinType", "WORK")
                    cabinType = try { CabinType.valueOf(typeStr) } catch (e: Exception) { CabinType.WORK }
                    colorHex = json.optString("colorHex", colorHex)
                    iconEmoji = json.optString("iconEmoji", iconEmoji)
                    val inviteCode = json.optString("inviteCode", "")
                    if (inviteCode.isNotBlank()) {
                        cabinDesc = "تم الانضمام عبر رمز الدعوة: $inviteCode"
                    }
                } else if (payload.isNotBlank()) {
                    cabinName = "كبينة ($payload)"
                }

                // Check if cabin already exists in local DB by name
                val existing = allCabins.value.find { it.name.trim().equals(cabinName.trim(), ignoreCase = true) }
                if (existing != null) {
                    _selectedCabin.value = existing
                    onResult(true, "أنت بالفعل عضو في ${existing.name}. تم تنشيط المفاتيح.")
                    return@launch
                }

                val newCabin = repository.addCabin(
                    name = cabinName,
                    description = cabinDesc,
                    type = cabinType,
                    colorHex = colorHex,
                    iconEmoji = iconEmoji
                )

                // Add welcome message
                repository.sendMessage(
                    cabinId = newCabin.id,
                    text = "مرحباً بك! تم الانضمام إلى الكبينة المشفرة بنجاح عبر مسح رمز QR وتوثيق مفتاح التعارف E2EE P2P.",
                    senderName = "النظام العسكري المعزول",
                    isMe = false
                )

                _selectedCabin.value = newCabin
                onResult(true, "تمت إضافة والانضمام إلى ${newCabin.name} بنجاح!")
            } catch (e: Exception) {
                onResult(false, "فشل تحليل رمز QR: ${e.localizedMessage}")
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        assignee: String,
        priority: TaskPriority,
        dueDate: String = "اليوم"
    ) {
        val currentCabin = _selectedCabin.value ?: return
        if (title.isBlank()) return
        val priorityText = when (priority) {
            TaskPriority.HIGH -> "عالية"
            TaskPriority.MEDIUM -> "متوسطة"
            TaskPriority.LOW -> "منخفضة"
        }
        viewModelScope.launch {
            repository.addTask(currentCabin.id, title, description, assignee, priority, dueDate)
            
            // Post an encrypted task announcement in the cabin chat so members see it
            repository.sendMessage(
                cabinId = currentCabin.id,
                text = "تم إنشاء مهمة جديدة داخل الكابينة: $title (المسند إليه: $assignee - الأهمية: $priorityText)",
                senderName = "نظام مهام الكبينة (Room)",
                isMe = false,
                isTaskAttachment = true,
                taskTitle = title
            )

            if (!_syncState.value.isOnline) {
                _syncState.value = _syncState.value.copy(
                    pendingCount = _syncState.value.pendingCount + 1
                )
            }

            // Real-time notification for the assigned task
            NotificationHelper.showTaskNotification(
                context = getApplication(),
                cabinName = currentCabin.name,
                taskTitle = title,
                assignee = assignee,
                priority = priorityText,
                cabin = currentCabin,
                cabinId = currentCabin.id
            )
        }
    }

    fun updateTask(task: CabinTask) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun updateTaskStatus(task: CabinTask, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(task, newStatus)
        }
    }

    fun toggleTaskCompletion(task: CabinTask) {
        val nextStatus = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
        updateTaskStatus(task, nextStatus)
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun toggleAudioPodJoin() {
        val current = _audioPodState.value
        if (current.isConnected || current.connectionStatus == WebRtcConnectionStatus.CONNECTING) {
            _audioPodState.value = current.copy(
                isConnected = false,
                connectionStatus = WebRtcConnectionStatus.DISCONNECTED
            )
        } else {
            viewModelScope.launch {
                _audioPodState.value = current.copy(
                    isConnected = false,
                    connectionStatus = WebRtcConnectionStatus.CONNECTING
                )
                delay(900)
                _audioPodState.value = _audioPodState.value.copy(
                    isConnected = true,
                    connectionStatus = WebRtcConnectionStatus.CONNECTED,
                    latencyMs = 24,
                    bitrateKbps = 64
                )
            }
        }
    }

    fun toggleAudioPodMute() {
        val current = _audioPodState.value
        val newMuted = !current.isMicMuted
        val updatedParticipants = current.participants.map { participant ->
            if (participant.id == "1") participant.copy(isMuted = newMuted) else participant
        }
        _audioPodState.value = current.copy(
            isMuted = newMuted,
            isMicMuted = newMuted,
            participants = updatedParticipants
        )
    }

    fun toggleAudioPodSpeakerMute() {
        val current = _audioPodState.value
        _audioPodState.value = current.copy(
            isSpeakerMuted = !current.isSpeakerMuted
        )
    }

    fun toggleAudioPodConnectionStatus() {
        val current = _audioPodState.value
        val nextStatus = when (current.connectionStatus) {
            WebRtcConnectionStatus.CONNECTED -> WebRtcConnectionStatus.RECONNECTING
            WebRtcConnectionStatus.RECONNECTING -> WebRtcConnectionStatus.CONNECTING
            WebRtcConnectionStatus.CONNECTING -> WebRtcConnectionStatus.DISCONNECTED
            WebRtcConnectionStatus.DISCONNECTED -> WebRtcConnectionStatus.CONNECTED
        }
        val isConn = nextStatus == WebRtcConnectionStatus.CONNECTED || nextStatus == WebRtcConnectionStatus.RECONNECTING
        _audioPodState.value = current.copy(
            connectionStatus = nextStatus,
            isConnected = isConn
        )
    }

    fun rotateE2EEKeys() {
        val current = _keyVault.value
        _keyVault.value = current.copy(
            doubleRatchetIndex = current.doubleRatchetIndex + 1,
            preKeysRemaining = current.preKeysRemaining - 1
        )
    }

    fun archiveCabin(cabin: Cabin) {
        viewModelScope.launch {
            repository.archiveCabin(cabin)
            val activeCabins = allCabins.value.filter { !it.isArchived && it.id != cabin.id }
            if (_selectedCabin.value?.id == cabin.id) {
                _selectedCabin.value = activeCabins.firstOrNull()
            }
        }
    }

    fun unarchiveCabin(cabin: Cabin) {
        viewModelScope.launch {
            repository.unarchiveCabin(cabin)
            _selectedCabin.value = cabin.copy(isArchived = false)
        }
    }

    fun setDndMode(cabin: Cabin, enabled: Boolean, durationHours: Int = 0) {
        viewModelScope.launch {
            repository.setDndMode(cabin, enabled, durationHours)
            val updated = cabin.copy(
                isDndEnabled = enabled,
                dndUntilMillis = if (enabled && durationHours > 0) System.currentTimeMillis() + (durationHours * 3600000L) else 0L
            )
            if (_selectedCabin.value?.id == cabin.id) {
                _selectedCabin.value = updated
            }
        }
    }

    fun updateCabinAppearance(cabin: Cabin, colorHex: String, iconEmoji: String) {
        viewModelScope.launch {
            repository.updateCabinAppearance(cabin, colorHex, iconEmoji)
            val updated = cabin.copy(colorHex = colorHex, iconEmoji = iconEmoji)
            if (_selectedCabin.value?.id == cabin.id) {
                _selectedCabin.value = updated
            }
        }
    }

    fun exportBackup(password: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val encryptedBackup = repository.exportBackup(password)
            onResult(encryptedBackup)
        }
    }

    fun restoreBackup(backupContent: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreBackup(backupContent, password)
            if (result.first) {
                val allList = repository.allCabins.first()
                val current = _selectedCabin.value
                if (current != null) {
                    val updated = allList.find { it.id == current.id }
                    if (updated != null) {
                        _selectedCabin.value = updated
                    } else if (allList.isNotEmpty()) {
                        _selectedCabin.value = allList.first()
                    }
                } else if (allList.isNotEmpty()) {
                    _selectedCabin.value = allList.first()
                }
            }
            onResult(result.first, result.second)
        }
    }
}
