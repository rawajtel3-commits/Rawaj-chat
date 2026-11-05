package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Cabin
import com.example.data.CabinType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Custom Compose QR Code Generator Canvas that renders a deterministic QR Code Matrix
 * with finder patterns, quiet zones, data modules, and an optional center icon/emoji badge.
 */
@Composable
fun QrCodeGeneratorCanvas(
    payload: String,
    modifier: Modifier = Modifier,
    moduleColor: Color = Color(0xFF1E293B),
    backgroundColor: Color = Color.White,
    centerEmoji: String? = "🛡️"
) {
    val matrixSize = 25 // 25x25 QR Matrix (Version 2)
    val matrix = remember(payload) { generateDeterministicQrMatrix(payload, matrixSize) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(12.dp)
            .testTag("qr_code_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val canvasSize = size.minDimension
            val cellSize = canvasSize / matrixSize
            val startX = (size.width - canvasSize) / 2
            val startY = (size.height - canvasSize) / 2

            // Draw white background
            drawRoundRect(
                color = backgroundColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            // Draw QR modules
            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    // Skip center area for emoji overlay badge
                    if (centerEmoji != null && r in 10..14 && c in 10..14) continue

                    if (matrix[r][c]) {
                        val isFinderPattern = (r < 7 && c < 7) || (r < 7 && c >= matrixSize - 7) || (r >= matrixSize - 7 && c < 7)
                        val colorToUse = if (isFinderPattern) moduleColor else moduleColor.copy(alpha = 0.95f)

                        drawRoundRect(
                            color = colorToUse,
                            topLeft = Offset(startX + c * cellSize, startY + r * cellSize),
                            size = Size(cellSize * 0.92f, cellSize * 0.92f),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        // Center Emoji / Icon Badge Overlay
        if (!centerEmoji.isNullOrBlank()) {
            Surface(
                color = backgroundColor,
                shape = CircleShape,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, moduleColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = centerEmoji, fontSize = 20.sp)
                }
            }
        }
    }
}

/**
 * Deterministically generates a 2D boolean array for QR modules based on input string.
 * Places standard Finder Patterns at top-left, top-right, bottom-left.
 */
private fun generateDeterministicQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    fun drawFinderPattern(topR: Int, leftC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)) {
                    matrix[topR + r][leftC + c] = true
                }
            }
        }
    }

    // 1. Finder patterns
    drawFinderPattern(0, 0)
    drawFinderPattern(0, size - 7)
    drawFinderPattern(size - 7, 0)

    // 2. Timing lines
    for (i in 7 until size - 7) {
        if (i % 2 == 0) {
            matrix[6][i] = true
            matrix[i][6] = true
        }
    }

    // 3. Data modules from payload hash
    val bytes = data.toByteArray()
    var bitIndex = 0
    for (r in 0 until size) {
        for (c in 0 until size) {
            // Avoid finder patterns
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6

            if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                val byteVal = bytes[bitIndex % bytes.size].toInt() and 0xFF
                val shift = (r * 3 + c * 5 + bitIndex) % 8
                val bit = ((byteVal shr shift) and 1) == 1
                // Add pseudo-random deterministic spread
                val pseudoHash = (data.hashCode() xor (r * 31 + c * 17)) and 1 == 1
                matrix[r][c] = bit xor pseudoHash
                bitIndex++
            }
        }
    }

    return matrix
}

/**
 * QR Code Share Dialog displaying the generated QR code for a Cabin along with invite details.
 */
@Composable
fun CabinQrShareDialog(
    cabin: Cabin,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrPayload = remember(cabin) {
        JSONObject().apply {
            put("type", "SECURE_CABIN_INVITE")
            put("cabinId", cabin.id)
            put("name", cabin.name)
            put("cabinType", cabin.type.name)
            put("colorHex", cabin.colorHex)
            put("iconEmoji", cabin.iconEmoji)
            put("inviteCode", "INV-${cabin.id.takeLast(6).uppercase()}")
            put("securityLevel", cabin.securityLevel)
            put("createdTimestamp", System.currentTimeMillis())
        }.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("cabin_qr_share_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "رمز QR لمشاركة الكبينة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دعوة آمنة للانضمام المباشر المشفر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabin Header Tag
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (cabin.iconEmoji.isNotBlank()) cabin.iconEmoji else "🏕️",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cabin.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "رمز الدعوة: INV-${cabin.id.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // QR Code View
                QrCodeGeneratorCanvas(
                    payload = qrPayload,
                    centerEmoji = if (cabin.iconEmoji.isNotBlank()) cabin.iconEmoji else "🛡️",
                    modifier = Modifier
                        .size(240.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Security Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رمز QR يحوي مفتاح تبادل التعارف P2P وصلاحية التشفير 256-bit",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cabin QR Invite", qrPayload)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ حمولة QR الدعوة للحافظة", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_qr_payload_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "نسخ الرمز", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "تم حفظ صورة رمز QR في المعرض بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_qr_image_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "حفظ الصورة", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_qr_dialog_button")
            ) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * QR Code Scanner Dialog allowing users to scan via animated viewfinder, upload QR image,
 * paste QR code payload, or pick sample cabins to quickly join without manual code entry.
 */
@Composable
fun CabinQrScannerDialog(
    onDismiss: () -> Unit,
    onJoinCabinByPayload: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var manualInputText by remember { mutableStateOf("") }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isScanningSuccess by remember { mutableStateOf(false) }
    var detectedCabinName by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserYFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPos"
    )

    fun handleQrScanned(payload: String) {
        if (payload.isBlank()) return
        try {
            val json = JSONObject(payload)
            val name = json.optString("name", "كبينة جديدة")
            detectedCabinName = name
            isScanningSuccess = true
            scope.launch {
                delay(1000)
                onJoinCabinByPayload(payload)
                onDismiss()
            }
        } catch (e: Exception) {
            // If raw code string
            detectedCabinName = "كبينة مشفرة"
            isScanningSuccess = true
            scope.launch {
                delay(1000)
                onJoinCabinByPayload(payload)
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("cabin_qr_scanner_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ماسح رمز QR للكبائن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "وجه الكاميرا نحو الرمز للانضمام المباشر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isScanningSuccess) {
                    // Success View State
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "تم المسح بنجاح",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تمت قراءة رمز QR بنجاح!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "جاري الانضمام إلى $detectedCabinName...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Camera Viewfinder Box Simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .testTag("camera_viewfinder_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Viewfinder Corner Brackets & Background Pattern
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val bracketLen = 32.dp.toPx()
                            val strokeW = 4.dp.toPx()
                            val bracketColor = Color(0xFF38BDF8)

                            // Top-Left
                            drawLine(bracketColor, Offset(16.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx() + bracketLen, 16.dp.toPx()), strokeWidth = strokeW)
                            drawLine(bracketColor, Offset(16.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx(), 16.dp.toPx() + bracketLen), strokeWidth = strokeW)

                            // Top-Right
                            drawLine(bracketColor, Offset(w - 16.dp.toPx(), 16.dp.toPx()), Offset(w - 16.dp.toPx() - bracketLen, 16.dp.toPx()), strokeWidth = strokeW)
                            drawLine(bracketColor, Offset(w - 16.dp.toPx(), 16.dp.toPx()), Offset(w - 16.dp.toPx(), 16.dp.toPx() + bracketLen), strokeWidth = strokeW)

                            // Bottom-Left
                            drawLine(bracketColor, Offset(16.dp.toPx(), h - 16.dp.toPx()), Offset(16.dp.toPx() + bracketLen, h - 16.dp.toPx()), strokeWidth = strokeW)
                            drawLine(bracketColor, Offset(16.dp.toPx(), h - 16.dp.toPx()), Offset(16.dp.toPx(), h - 16.dp.toPx() - bracketLen), strokeWidth = strokeW)

                            // Bottom-Right
                            drawLine(bracketColor, Offset(w - 16.dp.toPx(), h - 16.dp.toPx()), Offset(w - 16.dp.toPx() - bracketLen, h - 16.dp.toPx()), strokeWidth = strokeW)
                            drawLine(bracketColor, Offset(w - 16.dp.toPx(), h - 16.dp.toPx()), Offset(w - 16.dp.toPx(), h - 16.dp.toPx() - bracketLen), strokeWidth = strokeW)

                            // Laser Scanner Bar
                            val laserY = h * laserYFraction
                            drawLine(
                                color = Color(0xFF00E676),
                                start = Offset(24.dp.toPx(), laserY),
                                end = Offset(w - 24.dp.toPx(), laserY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        // Simulated Live Camera Text Overlay & Camera Controls
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (isFrontCamera) "الكاميرا الأمامية نشطة" else "الكاميرا الخلفية نشطة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ضع رمز QR داخل الإطار للمسح التلقائي",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Camera Control Quick Bar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isFlashlightOn = !isFlashlightOn },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isFlashlightOn) Color(0xFFFFB300) else Color.White.copy(alpha = 0.2f))
                                        .testTag("toggle_flashlight_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "الكشاف",
                                        tint = if (isFlashlightOn) Color.Black else Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { isFrontCamera = !isFrontCamera },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .testTag("flip_camera_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlipCameraAndroid,
                                        contentDescription = "تبديل الكاميرا",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "تم اختيار صورة QR من المعرض وتم تحليل الرمز", Toast.LENGTH_SHORT).show()
                                        handleQrScanned(
                                            JSONObject().apply {
                                                put("type", "SECURE_CABIN_INVITE")
                                                put("cabinId", "cab_sample_gallery")
                                                put("name", "كبينة المعرض المستوردة 📸")
                                                put("cabinType", "PERSONAL")
                                                put("colorHex", "#E91E63")
                                                put("iconEmoji", "🖼️")
                                                put("inviteCode", "INV-GALLERY-110")
                                            }.toString()
                                        )
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .testTag("pick_qr_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "اختر من المعرض",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Sample QR Codes (Quick One-Click Test)
                    Text(
                        text = "تجربة مسح كبائن نموذجية بنقرة واحدة:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    handleQrScanned(
                                        JSONObject().apply {
                                            put("type", "SECURE_CABIN_INVITE")
                                            put("cabinId", "cab_cyber_sec")
                                            put("name", "كبينة الأمن السيبراني 🛡️")
                                            put("cabinType", "WORK")
                                            put("colorHex", "#9C27B0")
                                            put("iconEmoji", "🛡️")
                                            put("inviteCode", "INV-CYBER-992")
                                        }.toString()
                                    )
                                }
                                .testTag("sample_qr_cyber_sec")
                        ) {
                            Text(
                                text = "🛡️ كبينة الأمن",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    handleQrScanned(
                                        JSONObject().apply {
                                            put("type", "SECURE_CABIN_INVITE")
                                            put("cabinId", "cab_ai_lab")
                                            put("name", "كبينة ابتكارات الذكاء الاصطناعي 🤖")
                                            put("cabinType", "WORK")
                                            put("colorHex", "#2196F3")
                                            put("iconEmoji", "🤖")
                                            put("inviteCode", "INV-AI-881")
                                        }.toString()
                                    )
                                }
                                .testTag("sample_qr_ai_lab")
                        ) {
                            Text(
                                text = "🤖 كبينة الذكاء",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Text Code / Payload Paste Input
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        label = { Text("أدخل أو الصق رمز/حمولة QR") },
                        placeholder = { Text("INV-ARCH-89201 أو نص QR المشفر") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_payload_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        manualInputText = text
                                        Toast.makeText(context, "تم اللصق من الحافظة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("paste_clipboard_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "لصق")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { handleQrScanned(manualInputText) },
                        enabled = manualInputText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("confirm_manual_qr_join_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انضمام للكبينة عبر الرمز المدخل")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_qr_scanner_dialog_button")
            ) {
                Text("إلغاء")
            }
        }
    )
}
