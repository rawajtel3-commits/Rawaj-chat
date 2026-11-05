package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    org.junit.Assert.assertTrue(appName == "Chat Rawaj" || appName == "شات رواج")
  }

  @Test
  fun `notification channels are created properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.NotificationHelper.createNotificationChannels(context)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    val msgChannel = notificationManager.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_MESSAGES_ID)
    val taskChannel = notificationManager.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_TASKS_ID)

    org.junit.Assert.assertNotNull("Messages notification channel should exist", msgChannel)
    org.junit.Assert.assertNotNull("Tasks notification channel should exist", taskChannel)
    assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, msgChannel.importance)
    assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, taskChannel.importance)
  }

  @Test
  fun `fcm token manager saves and retrieves cached token`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testToken = "test_fcm_token_12345"
    com.example.notifications.FcmTokenManager.saveToken(context, testToken)

    val retrieved = com.example.notifications.FcmTokenManager.getCachedToken(context)
    assertEquals(testToken, retrieved)
  }

  @Test
  fun `message reactions format and parse correctly`() {
    val initialMap = mapOf(
        "👍" to listOf("أنت", "Alice"),
        "🔥" to listOf("Bob")
    )
    val json = com.example.data.CabinMessage.formatReactionsJson(initialMap)
    val msg = com.example.data.CabinMessage(
        id = "test_msg_1",
        cabinId = "cabin_1",
        senderName = "Alice",
        isMe = false,
        text = "Hello E2EE",
        cipherText = "Cipher123",
        timestamp = "12:00 PM",
        reactionsJson = json
    )

    val parsed = msg.getReactionsMap()
    assertEquals(2, parsed.size)
    org.junit.Assert.assertTrue(parsed["👍"]?.contains("أنت") == true)
    org.junit.Assert.assertTrue(parsed["👍"]?.contains("Alice") == true)
    org.junit.Assert.assertTrue(parsed["🔥"]?.contains("Bob") == true)
  }

  @Test
  fun `toggle reaction adds and removes user from emoji reaction`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.AppDatabase.getDatabase(context)
    val repo = com.example.data.CabinsRepository(
        cabinDao = db.cabinDao(),
        messageDao = db.messageDao(),
        taskDao = db.taskDao(),
        outboxDao = db.outboxDao(),
        appContext = context
    )

    val testMsg = repo.sendMessage(
        cabinId = "cabin_work_1",
        text = "رسالة تجريبية للتفاعل المشفر",
        senderName = "أنت",
        isMe = true
    )

    // 1. Toggle reaction "👍" (should add "أنت")
    val updated1 = repo.toggleMessageReaction("cabin_work_1", testMsg.id, "👍", "أنت")
    org.junit.Assert.assertNotNull(updated1)
    org.junit.Assert.assertTrue(updated1?.getReactionsMap()?.get("👍")?.contains("أنت") == true)

    // 2. Toggle reaction "👍" again (should remove "أنت")
    val updated2 = repo.toggleMessageReaction("cabin_work_1", testMsg.id, "👍", "أنت")
    org.junit.Assert.assertNotNull(updated2)
    org.junit.Assert.assertTrue(updated2?.getReactionsMap()?.containsKey("👍") == false)
  }

  @Test
  fun `encrypted storage manager encrypts and decrypts attachment roundtrip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cabinId = "cabin_crypto_test"
    val originalText = "Top-Secret Cabin Architectural Specification (Confidential E2EE)"
    val originalBytes = originalText.toByteArray(Charsets.UTF_8)

    val result = com.example.util.EncryptedStorageManager.encryptBytesAndStore(
        context = context,
        cabinId = cabinId,
        plainBytes = originalBytes,
        fileName = "confidential_spec.pdf",
        mimeType = "application/pdf",
        isImage = false
    )

    // Verify metadata
    assertEquals("confidential_spec.pdf", result.fileName)
    assertEquals("application/pdf", result.mimeType)
    org.junit.Assert.assertFalse(result.isImage)
    org.junit.Assert.assertTrue(result.encryptedFile.exists())
    org.junit.Assert.assertTrue(result.encryptedFile.length() > 0)

    // Verify ciphertext on disk is completely scrambled
    val cipherBytesOnDisk = result.encryptedFile.readBytes()
    org.junit.Assert.assertFalse(cipherBytesOnDisk.contentEquals(originalBytes))

    // Verify SHA-256 calculation
    val expectedSha256 = com.example.util.EncryptedStorageManager.calculateSha256(originalBytes)
    assertEquals(expectedSha256, result.sha256Hex)

    // Decrypt and assert roundtrip equality
    val decryptedBytes = com.example.util.EncryptedStorageManager.decryptAttachment(
        encryptedFile = result.encryptedFile,
        ivHex = result.ivHex,
        cabinId = cabinId
    )
    org.junit.Assert.assertNotNull("Decrypted bytes must not be null", decryptedBytes)
    org.junit.Assert.assertArrayEquals(originalBytes, decryptedBytes)
    assertEquals(originalText, String(decryptedBytes!!, Charsets.UTF_8))
  }

  @Test
  fun `seeded messages contain encrypted document and image attachments`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.AppDatabase.getDatabase(context)
    val repo = com.example.data.CabinsRepository(
        cabinDao = db.cabinDao(),
        messageDao = db.messageDao(),
        taskDao = db.taskDao(),
        outboxDao = db.outboxDao(),
        appContext = context
    )

    repo.seedInitialDataIfEmpty()
    val allMessages = db.messageDao().getAllMessagesList()
    val docMsg = allMessages.find { it.attachmentType == "DOCUMENT" }
    val imgMsg = allMessages.find { it.attachmentType == "IMAGE" }

    org.junit.Assert.assertNotNull("Seeded document message should exist", docMsg)
    org.junit.Assert.assertNotNull("Seeded image message should exist", imgMsg)
    org.junit.Assert.assertTrue(docMsg?.isAttachment == true)
    org.junit.Assert.assertTrue(imgMsg?.isAttachment == true)
    assertEquals("E2EE_Security_Architecture_v2.pdf", docMsg?.attachmentName)
    assertEquals("Cabin_Topology_Schematic.png", imgMsg?.attachmentName)
    assertEquals("SYNCED_CLOUD", docMsg?.uploadStatus)
    assertEquals("SYNCED_CLOUD", imgMsg?.uploadStatus)
  }
}

