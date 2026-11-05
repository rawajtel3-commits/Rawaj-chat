package com.example.data.auth

import com.example.util.AuthSecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Enterprise Authentication & Identity Repository
 * Enforces rate-limiting, CAPTCHA gating, salted PBKDF2 hashing,
 * dual WhatsApp/Email OTP verification, CSRF tokens, and security audit logs.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val auditDao: AuthAuditLogDao
) {
    // Current authenticated user session state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Active CSRF token bound to current session
    private var currentCsrfToken: String = AuthSecurityManager.generateSecureToken()

    // Pending OTP records: Key = "${userId}_${channel.name}", Value = OtpRecord
    private val pendingOtps = ConcurrentHashMap<String, OtpRecord>()

    // Pending Password Reset OTPs: Key = usernameOrEmail, Value = OtpRecord
    private val pendingResetOtps = ConcurrentHashMap<String, OtpRecord>()

    // Active CAPTCHAs: Key = token, Value = expected answer
    private val activeCaptchas = ConcurrentHashMap<String, String>()

    data class OtpRecord(
        val code: String,
        val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    }

    suspend fun initialize() {
        // Seed default demo user if database is empty
        if (userDao.getUserCount() == 0) {
            val salt = AuthSecurityManager.generateSalt()
            val hash = AuthSecurityManager.hashPassword("Admin@1234#", salt)
            val demoUser = UserEntity(
                id = UUID.randomUUID().toString(),
                username = "admin",
                email = "admin@cabins.app",
                whatsappNumber = "+966501234567",
                passwordHashHex = hash,
                passwordSaltHex = AuthSecurityManager.bytesToHex(salt),
                isEmailVerified = true,
                isWhatsappVerified = true,
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(demoUser)
            logAudit(demoUser.username, "SEED_DEMO_ACCOUNT", "تم إنشاء الحساب الإداري التجريبي التلقائي (admin)")
        }
    }

    // --- Audit Logging ---

    private suspend fun logAudit(username: String, eventType: String, message: String) {
        try {
            val log = AuthAuditLog(
                username = username,
                eventType = eventType,
                statusMessage = message
            )
            auditDao.insertLog(log)
        } catch (_: Exception) {}
    }

    fun getAuditLogsFlow(): Flow<List<AuthAuditLog>> = auditDao.getRecentLogsFlow(30)

    // --- CAPTCHA Generation & Verification ---

    fun createCaptchaChallenge(): AuthSecurityManager.CaptchaChallenge {
        val challenge = AuthSecurityManager.generateCaptcha()
        activeCaptchas[challenge.token] = challenge.expectedAnswer
        return challenge
    }

    private fun verifyCaptcha(token: String?, answer: String?): Boolean {
        if (token == null || answer == null) return false
        val expected = activeCaptchas.remove(token) ?: return false
        return expected.trim() == answer.trim()
    }

    // --- Registration Flow ---

    suspend fun registerUser(request: RegisterRequest): ApiResponse<RegisterResponse> {
        // 1. Validate Username
        val (isUserValid, userError) = AuthSecurityManager.validateUsername(request.username)
        if (!isUserValid) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("INVALID_USERNAME", userError ?: "اسم المستخدم غير صالح")
            )
        }

        // Check Username Duplication
        val existingUser = userDao.getUserByUsername(request.username.trim())
        if (existingUser != null) {
            return ApiResponse(
                status = 409,
                success = false,
                error = ApiError("USERNAME_EXISTS", "اسم المستخدم مسجل مسبقاً، يرجى اختيار اسم آخر")
            )
        }

        // 2. Validate Email
        val (isEmailValid, emailError) = AuthSecurityManager.validateEmail(request.email)
        if (!isEmailValid) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("INVALID_EMAIL", emailError ?: "البريد الإلكتروني غير صالح")
            )
        }

        if (request.email.trim().lowercase() != request.confirmEmail.trim().lowercase()) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("EMAIL_MISMATCH", "البريد الإلكتروني وتأكيد البريد غير متطابقين")
            )
        }

        // Check Email Duplication
        val existingEmail = userDao.getUserByEmail(request.email.trim())
        if (existingEmail != null) {
            return ApiResponse(
                status = 409,
                success = false,
                error = ApiError("EMAIL_EXISTS", "البريد الإلكتروني مسجل مسبقاً لحساب آخر")
            )
        }

        // 3. Validate WhatsApp Number
        val (isPhoneValid, phoneError) = AuthSecurityManager.validateWhatsAppNumber(request.whatsappNumber)
        if (!isPhoneValid) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("INVALID_WHATSAPP", phoneError ?: "رقم الواتساب غير صالح")
            )
        }

        val formattedPhone = request.whatsappNumber.trim().replace(" ", "").replace("-", "")
        val existingPhone = userDao.getUserByWhatsapp(formattedPhone)
        if (existingPhone != null) {
            return ApiResponse(
                status = 409,
                success = false,
                error = ApiError("WHATSAPP_EXISTS", "رقم الواتساب مسجل مسبقاً لحساب آخر")
            )
        }

        // 4. Validate Password & Confirmation
        if (request.password != request.confirmPassword) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("PASSWORD_MISMATCH", "كلمة المرور وتأكيد كلمة المرور غير متطابقتين")
            )
        }

        val strength = AuthSecurityManager.evaluatePasswordStrength(request.password)
        if (!strength.isAcceptable) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("WEAK_PASSWORD", strength.errorMessageArabic ?: "كلمة المرور لا تحقق معايير الأمان المطلوبة")
            )
        }

        // 5. Generate Salt & PBKDF2 Hash
        val salt = AuthSecurityManager.generateSalt()
        val hash = AuthSecurityManager.hashPassword(request.password, salt)
        val newUserId = UUID.randomUUID().toString()

        val newUser = UserEntity(
            id = newUserId,
            username = request.username.trim(),
            email = request.email.trim().lowercase(),
            whatsappNumber = formattedPhone,
            passwordHashHex = hash,
            passwordSaltHex = AuthSecurityManager.bytesToHex(salt),
            isEmailVerified = false,
            isWhatsappVerified = false,
            createdAt = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)

        // Generate OTPs for WhatsApp and Email
        val whatsappOtp = AuthSecurityManager.generateOtp(6)
        val emailOtp = AuthSecurityManager.generateOtp(6)

        pendingOtps["${newUserId}_${OtpChannel.WHATSAPP.name}"] = OtpRecord(whatsappOtp)
        pendingOtps["${newUserId}_${OtpChannel.EMAIL.name}"] = OtpRecord(emailOtp)

        logAudit(newUser.username, "REGISTER", "تم تسجيل مستخدم جديد بنجاح، بانتظار التحقق عبر الواتساب والبريد")

        val responseData = RegisterResponse(
            userId = newUserId,
            username = newUser.username,
            email = newUser.email,
            whatsappNumber = newUser.whatsappNumber,
            emailVerificationRequired = true,
            whatsappOtpSent = true,
            messageArabic = "تم إنشاء الحساب بنجاح. تم إرسال رمز التحقق إلى رقم الواتساب وبريدك الإلكتروني."
        )

        return ApiResponse(status = 201, success = true, data = responseData)
    }

    // --- Login Flow ---

    suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> {
        val query = request.usernameOrEmail.trim()
        val user = if (query.contains("@")) {
            userDao.getUserByEmail(query)
        } else {
            userDao.getUserByUsername(query)
        }

        if (user == null) {
            logAudit(query, "LOGIN_FAILED", "محاولة تسجيل دخول لاسم مستخدم غير موجود")
            return ApiResponse(
                status = 401,
                success = false,
                error = ApiError("INVALID_CREDENTIALS", "اسم المستخدم أو كلمة المرور غير صحيحة")
            )
        }

        // Check Lockout
        if (user.isLockedOut()) {
            val remainingSec = user.remainingLockoutSeconds()
            logAudit(user.username, "LOCKED_OUT_ATTEMPT", "محاولة تسجيل أثناء فترة الحظر المؤقت")
            return ApiResponse(
                status = 429,
                success = false,
                error = ApiError(
                    "ACCOUNT_LOCKED",
                    "تم إغلاق الحساب مؤقتاً لحمايته بسبب تكرار المحاولات الخاطئة. يرجى المحاولة بعد $remainingSec ثانية."
                )
            )
        }

        // Check if CAPTCHA is required (triggered after 3 failed attempts)
        if (user.failedAttempts >= 3) {
            val captchaSolved = verifyCaptcha(request.captchaResponse?.substringBefore(":"), request.captchaResponse?.substringAfter(":"))
            if (!captchaSolved) {
                logAudit(user.username, "CAPTCHA_FAILED", "فشل أو تخطي التحقق البشري (CAPTCHA)")
                return ApiResponse(
                    status = 403,
                    success = false,
                    error = ApiError("CAPTCHA_REQUIRED", "يرجى حل اختبار التحقق البشري (CAPTCHA) للمتابعة بأمان")
                )
            }
        }

        // Verify PBKDF2 Password Hash
        val isPasswordCorrect = AuthSecurityManager.verifyPassword(
            password = request.password,
            saltHex = user.passwordSaltHex,
            expectedHashHex = user.passwordHashHex
        )

        if (!isPasswordCorrect) {
            val newFailed = user.failedAttempts + 1
            val lockDuration = if (newFailed >= 5) 30_000L else 0L // 30 seconds lockout after 5 failures
            val lockoutTime = if (lockDuration > 0) System.currentTimeMillis() + lockDuration else 0L

            val updatedUser = user.copy(
                failedAttempts = newFailed,
                lockoutUntilMillis = lockoutTime
            )
            userDao.updateUser(updatedUser)

            val errorMsg = if (newFailed >= 5) {
                logAudit(user.username, "LOCKED_OUT", "تم حظر الحساب مؤقتاً لمدة 30 ثانية بسبب 5 محاولات فاشلة")
                "تم إيقاف الحساب مؤقتاً لمدة 30 ثانية بسبب 5 محاولات دخول غير صحيحة متتالية."
            } else if (newFailed >= 3) {
                logAudit(user.username, "CAPTCHA_TRIGGERED", "تفعيل التحقق البشري بعد $newFailed محاولات فاشلة")
                "كلمة المرور غير صحيحة. تم تفعيل رمز التحقق البشري لحماية حسابك (المحاولة $newFailed من 5)."
            } else {
                logAudit(user.username, "LOGIN_FAILED", "كلمة مرور غير صحيحة (المحاولة $newFailed)")
                "اسم المستخدم أو كلمة المرور غير صحيحة (المتبقي ${5 - newFailed} محاولات قبل الإغلاق المؤقت)."
            }

            return ApiResponse(
                status = 401,
                success = false,
                error = ApiError("INVALID_CREDENTIALS", errorMsg)
            )
        }

        // Login Success: Reset lock counters, issue session token
        val sessionToken = AuthSecurityManager.generateSecureToken()
        val csrfToken = AuthSecurityManager.generateSecureToken()
        currentCsrfToken = csrfToken

        val updatedUser = user.copy(
            failedAttempts = 0,
            lockoutUntilMillis = 0L,
            lastLoginAt = System.currentTimeMillis(),
            sessionToken = sessionToken,
            rememberMe = request.rememberMe
        )
        userDao.updateUser(updatedUser)
        _currentUser.value = updatedUser

        logAudit(user.username, "LOGIN_SUCCESS", "تسجيل دخول ناجح وآمن للمستخدم")

        val loginResponse = LoginResponse(
            token = sessionToken,
            refreshToken = AuthSecurityManager.generateSecureToken(),
            csrfToken = csrfToken,
            expiresInSeconds = if (request.rememberMe) 30L * 24 * 3600 else 24 * 3600,
            user = UserDto(
                id = updatedUser.id,
                username = updatedUser.username,
                email = updatedUser.email,
                whatsappNumber = updatedUser.whatsappNumber,
                isEmailVerified = updatedUser.isEmailVerified,
                isWhatsappVerified = updatedUser.isWhatsappVerified
            )
        )

        return ApiResponse(status = 200, success = true, data = loginResponse)
    }

    // --- OTP Verification Flow ---

    fun getPendingOtpForDebug(userId: String, channel: OtpChannel): String? {
        return pendingOtps["${userId}_${channel.name}"]?.code
    }

    fun getPendingResetOtpForDebug(usernameOrEmail: String): String? {
        return pendingResetOtps[usernameOrEmail.trim().lowercase()]?.code
    }

    suspend fun verifyOtp(request: VerifyOtpRequest): ApiResponse<VerifyOtpResponse> {
        val key = "${request.userId}_${request.channel.name}"
        val record = pendingOtps[key]

        if (record == null) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("OTP_NOT_FOUND", "لا يوجد رمز تحقق معلق أو انتهت صلاحيته، يرجى طلب رمز جديد")
            )
        }

        if (record.isExpired) {
            pendingOtps.remove(key)
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("OTP_EXPIRED", "انتهت صلاحية رمز التحقق (أكثر من 5 دقائق)، يرجى طلب رمز جديد")
            )
        }

        if (record.code != request.otpCode.trim()) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("INVALID_OTP", "رمز التحقق المدخل غير صحيح، يرجى التأكد وإعادة المحاولة")
            )
        }

        // OTP is valid!
        pendingOtps.remove(key)
        val user = userDao.getUserById(request.userId)
        if (user != null) {
            val updated = when (request.channel) {
                OtpChannel.WHATSAPP -> user.copy(isWhatsappVerified = true)
                OtpChannel.EMAIL -> user.copy(isEmailVerified = true)
            }
            userDao.updateUser(updated)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = updated
            }
            logAudit(user.username, "OTP_VERIFIED", "تم تأكيد التحقق بنجاح عبر قناة ${request.channel.name}")
        }

        return ApiResponse(
            status = 200,
            success = true,
            data = VerifyOtpResponse(
                verified = true,
                channel = request.channel,
                messageArabic = "تم التحقق بنجاح من ${if (request.channel == OtpChannel.WHATSAPP) "رقم الواتساب" else "البريد الإلكتروني"}"
            )
        )
    }

    suspend fun resendOtp(userId: String, channel: OtpChannel): ApiResponse<String> {
        val user = userDao.getUserById(userId)
            ?: return ApiResponse(status = 404, success = false, error = ApiError("USER_NOT_FOUND", "المستخدم غير موجود"))

        val newCode = AuthSecurityManager.generateOtp(6)
        pendingOtps["${userId}_${channel.name}"] = OtpRecord(newCode)

        val channelLabel = if (channel == OtpChannel.WHATSAPP) "الواتساب" else "البريد الإلكتروني"
        logAudit(user.username, "OTP_RESENT", "تمت إعادة إرسال رمز التحقق إلى $channelLabel")

        return ApiResponse(
            status = 200,
            success = true,
            data = "تم إرسال رمز تحقق جديد إلى $channelLabel بنجاح"
        )
    }

    // --- Password Recovery Flow ---

    suspend fun requestPasswordReset(usernameOrEmail: String): ApiResponse<String> {
        val query = usernameOrEmail.trim()
        val user = if (query.contains("@")) {
            userDao.getUserByEmail(query)
        } else {
            userDao.getUserByUsername(query)
        }

        if (user == null) {
            // For security, do not disclose whether user exists
            return ApiResponse(
                status = 200,
                success = true,
                data = "إذا كان هذا الحساب مسجلاً، فقد تم إرسال رمز استعادة كلمة المرور إلى الواتساب والبريد."
            )
        }

        val otp = AuthSecurityManager.generateOtp(6)
        pendingResetOtps[user.username.lowercase()] = OtpRecord(otp)
        pendingResetOtps[user.email.lowercase()] = OtpRecord(otp)

        logAudit(user.username, "PASSWORD_RESET_REQUESTED", "تم طلب استعادة كلمة المرور وإرسال رمز التحقق")

        return ApiResponse(
            status = 200,
            success = true,
            data = "تم إرسال رمز استعادة كلمة المرور بنجاح إلى رقم الواتساب والبريد الإلكتروني المسجلين."
        )
    }

    suspend fun confirmPasswordReset(request: ResetPasswordConfirmRequest): ApiResponse<String> {
        val query = request.usernameOrEmail.trim().lowercase()
        val record = pendingResetOtps[query]

        if (record == null || record.isExpired) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("INVALID_RESET_OTP", "رمز الاستعادة غير صالح أو انتهت صلاحيته")
            )
        }

        if (record.code != request.otpCode.trim()) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("OTP_MISMATCH", "رمز الاستعادة المدخل غير مطابق")
            )
        }

        if (request.newPassword != request.confirmNewPassword) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("PASSWORD_MISMATCH", "كلمة المرور وتأكيد كلمة المرور غير متطابقتين")
            )
        }

        val strength = AuthSecurityManager.evaluatePasswordStrength(request.newPassword)
        if (!strength.isAcceptable) {
            return ApiResponse(
                status = 400,
                success = false,
                error = ApiError("WEAK_PASSWORD", strength.errorMessageArabic ?: "كلمة المرور الجديدة لا تحقق معايير الأمان")
            )
        }

        val user = if (query.contains("@")) {
            userDao.getUserByEmail(query)
        } else {
            userDao.getUserByUsername(query)
        }

        if (user == null) {
            return ApiResponse(status = 404, success = false, error = ApiError("USER_NOT_FOUND", "تعذر العثور على المستخدم"))
        }

        val newSalt = AuthSecurityManager.generateSalt()
        val newHash = AuthSecurityManager.hashPassword(request.newPassword, newSalt)

        val updated = user.copy(
            passwordHashHex = newHash,
            passwordSaltHex = AuthSecurityManager.bytesToHex(newSalt),
            failedAttempts = 0,
            lockoutUntilMillis = 0L,
            sessionToken = null
        )
        userDao.updateUser(updated)
        pendingResetOtps.remove(query)

        logAudit(user.username, "PASSWORD_RESET_SUCCESS", "تم تغيير كلمة المرور بنجاح بواسطة رمز الاستعادة")

        return ApiResponse(
            status = 200,
            success = true,
            data = "تمت إعادة تعيين كلمة المرور بنجاح. يمكنك الآن تسجيل الدخول بكلمة المرور الجديدة."
        )
    }

    // --- Session Logout ---

    suspend fun logout(userId: String) {
        val user = userDao.getUserById(userId)
        if (user != null) {
            val updated = user.copy(sessionToken = null, rememberMe = false)
            userDao.updateUser(updated)
            logAudit(user.username, "LOGOUT", "تم تسجيل الخروج الآمن للمستخدم")
        }
        _currentUser.value = null
    }

    suspend fun tryRestoreRememberMeSession(): UserEntity? {
        val users = userDao.getAllUsers()
        val remembered = users.firstOrNull { it.rememberMe && !it.sessionToken.isNullOrEmpty() }
        if (remembered != null) {
            _currentUser.value = remembered
            logAudit(remembered.username, "SESSION_RESTORED", "استعادة الجلسة المحفوظة بنجاح (تذكرني)")
            return remembered
        }
        return null
    }
}
