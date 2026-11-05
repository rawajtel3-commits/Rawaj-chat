package com.example

import com.example.data.auth.UserEntity
import com.example.util.AuthSecurityManager
import org.junit.Assert.*
import org.junit.Test

class AuthSecurityUnitTest {

    @Test
    fun testSaltGenerationAndHexConversion() {
        val salt1 = AuthSecurityManager.generateSalt()
        val salt2 = AuthSecurityManager.generateSalt()

        assertEquals(32, salt1.size)
        assertEquals(32, salt2.size)
        assertFalse(salt1.contentEquals(salt2))

        val hex = AuthSecurityManager.bytesToHex(salt1)
        val decoded = AuthSecurityManager.hexToBytes(hex)
        assertArrayEquals(salt1, decoded)
    }

    @Test
    fun testPasswordHashingAndVerification() {
        val password = "MySecurePassword#2026"
        val salt = AuthSecurityManager.generateSalt()
        val saltHex = AuthSecurityManager.bytesToHex(salt)

        val hashHex = AuthSecurityManager.hashPassword(password, salt)
        assertTrue(hashHex.isNotEmpty())
        assertEquals(64, hashHex.length) // 256 bits = 32 bytes = 64 hex chars

        // Correct password should pass
        assertTrue(AuthSecurityManager.verifyPassword(password, saltHex, hashHex))

        // Incorrect password should fail
        assertFalse(AuthSecurityManager.verifyPassword("WrongPassword123", saltHex, hashHex))

        // Same password with different salt produces different hash
        val salt2 = AuthSecurityManager.generateSalt()
        val hashHex2 = AuthSecurityManager.hashPassword(password, salt2)
        assertNotEquals(hashHex, hashHex2)
    }

    @Test
    fun testPasswordStrengthMeterPolicy() {
        // Empty
        val emptyResult = AuthSecurityManager.evaluatePasswordStrength("")
        assertFalse(emptyResult.isAcceptable)
        assertEquals(0, emptyResult.score)

        // Common weak passwords
        val commonResult = AuthSecurityManager.evaluatePasswordStrength("password")
        assertFalse(commonResult.isAcceptable)
        assertTrue(commonResult.errorMessageArabic!!.contains("شائعة"))

        val common123 = AuthSecurityManager.evaluatePasswordStrength("123456")
        assertFalse(common123.isAcceptable)

        // Short password
        val shortResult = AuthSecurityManager.evaluatePasswordStrength("Ab1!")
        assertFalse(shortResult.isAcceptable)

        // Missing digits
        val noDigits = AuthSecurityManager.evaluatePasswordStrength("ValidPasswordWithNoNum!")
        assertFalse(noDigits.isAcceptable)

        // Strong compliant password
        val strongResult = AuthSecurityManager.evaluatePasswordStrength("Strong@P4ssw0rd!2026")
        assertTrue(strongResult.isAcceptable)
        assertTrue(strongResult.score >= 3)
        assertNull(strongResult.errorMessageArabic)
    }

    @Test
    fun testUsernameValidation() {
        val (valid1, _) = AuthSecurityManager.validateUsername("ahmed_2026")
        assertTrue(valid1)

        val (valid2, _) = AuthSecurityManager.validateUsername("admin.user")
        assertTrue(valid2)

        // Too short
        val (invalidShort, errShort) = AuthSecurityManager.validateUsername("ab")
        assertFalse(invalidShort)
        assertNotNull(errShort)

        // Invalid characters (spaces)
        val (invalidSpace, _) = AuthSecurityManager.validateUsername("ahmed 2026")
        assertFalse(invalidSpace)
    }

    @Test
    fun testEmailValidation() {
        val (valid1, _) = AuthSecurityManager.validateEmail("user@example.com")
        assertTrue(valid1)

        val (valid2, _) = AuthSecurityManager.validateEmail("name.surname+tag@domain.co.uk")
        assertTrue(valid2)

        val (invalid1, _) = AuthSecurityManager.validateEmail("not-an-email")
        assertFalse(invalid1)

        val (invalid2, _) = AuthSecurityManager.validateEmail("user@")
        assertFalse(invalid2)
    }

    @Test
    fun testWhatsAppE164Validation() {
        // Valid E.164 with international country code
        val (saudiValid, _) = AuthSecurityManager.validateWhatsAppNumber("+966501234567")
        assertTrue(saudiValid)

        val (uaeValid, _) = AuthSecurityManager.validateWhatsAppNumber("+971501234567")
        assertTrue(uaeValid)

        val (egyptValid, _) = AuthSecurityManager.validateWhatsAppNumber("+201012345678")
        assertTrue(egyptValid)

        // Missing '+' prefix
        val (missingPlus, errPlus) = AuthSecurityManager.validateWhatsAppNumber("0501234567")
        assertFalse(missingPlus)
        assertNotNull(errPlus)

        // Too short
        val (tooShort, _) = AuthSecurityManager.validateWhatsAppNumber("+9661")
        assertFalse(tooShort)
    }

    @Test
    fun testOtpGeneration() {
        val otp1 = AuthSecurityManager.generateOtp(6)
        val otp2 = AuthSecurityManager.generateOtp(6)

        assertEquals(6, otp1.length)
        assertEquals(6, otp2.length)
        assertTrue(otp1.all { it.isDigit() })
        assertTrue(otp2.all { it.isDigit() })
    }

    @Test
    fun testCaptchaChallengeGeneration() {
        val challenge = AuthSecurityManager.generateCaptcha()
        assertNotNull(challenge.questionArabic)
        assertTrue(challenge.questionArabic.contains("؟"))
        assertNotNull(challenge.expectedAnswer)
        assertTrue(challenge.expectedAnswer.toIntOrNull() != null)
        assertTrue(challenge.token.isNotEmpty())
    }

    @Test
    fun testUserLockoutBehavior() {
        val activeUser = UserEntity(
            id = "user1",
            username = "testuser",
            email = "test@example.com",
            whatsappNumber = "+966501234567",
            passwordHashHex = "dummy",
            passwordSaltHex = "dummy",
            failedAttempts = 2,
            lockoutUntilMillis = 0L
        )
        assertFalse(activeUser.isLockedOut())
        assertEquals(0, activeUser.remainingLockoutSeconds())

        val lockedUser = activeUser.copy(
            failedAttempts = 5,
            lockoutUntilMillis = System.currentTimeMillis() + 25_000L
        )
        assertTrue(lockedUser.isLockedOut())
        assertTrue(lockedUser.remainingLockoutSeconds() > 0)
    }
}
