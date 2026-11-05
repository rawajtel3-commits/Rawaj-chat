package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedAttachmentResult(
    val attachmentId: String,
    val fileName: String,
    val mimeType: String,
    val isImage: Boolean,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val encryptedFile: File,
    val encryptedFilePath: String,
    val ivHex: String,
    val sha256Hex: String,
    val cloudStorageRef: String
)

object EncryptedStorageManager {

    private const val TAG = "EncryptedStorageManager"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_PREFIX = "PrivateCabinAttachmentSalt_2026_"

    private fun deriveCabinKey(cabinId: String): SecretKey {
        val masterPassword = "CabinSecretKey_${cabinId}_E2EE_Vault"
        val salt = (SALT_PREFIX + cabinId).toByteArray(Charsets.UTF_8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(masterPassword.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02X", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return bytesToHex(digest)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun getVaultDirectory(context: Context, cabinId: String): File {
        val dir = File(context.filesDir, "cabin_vault/$cabinId/attachments")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun resolveFileMeta(context: Context, uri: Uri, fallbackIsImage: Boolean): Pair<String, String> {
        var name = if (fallbackIsImage) "image_${System.currentTimeMillis()}.jpg" else "document_${System.currentTimeMillis()}.pdf"
        var mime = if (fallbackIsImage) "image/jpeg" else "application/pdf"

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val resolvedName = it.getString(nameIndex)
                        if (!resolvedName.isNullOrBlank()) {
                            name = resolvedName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving filename from uri: ${e.localizedMessage}")
        }

        try {
            val type = context.contentResolver.getType(uri)
            if (!type.isNullOrBlank()) {
                mime = type
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving mime type: ${e.localizedMessage}")
        }

        return Pair(name, mime)
    }

    fun encryptBytesAndStore(
        context: Context,
        cabinId: String,
        plainBytes: ByteArray,
        fileName: String,
        mimeType: String,
        isImage: Boolean
    ): EncryptedAttachmentResult {
        val attachmentId = "att_${System.currentTimeMillis()}_" + UUID.randomUUID().toString().take(6)
        val vaultDir = getVaultDirectory(context, cabinId)
        val encryptedFile = File(vaultDir, "$attachmentId.enc")

        // 1. Generate 12-byte cryptographically secure random IV
        val random = SecureRandom()
        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)
        val ivHex = bytesToHex(iv)

        // 2. Compute SHA-256 checksum of original bytes
        val sha256Hex = calculateSha256(plainBytes)

        // 3. Encrypt with AES-256-GCM
        val key = deriveCabinKey(cabinId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherBytes = cipher.doFinal(plainBytes)

        // 4. Write encrypted ciphertext to private internal storage
        FileOutputStream(encryptedFile).use { it.write(cipherBytes) }

        val fileSize = plainBytes.size.toLong()
        val cloudRef = "gs://private-cabins.appspot.com/cabins/$cabinId/attachments/$attachmentId.enc"

        return EncryptedAttachmentResult(
            attachmentId = attachmentId,
            fileName = fileName,
            mimeType = mimeType,
            isImage = isImage,
            fileSizeBytes = fileSize,
            formattedSize = formatFileSize(fileSize),
            encryptedFile = encryptedFile,
            encryptedFilePath = encryptedFile.absolutePath,
            ivHex = ivHex,
            sha256Hex = sha256Hex,
            cloudStorageRef = cloudRef
        )
    }

    fun encryptFromUri(
        context: Context,
        cabinId: String,
        uri: Uri,
        fallbackIsImage: Boolean = false
    ): EncryptedAttachmentResult {
        val (fileName, mimeType) = resolveFileMeta(context, uri, fallbackIsImage)
        val isImage = fallbackIsImage || mimeType.startsWith("image/")

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

        val bytes = inputStream.use { it.readBytes() }
        return encryptBytesAndStore(
            context = context,
            cabinId = cabinId,
            plainBytes = bytes,
            fileName = fileName,
            mimeType = mimeType,
            isImage = isImage
        )
    }

    fun decryptAttachment(
        encryptedFile: File,
        ivHex: String,
        cabinId: String
    ): ByteArray? {
        if (!encryptedFile.exists() || !encryptedFile.canRead()) {
            Log.w(TAG, "Encrypted file does not exist: ${encryptedFile.absolutePath}")
            return null
        }
        return try {
            val cipherBytes = encryptedFile.readBytes()
            val iv = hexToBytes(ivHex)
            val key = deriveCabinKey(cabinId)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.localizedMessage}", e)
            null
        }
    }

    fun decryptToBitmap(
        encryptedFile: File,
        ivHex: String,
        cabinId: String
    ): Bitmap? {
        val bytes = decryptAttachment(encryptedFile, ivHex, cabinId) ?: return null
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed decoding bitmap: ${e.localizedMessage}")
            null
        }
    }
}
