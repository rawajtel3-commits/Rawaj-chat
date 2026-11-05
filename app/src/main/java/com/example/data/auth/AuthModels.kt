package com.example.data.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val whatsappNumber: String,
    val passwordHashHex: String,
    val passwordSaltHex: String,
    val isEmailVerified: Boolean = false,
    val isWhatsappVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val sessionToken: String? = null,
    val rememberMe: Boolean = false
) {
    fun isLockedOut(): Boolean {
        return lockoutUntilMillis > System.currentTimeMillis()
    }

    fun remainingLockoutSeconds(): Int {
        val remaining = (lockoutUntilMillis - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining.toInt() else 0
    }
}

@Entity(tableName = "auth_audit_logs")
data class AuthAuditLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val username: String,
    val eventType: String, // "LOGIN_SUCCESS", "LOGIN_FAILED", "REGISTER", "PASSWORD_RESET", "LOGOUT", "LOCKED_OUT", "CAPTCHA_TRIGGERED"
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String = "127.0.0.1",
    val deviceInfo: String = "Android Device (Client)",
    val statusMessage: String
)

// --- API DTOs and RESTful contracts ---

data class LoginRequest(
    val usernameOrEmail: String,
    val password: String,
    val rememberMe: Boolean = false,
    val captchaResponse: String? = null
)

data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val csrfToken: String,
    val expiresInSeconds: Long,
    val user: UserDto
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val confirmEmail: String,
    val whatsappNumber: String,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponse(
    val userId: String,
    val username: String,
    val email: String,
    val whatsappNumber: String,
    val emailVerificationRequired: Boolean = true,
    val whatsappOtpSent: Boolean = true,
    val messageArabic: String
)

data class VerifyOtpRequest(
    val userId: String,
    val channel: OtpChannel, // WHATSAPP or EMAIL
    val otpCode: String
)

data class VerifyOtpResponse(
    val verified: Boolean,
    val channel: OtpChannel,
    val messageArabic: String,
    val authToken: String? = null
)

data class ForgotPasswordRequest(
    val usernameOrEmail: String
)

data class ResetPasswordConfirmRequest(
    val usernameOrEmail: String,
    val otpCode: String,
    val newPassword: String,
    val confirmNewPassword: String
)

enum class OtpChannel {
    WHATSAPP,
    EMAIL
}

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val whatsappNumber: String,
    val isEmailVerified: Boolean,
    val isWhatsappVerified: Boolean
)

data class ApiResponse<T>(
    val status: Int,
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApiError(
    val code: String,
    val messageArabic: String,
    val fieldErrors: Map<String, String>? = null
)
