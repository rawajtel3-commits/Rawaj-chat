package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.auth.AuthAuditLog
import com.example.data.auth.OtpChannel
import com.example.util.AuthSecurityManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showAuditLogsDialog by remember { mutableStateOf(false) }

    // If user is already authenticated, notify parent
    LaunchedEffect(currentUser) {
        if (currentUser != null && !currentUser!!.isLockedOut()) {
            onAuthenticationSuccess()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "أيقونة الحماية",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "بوابة الحماية والمصادقة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نظام التحقق الآمن والتشفير المتطور",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAuditLogsDialog = true },
                            modifier = Modifier.testTag("audit_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "سجل الأمان والتدقيق",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Global alerts (Success / Error / Lockout)
                    AnimatedVisibility(visible = uiState.globalSuccessMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "نجاح",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.globalSuccessMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.globalErrorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "خطأ",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.globalErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Content based on Mode
                    when (uiState.mode) {
                        AuthMode.LOGIN -> LoginContent(
                            state = uiState,
                            viewModel = viewModel
                        )
                        AuthMode.REGISTER -> RegisterContent(
                            state = uiState,
                            viewModel = viewModel
                        )
                        AuthMode.OTP_VERIFY -> OtpVerifyContent(
                            state = uiState,
                            viewModel = viewModel
                        )
                        AuthMode.FORGOT_PASSWORD -> ForgotPasswordContent(
                            state = uiState,
                            viewModel = viewModel
                        )
                        AuthMode.AUDIT_LOGS -> {
                            // Handled in dialog
                        }
                    }
                }

                // Audit Logs Dialog
                if (showAuditLogsDialog) {
                    AuditLogsDialog(
                        logs = auditLogs,
                        onDismiss = { showAuditLogsDialog = false }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. LOGIN CONTENT
// -------------------------------------------------------------

@Composable
private fun LoginContent(
    state: AuthUiState,
    viewModel: AuthViewModel
) {
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل تسجيل الدخول",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "تسجيل الدخول",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "أدخل بياناتك للوصول إلى كبائنك المشفرة بأمان",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Lockout countdown alert
            if (state.lockoutSecondsRemaining > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "الحساب مقفل مؤقتاً. المتبقي: ${state.lockoutSecondsRemaining} ثانية",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Username or Email Input
            OutlinedTextField(
                value = state.loginIdentifier,
                onValueChange = { viewModel.onLoginIdentifierChanged(it) },
                label = { Text("اسم المستخدم أو البريد الإلكتروني") },
                placeholder = { Text("admin أو user@example.com") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            OutlinedTextField(
                value = state.loginPassword,
                onValueChange = { viewModel.onLoginPasswordChanged(it) },
                label = { Text("كلمة المرور") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleLoginPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.loginPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                        )
                    }
                },
                visualTransformation = if (state.loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.submitLogin()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Remember Me & Forgot Password Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.toggleRememberMe(!state.rememberMe) }
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = { viewModel.toggleRememberMe(it) },
                        modifier = Modifier.testTag("login_remember_me_checkbox")
                    )
                    Text(
                        text = "تذكرني",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TextButton(
                    onClick = { viewModel.switchMode(AuthMode.FORGOT_PASSWORD) },
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text("نسيت كلمة المرور؟", style = MaterialTheme.typography.bodySmall)
                }
            }

            // CAPTCHA Card (if required)
            if (state.isCaptchaRequired && state.captchaChallenge != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اختبار التحقق البشري (CAPTCHA)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { viewModel.refreshCaptcha() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث السؤال"
                                )
                            }
                        }

                        Text(
                            text = "لحماية حسابك بعد عدة محاولات خاطئة، أجب عن السؤال الرياضي:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = state.captchaChallenge.questionArabic,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = state.captchaInput,
                                onValueChange = { viewModel.onCaptchaInputChanged(it) },
                                placeholder = { Text("الناتج") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .width(110.dp)
                                    .testTag("captcha_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Login Button
            Button(
                onClick = { viewModel.submitLogin() },
                enabled = !state.isLoading && state.lockoutSecondsRemaining == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جاري التحقق الآمن...")
                } else {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الدخول الآمن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Demo Admin Fill
            OutlinedButton(
                onClick = { viewModel.prefillDemoAdmin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_admin_fill_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تعبئة الحساب التجريبي المسبق (admin)")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switch to Register
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ليس لديك حساب بعد؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { viewModel.switchMode(AuthMode.REGISTER) },
                    modifier = Modifier.testTag("switch_to_register_button")
                ) {
                    Text(
                        text = "إنشاء حساب جديد",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. REGISTER CONTENT
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterContent(
    state: AuthUiState,
    viewModel: AuthViewModel
) {
    val focusManager = LocalFocusManager.current
    var showCountryPicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "إنشاء حساب جديد",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "إنشاء حساب مشفر جديد",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "املأ البيانات بدقة للتفعيل عبر الواتساب والبريد الإلكتروني",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Username
            OutlinedTextField(
                value = state.registerUsername,
                onValueChange = { viewModel.onRegisterUsernameChanged(it) },
                label = { Text("اسم المستخدم (Username)") },
                placeholder = { Text("مثال: ahmed_2026") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                isError = state.usernameValidationMsg != null,
                supportingText = {
                    if (state.usernameValidationMsg != null) {
                        Text(state.usernameValidationMsg, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("3 أحرف على الأقل، حروف إنجليزية وأرقام ورموز (- . _)")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Email
            OutlinedTextField(
                value = state.registerEmail,
                onValueChange = { viewModel.onRegisterEmailChanged(it) },
                label = { Text("البريد الإلكتروني") },
                placeholder = { Text("user@example.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = state.emailValidationMsg != null,
                supportingText = {
                    if (state.emailValidationMsg != null) {
                        Text(state.emailValidationMsg, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Confirm Email
            val emailsMatch = state.registerEmail.isNotBlank() &&
                    state.registerConfirmEmail.isNotBlank() &&
                    state.registerEmail.trim().equals(state.registerConfirmEmail.trim(), ignoreCase = true)

            OutlinedTextField(
                value = state.registerConfirmEmail,
                onValueChange = { viewModel.onRegisterConfirmEmailChanged(it) },
                label = { Text("تأكيد البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Default.MarkEmailRead, contentDescription = null) },
                trailingIcon = {
                    if (emailsMatch) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "متطابق",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                },
                isError = state.registerConfirmEmail.isNotBlank() && !emailsMatch,
                supportingText = {
                    if (state.registerConfirmEmail.isNotBlank() && !emailsMatch) {
                        Text("البريد غير متطابق", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_confirm_email_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. WhatsApp Number (E.164 with Country Picker)
            Text(
                text = "رقم الواتساب للتفعيل الفوري (E.164):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Button
                OutlinedCard(
                    onClick = { showCountryPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("country_picker_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.registerSelectedCountry.flag, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.registerSelectedCountry.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Phone Input
                OutlinedTextField(
                    value = state.registerPhoneNumberOnly,
                    onValueChange = { viewModel.onRegisterPhoneOnlyChanged(it) },
                    placeholder = { Text("مثال: ${state.registerSelectedCountry.example}") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("register_whatsapp_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (state.whatsappValidationMsg != null && state.registerPhoneNumberOnly.isNotBlank()) {
                Text(
                    text = state.whatsappValidationMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Password Input
            OutlinedTextField(
                value = state.registerPassword,
                onValueChange = { viewModel.onRegisterPasswordChanged(it) },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleRegisterPasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.registerPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (state.registerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Password Strength Indicator
            if (state.registerPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                PasswordStrengthMeter(result = state.passwordStrength)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Confirm Password Input
            val passwordsMatch = state.registerPassword.isNotBlank() &&
                    state.registerConfirmPassword.isNotBlank() &&
                    state.registerPassword == state.registerConfirmPassword

            OutlinedTextField(
                value = state.registerConfirmPassword,
                onValueChange = { viewModel.onRegisterConfirmPasswordChanged(it) },
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                trailingIcon = {
                    if (passwordsMatch) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "متطابق",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                },
                visualTransformation = if (state.registerConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = state.registerConfirmPassword.isNotBlank() && !passwordsMatch,
                supportingText = {
                    if (state.registerConfirmPassword.isNotBlank() && !passwordsMatch) {
                        Text("كلمة المرور غير متطابقة", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_confirm_password_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Registration Button
            Button(
                onClick = { viewModel.submitRegister() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري تشفير الحساب وإرسال الرمز...")
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء الحساب ومتابعة التحقق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Back to Login Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "لديك حساب بالفعل؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { viewModel.switchMode(AuthMode.LOGIN) },
                    modifier = Modifier.testTag("switch_to_login_button")
                ) {
                    Text(
                        text = "تسجيل الدخول",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Country Picker Dialog
    if (showCountryPicker) {
        Dialog(onDismissRequest = { showCountryPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "اختر مفتاح الدولة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(viewModel.countryOptions) { country ->
                            ListItem(
                                headlineContent = { Text(country.nameArabic) },
                                leadingContent = { Text(country.flag, fontSize = 24.sp) },
                                trailingContent = {
                                    Text(
                                        text = country.code,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onRegisterCountryChanged(country)
                                        showCountryPicker = false
                                    }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. LIVE PASSWORD STRENGTH METER
// -------------------------------------------------------------

@Composable
fun PasswordStrengthMeter(
    result: AuthSecurityManager.PasswordStrengthResult,
    modifier: Modifier = Modifier
) {
    val barColor by animateColorAsState(
        targetValue = when (result.level) {
            AuthSecurityManager.StrengthLevel.VERY_WEAK -> Color(0xFFD32F2F) // Red
            AuthSecurityManager.StrengthLevel.WEAK -> Color(0xFFF57C00) // Orange
            AuthSecurityManager.StrengthLevel.MEDIUM -> Color(0xFFFBC02D) // Amber
            AuthSecurityManager.StrengthLevel.STRONG -> Color(0xFF388E3C) // Green
            AuthSecurityManager.StrengthLevel.VERY_STRONG -> Color(0xFF00796B) // Teal
        },
        label = "password_strength_color"
    )

    val progress by animateFloatAsState(
        targetValue = result.percentage,
        label = "password_strength_progress"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Strength Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مستوى قوة كلمة المرور:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = barColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = result.level.labelArabic,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Rule Checklist Grid (2 columns)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.rules.chunked(2).forEach { rowRules ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowRules.forEach { rule ->
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (rule.satisfied) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (rule.satisfied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = rule.labelArabic,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (rule.satisfied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. OTP VERIFICATION CONTENT
// -------------------------------------------------------------

@Composable
private fun OtpVerifyContent(
    state: AuthUiState,
    viewModel: AuthViewModel
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "التحقق بخطوتين",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "التحقق الثنائي (OTP)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "أدخل رمز التحقق المكون من 6 أرقام المرسل إليك",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Channel Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.otpChannel == OtpChannel.WHATSAPP) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectOtpChannel(OtpChannel.WHATSAPP) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = if (state.otpChannel == OtpChannel.WHATSAPP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عبر الواتساب",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.otpChannel == OtpChannel.WHATSAPP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.otpChannel == OtpChannel.EMAIL) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectOtpChannel(OtpChannel.EMAIL) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = if (state.otpChannel == OtpChannel.EMAIL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عبر البريد",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.otpChannel == OtpChannel.EMAIL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sandbox Simulated Code Hint for Easy Testing
            if (state.debugLatestOtp != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onOtpInputChanged(state.debugLatestOtp) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "رمز المحاكاة المرسل: ${state.debugLatestOtp} (اضغط للتعبئة التلقائية)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6-digit OTP Input
            OutlinedTextField(
                value = state.otpInput,
                onValueChange = { viewModel.onOtpInputChanged(it) },
                placeholder = { Text("• • • • • •", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_code_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Verify OTP Button
            Button(
                onClick = { viewModel.submitOtpVerification() },
                enabled = !state.isLoading && state.otpInput.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("otp_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التأكيد...")
                } else {
                    Text("تأكيد الرمز وتفعيل الحساب", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resend Countdown
            if (state.resendCountdown > 0) {
                Text(
                    text = "إعادة إرسال رمز جديد متاحة خلال: ${state.resendCountdown} ثانية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(
                    onClick = { viewModel.resendOtp() },
                    modifier = Modifier.testTag("resend_otp_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة إرسال رمز جديد")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { viewModel.switchMode(AuthMode.LOGIN) },
                modifier = Modifier.testTag("otp_back_to_login_button")
            ) {
                Text("العودة لتسجيل الدخول")
            }
        }
    }
}

// -------------------------------------------------------------
// 5. FORGOT PASSWORD CONTENT
// -------------------------------------------------------------

@Composable
private fun ForgotPasswordContent(
    state: AuthUiState,
    viewModel: AuthViewModel
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "استعادة كلمة المرور",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "استعادة كلمة المرور",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (state.forgotPasswordStep == 1) {
                    "أدخل اسم المستخدم أو البريد الإلكتروني لاستلام رمز استعادة آمن"
                } else {
                    "أدخل الرمز المستلم وكلمة المرور الجديدة المتوافقة مع معايير الأمان"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.forgotPasswordStep == 1) {
                // Step 1: Identifier input
                OutlinedTextField(
                    value = state.forgotPasswordIdentifier,
                    onValueChange = { viewModel.onForgotPasswordIdentifierChanged(it) },
                    label = { Text("اسم المستخدم أو البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_identifier_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { viewModel.submitForgotPasswordRequest() },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("forgot_send_code_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري الإرسال...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال رمز الاستعادة", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Step 2: Confirm with OTP + New Password
                if (state.debugLatestOtp != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onForgotPasswordOtpChanged(state.debugLatestOtp) }
                    ) {
                        Text(
                            text = "رمز الاستعادة التجريبي: ${state.debugLatestOtp} (اضغط للتعبئة)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = state.forgotPasswordOtp,
                    onValueChange = { viewModel.onForgotPasswordOtpChanged(it) },
                    label = { Text("رمز التحقق (6 أرقام)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_otp_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.forgotNewPassword,
                    onValueChange = { viewModel.onForgotNewPasswordChanged(it) },
                    label = { Text("كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleForgotPasswordVisibility() }) {
                            Icon(
                                imageVector = if (state.forgotPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (state.forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_new_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (state.forgotNewPassword.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthMeter(result = state.forgotPasswordStrength)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.forgotConfirmNewPassword,
                    onValueChange = { viewModel.onForgotConfirmNewPasswordChanged(it) },
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                    visualTransformation = if (state.forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_confirm_new_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { viewModel.submitConfirmPasswordReset() },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("forgot_confirm_reset_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("تأكيد تغيير كلمة المرور", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.switchMode(AuthMode.LOGIN) },
                modifier = Modifier.testTag("forgot_back_to_login_button")
            ) {
                Text("العودة لتسجيل الدخول")
            }
        }
    }
}

// -------------------------------------------------------------
// 6. AUDIT LOGS DIALOG (SECURITY TRAIL)
// -------------------------------------------------------------

@Composable
fun AuditLogsDialog(
    logs: List<AuthAuditLog>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سجل التدقيق الأمني (Audit Logs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "سجل محمي ومؤرخ لجميع أحداث تسجيل الدخول والحظر والتحقق",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد أحداث أمنية مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            val (badgeColor, badgeLabel) = when (log.eventType) {
                                "LOGIN_SUCCESS" -> Color(0xFF2E7D32) to "دخول ناجح"
                                "LOGIN_FAILED" -> Color(0xFFD32F2F) to "دخول فاشل"
                                "LOCKED_OUT" -> Color(0xFFC2185B) to "حظر مؤقت"
                                "CAPTCHA_TRIGGERED" -> Color(0xFFF57C00) to "تحقق بشري"
                                "REGISTER" -> Color(0xFF1976D2) to "حساب جديد"
                                "OTP_VERIFIED" -> Color(0xFF00796B) to "تأكيد OTP"
                                "PASSWORD_RESET_SUCCESS" -> Color(0xFF512DA8) to "تغيير كلمة المرور"
                                "LOGOUT" -> Color(0xFF616161) to "تسجيل خروج"
                                else -> MaterialTheme.colorScheme.primary to log.eventType
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = badgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = dateFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = log.statusMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "المستخدم: ${log.username}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${log.deviceInfo} (${log.ipAddress})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
