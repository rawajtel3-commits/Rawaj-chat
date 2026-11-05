package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise-grade Security & Authentication Manager
 * Implements PBKDF2WithHmacSHA256 hashing (64,000 iterations, 256-bit salt),
 * real-time password strength scoring, E.164 phone & email validation,
 * secure OTP generation, and CAPTCHA challenge creation.
 */
object AuthSecurityManager {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 64_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    // Common weak passwords to prevent in compliance with NIST SP 800-63B
    private val COMMON_WEAK_PASSWORDS = setOf(
        "123456", "12345678", "123456789", "password", "password123",
        "admin", "admin123", "qwerty", "welcome", "iloveyou",
        "111111", "123123", "pass1234", "master", "dragon",
        "football", "monkey", "sunshine", "princess", "secret"
    )

    // Regex for E.164 International Phone Numbers (WhatsApp standard, e.g. +966501234567)
    private val E164_PHONE_REGEX = Regex("^\\+[1-9]\\d{7,14}\$")

    // RFC 5322 compatible email pattern
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    // Username: 3 to 30 characters, alphanumeric, underscores, and dashes
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9_.-]{2,29}\$")

    // --- Password Hashing & Verification ---

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return bytesToHex(hash)
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        return try {
            val salt = hexToBytes(saltHex)
            val computedHashHex = hashPassword(password, salt)
            // Constant-time comparison to prevent timing attacks
            MessageDigest.isEqual(computedHashHex.toByteArray(Charsets.UTF_8), expectedHashHex.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            false
        }
    }

    // --- Password Strength & Policy ---

    data class PasswordRuleStatus(
        val key: String,
        val labelArabic: String,
        val satisfied: Boolean
    )

    enum class StrengthLevel(val score: Int, val labelArabic: String) {
        VERY_WEAK(0, "ضعيفة جداً"),
        WEAK(1, "ضعيفة"),
        MEDIUM(2, "متوسطة"),
        STRONG(3, "قوية"),
        VERY_STRONG(4, "ممتازة")
    }

    data class PasswordStrengthResult(
        val score: Int, // 0 to 4
        val percentage: Float, // 0f to 1f
        val level: StrengthLevel,
        val rules: List<PasswordRuleStatus>,
        val isAcceptable: Boolean,
        val errorMessageArabic: String? = null
    )

    fun evaluatePasswordStrength(password: String): PasswordStrengthResult {
        val trimmed = password.trim()
        val isCommon = COMMON_WEAK_PASSWORDS.contains(trimmed.lowercase())

        val hasMinLength = trimmed.length >= 8
        val hasMinLengthOptimal = trimmed.length >= 10
        val hasUppercase = trimmed.any { it.isUpperCase() }
        val hasLowercase = trimmed.any { it.isLowerCase() }
        val hasDigit = trimmed.any { it.isDigit() }
        val hasSpecial = trimmed.any { !it.isLetterOrDigit() }

        val rules = listOf(
            PasswordRuleStatus("length", "8 أحرف على الأقل", hasMinLength),
            PasswordRuleStatus("uppercase", "حرف كبير واحد على الأقل (A-Z)", hasUppercase),
            PasswordRuleStatus("lowercase", "حرف صغير واحد على الأقل (a-z)", hasLowercase),
            PasswordRuleStatus("digit", "رقم واحد على الأقل (0-9)", hasDigit),
            PasswordRuleStatus("special", "رمز خاص (!@#\$%^&*)", hasSpecial),
            PasswordRuleStatus("not_common", "ليست كلمة شائعة أو متوقعة", !isCommon)
        )

        if (trimmed.isEmpty()) {
            return PasswordStrengthResult(
                score = 0,
                percentage = 0f,
                level = StrengthLevel.VERY_WEAK,
                rules = rules,
                isAcceptable = false,
                errorMessageArabic = null
            )
        }

        if (isCommon) {
            return PasswordStrengthResult(
                score = 1,
                percentage = 0.2f,
                level = StrengthLevel.VERY_WEAK,
                rules = rules,
                isAcceptable = false,
                errorMessageArabic = "كلمة المرور هذه شائعة جداً وسهلة التخمين، يرجى اختيار كلمة أكثر أماناً."
            )
        }

        var scorePoints = 0
        if (hasMinLength) scorePoints += 1
        if (hasMinLengthOptimal) scorePoints += 1
        if (hasUppercase) scorePoints += 1
        if (hasLowercase) scorePoints += 1
        if (hasDigit) scorePoints += 1
        if (hasSpecial) scorePoints += 1

        val finalScore = when (scorePoints) {
            0, 1 -> 0
            2, 3 -> 1
            4 -> 2
            5 -> 3
            else -> 4
        }

        val level = when (finalScore) {
            0 -> StrengthLevel.VERY_WEAK
            1 -> StrengthLevel.WEAK
            2 -> StrengthLevel.MEDIUM
            3 -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }

        // Acceptable requires min length, at least 1 digit, 1 letter, not common, and score >= 2
        val isAcceptable = hasMinLength && (hasUppercase || hasLowercase) && hasDigit && !isCommon && finalScore >= 2

        val errorMsg = if (!isAcceptable) {
            when {
                !hasMinLength -> "يجب ألا يقل طول كلمة المرور عن 8 أحرف"
                !hasDigit -> "يجب أن تحتوي كلمة المرور على رقم واحد على الأقل"
                !hasUppercase && !hasLowercase -> "يجب أن تحتوي كلمة المرور على أحرف إنجليزية"
                else -> "كلمة المرور ضعيفة، يرجى إضافة رموز أو أحرف كبيرة"
            }
        } else null

        return PasswordStrengthResult(
            score = finalScore,
            percentage = (finalScore + 1) / 5f,
            level = level,
            rules = rules,
            isAcceptable = isAcceptable,
            errorMessageArabic = errorMsg
        )
    }

    // --- Validation Rules ---

    fun validateUsername(username: String): Pair<Boolean, String?> {
        val trimmed = username.trim()
        return when {
            trimmed.isEmpty() -> false to "يرجى إدخال اسم المستخدم"
            trimmed.length < 3 -> false to "اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل"
            trimmed.length > 30 -> false to "اسم المستخدم لا يمكن أن يتجاوز 30 حرفاً"
            !USERNAME_REGEX.matches(trimmed) -> false to "اسم المستخدم يجب أن يحتوي على أحرف إنجليزية وأرقام، ورموز (- . _) فقط"
            else -> true to null
        }
    }

    fun validateEmail(email: String): Pair<Boolean, String?> {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> false to "يرجى إدخال البريد الإلكتروني"
            !EMAIL_REGEX.matches(trimmed) -> false to "صيغة البريد الإلكتروني غير صحيحة (مثال: user@example.com)"
            else -> true to null
        }
    }

    fun validateWhatsAppNumber(phone: String): Pair<Boolean, String?> {
        val trimmed = phone.trim().replace(" ", "").replace("-", "")
        return when {
            trimmed.isEmpty() -> false to "يرجى إدخال رقم الواتساب بالصيغة الدولية"
            !trimmed.startsWith("+") -> false to "يجب أن يبدأ الرقم بمفتاح الدولة مع رمز + (مثال: +966501234567)"
            !E164_PHONE_REGEX.matches(trimmed) -> false to "صيغة رقم الهاتف الدولية غير صحيحة (E.164)"
            else -> true to null
        }
    }

    // --- OTP & Security Code Generation ---

    fun generateOtp(length: Int = 6): String {
        val builder = java.lang.StringBuilder()
        repeat(length) {
            builder.append(secureRandom.nextInt(10))
        }
        return builder.toString()
    }

    fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    // --- Math / Visual CAPTCHA Generator ---

    data class CaptchaChallenge(
        val questionArabic: String,
        val expectedAnswer: String,
        val token: String
    )

    fun generateCaptcha(): CaptchaChallenge {
        val a = secureRandom.nextInt(15) + 1
        val b = secureRandom.nextInt(15) + 1
        val isAddition = secureRandom.nextBoolean()

        val question = if (isAddition) {
            "$a + $b = ؟"
        } else {
            val max = maxOf(a, b)
            val min = minOf(a, b)
            "$max - $min = ؟"
        }

        val answer = if (isAddition) (a + b).toString() else (maxOf(a, b) - minOf(a, b)).toString()
        val token = generateSecureToken()

        return CaptchaChallenge(
            questionArabic = question,
            expectedAnswer = answer,
            token = token
        )
    }

    // --- Utilities ---

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef".toCharArray()
        val result = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            result[i * 2] = hexChars[v ushr 4]
            result[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(result)
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
