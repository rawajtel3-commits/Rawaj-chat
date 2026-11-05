package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.*
import com.example.util.AuthSecurityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    OTP_VERIFY,
    AUDIT_LOGS
}

data class CountryCodeOption(
    val code: String,
    val flag: String,
    val nameArabic: String,
    val example: String
)

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    val globalSuccessMessage: String? = null,
    val globalErrorMessage: String? = null,

    // Login fields
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isCaptchaRequired: Boolean = false,
    val captchaChallenge: AuthSecurityManager.CaptchaChallenge? = null,
    val captchaInput: String = "",
    val lockoutSecondsRemaining: Int = 0,

    // Registration fields
    val registerUsername: String = "",
    val registerEmail: String = "",
    val registerConfirmEmail: String = "",
    val registerSelectedCountry: CountryCodeOption = CountryCodeOption("+966", "🇸🇦", "السعودية", "501234567"),
    val registerPhoneNumberOnly: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val registerPasswordVisible: Boolean = false,
    val registerConfirmPasswordVisible: Boolean = false,
    val passwordStrength: AuthSecurityManager.PasswordStrengthResult = AuthSecurityManager.evaluatePasswordStrength(""),
    val usernameValidationMsg: String? = null,
    val emailValidationMsg: String? = null,
    val whatsappValidationMsg: String? = null,

    // OTP Verification
    val pendingUserId: String? = null,
    val otpChannel: OtpChannel = OtpChannel.WHATSAPP,
    val otpInput: String = "",
    val resendCountdown: Int = 0,
    val debugLatestOtp: String? = null,

    // Forgot Password
    val forgotPasswordIdentifier: String = "",
    val forgotPasswordStep: Int = 1, // 1: request, 2: confirm
    val forgotPasswordOtp: String = "",
    val forgotNewPassword: String = "",
    val forgotConfirmNewPassword: String = "",
    val forgotPasswordStrength: AuthSecurityManager.PasswordStrengthResult = AuthSecurityManager.evaluatePasswordStrength(""),
    val forgotPasswordVisible: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    val auditLogs: StateFlow<List<AuthAuditLog>> = repository.getAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var lockoutTimerJob: Job? = null

    val countryOptions = listOf(
        CountryCodeOption("+966", "🇸🇦", "السعودية (+966)", "501234567"),
        CountryCodeOption("+971", "🇦🇪", "الإمارات (+971)", "501234567"),
        CountryCodeOption("+965", "🇰🇼", "الكويت (+965)", "50123456"),
        CountryCodeOption("+974", "🇶🇦", "قطر (+974)", "50123456"),
        CountryCodeOption("+973", "🇧🇭", "البحرين (+973)", "39123456"),
        CountryCodeOption("+968", "🇴🇲", "عُمان (+968)", "91234567"),
        CountryCodeOption("+20", "🇪🇬", "مصر (+20)", "1012345678"),
        CountryCodeOption("+962", "🇯🇴", "الأردن (+962)", "791234567")
    )

    init {
        viewModelScope.launch {
            repository.initialize()
            // Check remember me auto-restore
            val restored = repository.tryRestoreRememberMeSession()
            if (restored != null) {
                _uiState.value = _uiState.value.copy(
                    globalSuccessMessage = "أهلاً بك مجدداً ${restored.username}! تم تسجيل دخولك بنجاح."
                )
            }
        }
    }

    fun switchMode(mode: AuthMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            globalErrorMessage = null,
            globalSuccessMessage = null
        )
    }

    // --- Login Handlers ---

    fun onLoginIdentifierChanged(value: String) {
        _uiState.value = _uiState.value.copy(loginIdentifier = value, globalErrorMessage = null)
    }

    fun onLoginPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(loginPassword = value, globalErrorMessage = null)
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.value = _uiState.value.copy(loginPasswordVisible = !_uiState.value.loginPasswordVisible)
    }

    fun toggleRememberMe(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
    }

    fun onCaptchaInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(captchaInput = value)
    }

    fun refreshCaptcha() {
        val challenge = repository.createCaptchaChallenge()
        _uiState.value = _uiState.value.copy(captchaChallenge = challenge, captchaInput = "")
    }

    fun prefillDemoAdmin() {
        _uiState.value = _uiState.value.copy(
            loginIdentifier = "admin",
            loginPassword = "Admin@1234#",
            globalErrorMessage = null
        )
    }

    fun submitLogin() {
        val state = _uiState.value
        if (state.loginIdentifier.isBlank() || state.loginPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(globalErrorMessage = "يرجى ملء جميع الحقول المطلوبة")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, globalErrorMessage = null)

            val captchaTokenAnswer = if (state.captchaChallenge != null) {
                "${state.captchaChallenge.token}:${state.captchaInput}"
            } else null

            val response = repository.login(
                LoginRequest(
                    usernameOrEmail = state.loginIdentifier,
                    password = state.loginPassword,
                    rememberMe = state.rememberMe,
                    captchaResponse = captchaTokenAnswer
                )
            )

            if (response.success && response.data != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalSuccessMessage = "تم تسجيل الدخول بنجاح! مرحباً ${response.data.user.username}",
                    globalErrorMessage = null,
                    isCaptchaRequired = false,
                    captchaChallenge = null,
                    captchaInput = ""
                )
            } else {
                val isLocked = response.status == 429
                val needsCaptcha = response.status == 403 || response.error?.code == "CAPTCHA_REQUIRED"

                val challenge = if (needsCaptcha) repository.createCaptchaChallenge() else state.captchaChallenge

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalErrorMessage = response.error?.messageArabic ?: "فشل تسجيل الدخول",
                    isCaptchaRequired = needsCaptcha,
                    captchaChallenge = challenge,
                    captchaInput = ""
                )

                if (isLocked) {
                    startLockoutTimer(30)
                }
            }
        }
    }

    private fun startLockoutTimer(seconds: Int) {
        lockoutTimerJob?.cancel()
        lockoutTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(lockoutSecondsRemaining = remaining)
                delay(1000)
                remaining--
            }
            _uiState.value = _uiState.value.copy(lockoutSecondsRemaining = 0, globalErrorMessage = null)
        }
    }

    // --- Registration Handlers ---

    fun onRegisterUsernameChanged(value: String) {
        val (_, error) = AuthSecurityManager.validateUsername(value)
        _uiState.value = _uiState.value.copy(
            registerUsername = value,
            usernameValidationMsg = error,
            globalErrorMessage = null
        )
    }

    fun onRegisterEmailChanged(value: String) {
        val (_, error) = AuthSecurityManager.validateEmail(value)
        _uiState.value = _uiState.value.copy(
            registerEmail = value,
            emailValidationMsg = error,
            globalErrorMessage = null
        )
    }

    fun onRegisterConfirmEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            registerConfirmEmail = value,
            globalErrorMessage = null
        )
    }

    fun onRegisterCountryChanged(country: CountryCodeOption) {
        _uiState.value = _uiState.value.copy(registerSelectedCountry = country)
        revalidatePhone()
    }

    fun onRegisterPhoneOnlyChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(registerPhoneNumberOnly = digitsOnly)
        revalidatePhone()
    }

    private fun revalidatePhone() {
        val fullNumber = "${_uiState.value.registerSelectedCountry.code}${_uiState.value.registerPhoneNumberOnly}"
        val (_, error) = AuthSecurityManager.validateWhatsAppNumber(fullNumber)
        _uiState.value = _uiState.value.copy(whatsappValidationMsg = error)
    }

    fun onRegisterPasswordChanged(value: String) {
        val strength = AuthSecurityManager.evaluatePasswordStrength(value)
        _uiState.value = _uiState.value.copy(
            registerPassword = value,
            passwordStrength = strength,
            globalErrorMessage = null
        )
    }

    fun onRegisterConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            registerConfirmPassword = value,
            globalErrorMessage = null
        )
    }

    fun toggleRegisterPasswordVisibility() {
        _uiState.value = _uiState.value.copy(registerPasswordVisible = !_uiState.value.registerPasswordVisible)
    }

    fun toggleRegisterConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(registerConfirmPasswordVisible = !_uiState.value.registerConfirmPasswordVisible)
    }

    fun submitRegister() {
        val state = _uiState.value
        val fullPhone = "${state.registerSelectedCountry.code}${state.registerPhoneNumberOnly}"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, globalErrorMessage = null)

            val request = RegisterRequest(
                username = state.registerUsername,
                email = state.registerEmail,
                confirmEmail = state.registerConfirmEmail,
                whatsappNumber = fullPhone,
                password = state.registerPassword,
                confirmPassword = state.registerConfirmPassword
            )

            val response = repository.registerUser(request)
            if (response.success && response.data != null) {
                // Fetch simulated debug OTP for convenience
                val debugOtp = repository.getPendingOtpForDebug(response.data.userId, OtpChannel.WHATSAPP)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mode = AuthMode.OTP_VERIFY,
                    pendingUserId = response.data.userId,
                    otpChannel = OtpChannel.WHATSAPP,
                    otpInput = "",
                    debugLatestOtp = debugOtp,
                    globalSuccessMessage = response.data.messageArabic,
                    globalErrorMessage = null
                )
                startOtpResendCountdown()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalErrorMessage = response.error?.messageArabic ?: "فشل تسجيل الحساب"
                )
            }
        }
    }

    // --- OTP Verification Handlers ---

    fun onOtpInputChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(otpInput = digits, globalErrorMessage = null)
    }

    fun selectOtpChannel(channel: OtpChannel) {
        val userId = _uiState.value.pendingUserId ?: return
        val debugOtp = repository.getPendingOtpForDebug(userId, channel)
        _uiState.value = _uiState.value.copy(
            otpChannel = channel,
            otpInput = "",
            debugLatestOtp = debugOtp,
            globalErrorMessage = null
        )
    }

    fun submitOtpVerification() {
        val state = _uiState.value
        val userId = state.pendingUserId ?: return

        if (state.otpInput.length < 6) {
            _uiState.value = _uiState.value.copy(globalErrorMessage = "يرجى إدخال رمز التحقق المكون من 6 أرقام")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, globalErrorMessage = null)

            val request = VerifyOtpRequest(
                userId = userId,
                channel = state.otpChannel,
                otpCode = state.otpInput
            )

            val response = repository.verifyOtp(request)
            if (response.success && response.data != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mode = AuthMode.LOGIN,
                    globalSuccessMessage = "${response.data.messageArabic}! يمكنك الآن تسجيل الدخول مباشرة.",
                    globalErrorMessage = null,
                    otpInput = ""
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalErrorMessage = response.error?.messageArabic ?: "رمز التحقق غير صحيح"
                )
            }
        }
    }

    fun resendOtp() {
        val state = _uiState.value
        val userId = state.pendingUserId ?: return
        if (state.resendCountdown > 0) return

        viewModelScope.launch {
            val response = repository.resendOtp(userId, state.otpChannel)
            if (response.success) {
                val debugOtp = repository.getPendingOtpForDebug(userId, state.otpChannel)
                _uiState.value = _uiState.value.copy(
                    debugLatestOtp = debugOtp,
                    globalSuccessMessage = response.data,
                    globalErrorMessage = null
                )
                startOtpResendCountdown()
            } else {
                _uiState.value = _uiState.value.copy(
                    globalErrorMessage = response.error?.messageArabic ?: "فشل إعادة الإرسال"
                )
            }
        }
    }

    private fun startOtpResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var count = 60
            while (count > 0) {
                _uiState.value = _uiState.value.copy(resendCountdown = count)
                delay(1000)
                count--
            }
            _uiState.value = _uiState.value.copy(resendCountdown = 0)
        }
    }

    // --- Forgot Password Handlers ---

    fun onForgotPasswordIdentifierChanged(value: String) {
        _uiState.value = _uiState.value.copy(forgotPasswordIdentifier = value, globalErrorMessage = null)
    }

    fun onForgotPasswordOtpChanged(value: String) {
        _uiState.value = _uiState.value.copy(forgotPasswordOtp = value.filter { it.isDigit() }.take(6))
    }

    fun onForgotNewPasswordChanged(value: String) {
        val strength = AuthSecurityManager.evaluatePasswordStrength(value)
        _uiState.value = _uiState.value.copy(
            forgotNewPassword = value,
            forgotPasswordStrength = strength,
            globalErrorMessage = null
        )
    }

    fun onForgotConfirmNewPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(forgotConfirmNewPassword = value)
    }

    fun toggleForgotPasswordVisibility() {
        _uiState.value = _uiState.value.copy(forgotPasswordVisible = !_uiState.value.forgotPasswordVisible)
    }

    fun submitForgotPasswordRequest() {
        val query = _uiState.value.forgotPasswordIdentifier.trim()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(globalErrorMessage = "يرجى إدخال اسم المستخدم أو البريد الإلكتروني")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, globalErrorMessage = null)
            val response = repository.requestPasswordReset(query)
            val debugOtp = repository.getPendingResetOtpForDebug(query)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                forgotPasswordStep = 2,
                debugLatestOtp = debugOtp,
                globalSuccessMessage = response.data,
                globalErrorMessage = null
            )
        }
    }

    fun submitConfirmPasswordReset() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, globalErrorMessage = null)

            val request = ResetPasswordConfirmRequest(
                usernameOrEmail = state.forgotPasswordIdentifier,
                otpCode = state.forgotPasswordOtp,
                newPassword = state.forgotNewPassword,
                confirmNewPassword = state.forgotConfirmNewPassword
            )

            val response = repository.confirmPasswordReset(request)
            if (response.success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mode = AuthMode.LOGIN,
                    forgotPasswordStep = 1,
                    forgotPasswordIdentifier = "",
                    forgotPasswordOtp = "",
                    forgotNewPassword = "",
                    forgotConfirmNewPassword = "",
                    globalSuccessMessage = response.data,
                    globalErrorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalErrorMessage = response.error?.messageArabic ?: "فشل تعيين كلمة المرور"
                )
            }
        }
    }

    // --- Logout ---

    fun logout() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.logout(user.id)
            _uiState.value = AuthUiState(
                mode = AuthMode.LOGIN,
                globalSuccessMessage = "تم تسجيل الخروج الآمن بنجاح."
            )
        }
    }
}
