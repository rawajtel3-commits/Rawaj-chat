package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.ui.components.AppLockScreen
import com.example.ui.components.AudioPodTab
import com.example.ui.components.CabinDndDialog
import com.example.ui.components.CabinDrawerContent
import com.example.ui.components.CabinHeader
import com.example.ui.components.CabinQrScannerDialog
import com.example.ui.components.CabinQrShareDialog
import com.example.ui.components.ChatTab
import com.example.ui.components.CreateCabinDialog
import com.example.ui.components.CreateTaskDialog
import com.example.ui.components.CustomizeCabinAppearanceDialog
import com.example.ui.components.ExportBackupDialog
import com.example.ui.components.RestoreBackupDialog
import com.example.ui.components.SecurityVaultTab
import com.example.ui.components.SyncStatusBanner
import com.example.ui.components.TasksTab
import com.example.ui.theme.PrivateCabinsTheme
import com.example.ui.viewmodel.CabinTabType
import com.example.ui.viewmodel.CabinsViewModel
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

import com.example.data.AppDatabase
import com.example.data.auth.AuthRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: CabinsViewModel by viewModels()

    private val authRepository by lazy {
        val db = AppDatabase.getDatabase(this)
        AuthRepository(db.userDao(), db.authAuditLogDao())
    }

    private val authViewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(authRepository) as T
            }
        }
    }

    private var isAppUnlockedState = mutableStateOf(false)
    private var biometricErrorMessageState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Trigger biometric authentication on app launch
        showBiometricPrompt()
        handleNotificationIntent(intent)

        setContent {
            PrivateCabinsTheme {
                val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                val isUnlocked by isAppUnlockedState
                val errorMsg by biometricErrorMessageState

                if (currentUser == null) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthenticationSuccess = {
                            isAppUnlockedState.value = true
                        }
                    )
                } else if (!isUnlocked) {
                    AppLockScreen(
                        onTriggerBiometric = { showBiometricPrompt() },
                        onUnlocked = {
                            isAppUnlockedState.value = true
                            biometricErrorMessageState.value = null
                        },
                        errorMessage = errorMsg
                    )
                } else {
                    PrivateCabinsApp(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        onLogout = {
                            authViewModel.logout()
                            isAppUnlockedState.value = false
                        },
                        onLockApp = {
                            isAppUnlockedState.value = false
                        },
                        onRequestBiometricDecrypt = { onSuccess ->
                            showBiometricPromptForDecryption(onSuccess)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val cabinId = intent.getStringExtra(NotificationHelper.EXTRA_CABIN_ID)
        val targetTab = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_TAB)
        if (!cabinId.isNullOrBlank()) {
            val tab = if (targetTab == "TASKS") CabinTabType.TASKS else CabinTabType.CHAT
            viewModel.selectCabinById(cabinId, tab)
            isAppUnlockedState.value = true
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAppUnlockedState.value = true
                    biometricErrorMessageState.value = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        biometricErrorMessageState.value = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricErrorMessageState.value = "لم يتم التعرف على البصمة"
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("شات رواج - القفل الحيوي")
            .setSubtitle("استخدم البصمة أو التعرف على الوجه لفتح الكبائن")
            .setNegativeButtonText("إدخال الرمز السري")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            biometricErrorMessageState.value = "يرجى استخدام رمز PIN لفتح التطبيق"
        }
    }

    fun showBiometricPromptForDecryption(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                        errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT) {
                        onSuccess()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("مصادقة فك تشفير الكبينة الخاصة")
            .setSubtitle("يلزم تأكيد الهوية بالبصمة أو الوجه لعرض الرسائل المشفّرة")
            .setNegativeButtonText("إلغاء")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onSuccess()
        }
    }
}

@Composable
fun PrivateCabinsApp(
    viewModel: CabinsViewModel,
    currentUser: com.example.data.auth.UserEntity? = null,
    onLogout: (() -> Unit)? = null,
    onLockApp: () -> Unit,
    onRequestBiometricDecrypt: (onSuccess: () -> Unit) -> Unit = {}
) {
    val cabins by viewModel.allCabins.collectAsStateWithLifecycle()
    val selectedCabin by viewModel.selectedCabin.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val messages by viewModel.messagesForSelectedCabin.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedCabin.collectAsStateWithLifecycle()
    val allTasksAcrossCabins by viewModel.allTasksAcrossCabins.collectAsStateWithLifecycle()
    val isDecryptedView by viewModel.isDecryptedView.collectAsStateWithLifecycle()
    val audioPodState by viewModel.audioPodState.collectAsStateWithLifecycle()
    val keyVault by viewModel.keyVault.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showCreateCabinDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showDndDialog by remember { mutableStateOf(false) }
    var showCustomizeAppearanceDialog by remember { mutableStateOf(false) }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var showRestoreBackupDialog by remember { mutableStateOf(false) }
    var showQrShareDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var openCentralizedTasksMode by remember { mutableStateOf(false) }
    var exportedBackupContent by remember { mutableStateOf<String?>(null) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CabinDrawerContent(
                cabins = cabins,
                selectedCabin = selectedCabin,
                onSelectCabin = { cabin ->
                    viewModel.selectCabin(cabin)
                    openCentralizedTasksMode = false
                    scope.launch { drawerState.close() }
                },
                onOpenCreateCabinDialog = {
                    showCreateCabinDialog = true
                    scope.launch { drawerState.close() }
                },
                onArchiveCabin = { cabin -> viewModel.archiveCabin(cabin) },
                onUnarchiveCabin = { cabin -> viewModel.unarchiveCabin(cabin) },
                onOpenQrScanner = {
                    showQrScannerDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenQrShare = {
                    showQrShareDialog = true
                    scope.launch { drawerState.close() }
                },
                onOpenCentralizedTasks = {
                    openCentralizedTasksMode = true
                    viewModel.selectTab(CabinTabType.TASKS)
                    scope.launch { drawerState.close() }
                },
                totalTasksCount = allTasksAcrossCabins.size
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    CabinHeader(
                        cabin = selectedCabin,
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        isDecryptedView = isDecryptedView,
                        onToggleCipherView = {
                            if (isDecryptedView) {
                                viewModel.setDecryptedView(false)
                            } else {
                                onRequestBiometricDecrypt {
                                    viewModel.setDecryptedView(true)
                                }
                            }
                        },
                        taskCount = tasks.count { it.status != com.example.data.TaskStatus.DONE },
                        audioPodState = audioPodState,
                        onArchiveCabin = { selectedCabin?.let { viewModel.archiveCabin(it) } },
                        onOpenDndDialog = { showDndDialog = true },
                        onOpenCustomizeAppearance = { showCustomizeAppearanceDialog = true },
                        onOpenQrShare = { showQrShareDialog = true },
                        onOpenQrScanner = { showQrScannerDialog = true }
                    )
                    SyncStatusBanner(
                        syncState = syncState,
                        onToggleConnectivity = { viewModel.toggleConnectivity() },
                        onTriggerManualSync = { viewModel.triggerManualSync() }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    CabinTabType.CHAT -> {
                        ChatTab(
                            cabin = selectedCabin,
                            messages = messages,
                            isDecryptedView = isDecryptedView,
                            onSendMessage = { text -> viewModel.sendMessage(text) },
                            onOpenAddTaskDialog = { showCreateTaskDialog = true },
                            onRequestBiometricDecrypt = {
                                onRequestBiometricDecrypt {
                                    viewModel.setDecryptedView(true)
                                }
                            },
                            onToggleReaction = { message, emoji ->
                                viewModel.toggleReaction(message, emoji)
                            },
                            onShareAttachment = { uri, isImage ->
                                viewModel.shareAttachment(uri, isImage)
                            },
                            onDecryptAttachmentBitmap = { msg ->
                                viewModel.decryptAttachmentBitmap(msg)
                            }
                        )
                    }

                    CabinTabType.TASKS -> {
                        TasksTab(
                            tasks = tasks,
                            cabinName = selectedCabin?.name ?: "الكبينة الحالية",
                            onUpdateTaskStatus = { task, status -> viewModel.updateTaskStatus(task, status) },
                            onDeleteTask = { task -> viewModel.deleteTask(task.id) },
                            onOpenAddTaskDialog = { showCreateTaskDialog = true },
                            onUpdateTask = { updated -> viewModel.updateTask(updated) },
                            allTasksAcrossCabins = allTasksAcrossCabins,
                            allCabins = cabins,
                            onToggleTaskCompletion = { task -> viewModel.toggleTaskCompletion(task) },
                            onSelectCabin = { cabin -> viewModel.selectCabin(cabin) },
                            initialCentralizedView = openCentralizedTasksMode
                        )
                    }

                    CabinTabType.AUDIO_POD -> {
                        AudioPodTab(
                            state = audioPodState,
                            onToggleJoin = { viewModel.toggleAudioPodJoin() },
                            onToggleMute = { viewModel.toggleAudioPodMute() },
                            onToggleSpeakerMute = { viewModel.toggleAudioPodSpeakerMute() },
                            onToggleConnectionStatus = { viewModel.toggleAudioPodConnectionStatus() }
                        )
                    }

                    CabinTabType.SECURITY_VAULT -> {
                        SecurityVaultTab(
                            keyVault = keyVault,
                            onRotateKeys = { viewModel.rotateE2EEKeys() },
                            onLockApp = onLockApp,
                            onExportBackup = {
                                exportedBackupContent = null
                                showExportBackupDialog = true
                            },
                            onRestoreBackup = {
                                restoreResultMessage = null
                                showRestoreBackupDialog = true
                            },
                            fcmToken = fcmToken,
                            onSimulatePushMessage = { viewModel.simulatePushMessage() },
                            onSimulatePushTask = { viewModel.simulatePushTask() },
                            currentUser = currentUser,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }

    if (showCreateCabinDialog) {
        CreateCabinDialog(
            onDismiss = { showCreateCabinDialog = false },
            onCreateCabin = { name, desc, type, color, emoji, tags ->
                viewModel.createCabin(name, desc, type, color, emoji, tags)
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateTaskDialog = false },
            onCreateTask = { title, desc, assignee, priority, dueDate ->
                viewModel.createTask(title, desc, assignee, priority, dueDate)
            }
        )
    }

    if (showDndDialog && selectedCabin != null) {
        CabinDndDialog(
            cabinName = selectedCabin!!.name,
            isCurrentlyDnd = selectedCabin!!.isDndActive(),
            onDismiss = { showDndDialog = false },
            onSetDndMode = { enabled, hours ->
                selectedCabin?.let { viewModel.setDndMode(it, enabled, hours) }
            }
        )
    }

    if (showCustomizeAppearanceDialog && selectedCabin != null) {
        CustomizeCabinAppearanceDialog(
            cabin = selectedCabin!!,
            onDismiss = { showCustomizeAppearanceDialog = false },
            onSaveAppearance = { colorHex, iconEmoji ->
                selectedCabin?.let { viewModel.updateCabinAppearance(it, colorHex, iconEmoji) }
            }
        )
    }

    if (showExportBackupDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ExportBackupDialog(
            onDismiss = { showExportBackupDialog = false },
            onGenerateBackup = { password ->
                viewModel.exportBackup(password) { exported ->
                    exportedBackupContent = exported
                }
            },
            exportedText = exportedBackupContent,
            onCopyToClipboard = { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("EncryptedBackup", text)
                clipboard.setPrimaryClip(clip)
            }
        )
    }

    if (showRestoreBackupDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreBackupDialog = false },
            onRestore = { backupJson, password ->
                viewModel.restoreBackup(backupJson, password) { success, msg ->
                    restoreResultMessage = msg
                }
            },
            resultMessage = restoreResultMessage
        )
    }

    if (showQrShareDialog && selectedCabin != null) {
        CabinQrShareDialog(
            cabin = selectedCabin!!,
            onDismiss = { showQrShareDialog = false }
        )
    }

    if (showQrScannerDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        CabinQrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onJoinCabinByPayload = { payload ->
                viewModel.joinCabinByQrPayload(payload) { success, message ->
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
