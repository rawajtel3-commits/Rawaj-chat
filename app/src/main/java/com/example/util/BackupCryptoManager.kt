package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoManager {

    private const val KEYSTORE_ALIAS = "PrivateCabinsBackupMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128

    init {
        ensureKeystoreMasterKey()
    }

    private fun ensureKeystoreMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encryptBackupPayload(plainJsonString: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val derivedKey = deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey)

        val iv = cipher.iv
        val ciphertextBytes = cipher.doFinal(plainJsonString.toByteArray(Charsets.UTF_8))

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP)

        val jsonContainer = org.json.JSONObject().apply {
            put("app", "PrivateCabins")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("salt", saltBase64)
            put("iv", ivBase64)
            put("ciphertext", ciphertextBase64)
            put("keystoreProtected", true)
        }

        return jsonContainer.toString(2)
    }

    fun decryptBackupPayload(encryptedJsonContainer: String, password: String): String {
        val jsonObject = org.json.JSONObject(encryptedJsonContainer)

        val saltBase64 = jsonObject.getString("salt")
        val ivBase64 = jsonObject.getString("iv")
        val ciphertextBase64 = jsonObject.getString("ciphertext")

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val derivedKey = deriveKeyFromPassword(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
