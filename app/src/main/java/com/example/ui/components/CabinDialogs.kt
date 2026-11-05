package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Cabin
import com.example.data.CabinTask
import com.example.data.CabinType
import com.example.data.TaskPriority
import com.example.data.TaskStatus

val PRESET_COLORS = listOf(
    "#4CAF50",
    "#2196F3",
    "#9C27B0",
    "#FF9800",
    "#E91E63",
    "#00BCD4",
    "#FF5252",
    "#607D8B"
)

val PRESET_EMOJIS = listOf("🏕️", "🚀", "🔥", "🛡️", "💼", "💎", "⭐", "🌿", "🏰", "🔒", "🎯", "⚡")

@Composable
fun CreateCabinDialog(
    onDismiss: () -> Unit,
    onCreateCabin: (String, String, CabinType, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var participantTags by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CabinType.WORK) }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var selectedEmoji by remember { mutableStateOf("🏕️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إنشاء كبينة خاصة جديدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الكبينة") },
                    modifier = Modifier.fillMaxWidth().testTag("cabin_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الكبينة أو الهدف منها") },
                    modifier = Modifier.fillMaxWidth().testTag("cabin_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = participantTags,
                    onValueChange = { participantTags = it },
                    label = { Text("وسوم المشاركين والرموز (مثال: @أحمد, @سارة, #مشروع)") },
                    placeholder = { Text("@أحمد, @سارة, #تطوير") },
                    modifier = Modifier.fillMaxWidth().testTag("cabin_tags_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Text(
                    text = "نوع الكبينة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TypeSelectChip("عمل", selectedType == CabinType.WORK) { selectedType = CabinType.WORK }
                    TypeSelectChip("عائلة", selectedType == CabinType.FAMILY) { selectedType = CabinType.FAMILY }
                    TypeSelectChip("سرية", selectedType == CabinType.SECRET_VAULT) { selectedType = CabinType.SECRET_VAULT }
                    TypeSelectChip("عامة", selectedType == CabinType.PUBLIC) { selectedType = CabinType.PUBLIC }
                }

                Text(
                    text = "رمز وشعار الكبينة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                EmojiSelectorRow(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )

                Text(
                    text = "لون المميز بالقائمة الجانبية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ColorSelectorRow(
                    selectedColorHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateCabin(name, description, selectedType, selectedColor, selectedEmoji, participantTags)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("submit_create_cabin_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إنشاء الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun CustomizeCabinAppearanceDialog(
    cabin: Cabin,
    onDismiss: () -> Unit,
    onSaveAppearance: (String, String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(if (cabin.colorHex.isNotBlank()) cabin.colorHex else "#4CAF50") }
    var selectedEmoji by remember { mutableStateOf(if (cabin.iconEmoji.isNotBlank()) cabin.iconEmoji else "🏕️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تخصيص مظهر الكبينة (${cabin.name})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "اختر رمز الشعار واللون المميز لهذه الكبينة للظهور في القائمة الجانبية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "الرمز التعبيري (Emoji):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                EmojiSelectorRow(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )

                Text(
                    text = "اللون المميز:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ColorSelectorRow(
                    selectedColorHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveAppearance(selectedColor, selectedEmoji)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ التغييرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun EmojiSelectorRow(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(PRESET_EMOJIS) { emoji ->
            val isSelected = selectedEmoji == emoji
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        BorderStroke(
                            if (isSelected) 2.dp else 0.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        CircleShape
                    )
                    .clickable { onEmojiSelected(emoji) }
                    .testTag("emoji_select_$emoji"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ColorSelectorRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(PRESET_COLORS) { hex ->
            val color = parseHexColor(hex)
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        BorderStroke(
                            if (isSelected) 3.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                        ),
                        CircleShape
                    )
                    .clickable { onColorSelected(hex) }
                    .testTag("color_select_$hex")
            )
        }
    }
}

fun parseHexColor(hexString: String): Color {
    return try {
        val clean = if (hexString.startsWith("#")) hexString.substring(1) else hexString
        val colorInt = clean.toLong(16).let { if (clean.length == 6) 0xFF000000 or it else it }
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreateTask: (String, String, String, TaskPriority, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("أنت") }
    var dueDate by remember { mutableStateOf("اليوم") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إسناد مهمة جديدة داخل الكابينة (Room DB)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المهمة") },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل المهمة ومتطلبات التنفيذ") },
                    modifier = Modifier.fillMaxWidth().testTag("task_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    label = { Text("الشخص المسند إليه") },
                    modifier = Modifier.fillMaxWidth().testTag("task_assignee_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("موعد التسليم المتوقع (Due Date)") },
                    modifier = Modifier.fillMaxWidth().testTag("task_due_date_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeSelectChip("اليوم", dueDate == "اليوم") { dueDate = "اليوم" }
                    TypeSelectChip("غداً", dueDate == "غداً") { dueDate = "غداً" }
                    TypeSelectChip("خلال أسبوع", dueDate == "خلال أسبوع") { dueDate = "خلال أسبوع" }
                }

                Text(
                    text = "مستوى الأهمية (Priority):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TypeSelectChip("عالية", selectedPriority == TaskPriority.HIGH) { selectedPriority = TaskPriority.HIGH }
                    TypeSelectChip("متوسطة", selectedPriority == TaskPriority.MEDIUM) { selectedPriority = TaskPriority.MEDIUM }
                    TypeSelectChip("منخفضة", selectedPriority == TaskPriority.LOW) { selectedPriority = TaskPriority.LOW }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateTask(title, description, assignee, selectedPriority, dueDate)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_create_task_button")
            ) {
                Text("إسناد المهمة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: CabinTask,
    onDismiss: () -> Unit,
    onSaveTask: (CabinTask) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var assignee by remember { mutableStateOf(task.assignee) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var selectedPriority by remember { mutableStateOf(task.priority) }
    var selectedStatus by remember { mutableStateOf(task.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل بيانات المهمة (Room DB)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المهمة") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_task_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل المهمة") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_task_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    label = { Text("المسند إليه") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("موعد التسليم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Text(
                    text = "الحالة الحالية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TypeSelectChip("قيد الانتظار", selectedStatus == TaskStatus.TODO) { selectedStatus = TaskStatus.TODO }
                    TypeSelectChip("جاري العمل", selectedStatus == TaskStatus.IN_PROGRESS) { selectedStatus = TaskStatus.IN_PROGRESS }
                    TypeSelectChip("مكتملة", selectedStatus == TaskStatus.DONE) { selectedStatus = TaskStatus.DONE }
                }

                Text(
                    text = "مستوى الأهمية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TypeSelectChip("عالية", selectedPriority == TaskPriority.HIGH) { selectedPriority = TaskPriority.HIGH }
                    TypeSelectChip("متوسطة", selectedPriority == TaskPriority.MEDIUM) { selectedPriority = TaskPriority.MEDIUM }
                    TypeSelectChip("منخفضة", selectedPriority == TaskPriority.LOW) { selectedPriority = TaskPriority.LOW }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val updated = task.copy(
                            title = title,
                            description = description,
                            assignee = assignee,
                            dueDate = dueDate,
                            priority = selectedPriority,
                            status = selectedStatus
                        )
                        onSaveTask(updated)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_edit_task_button")
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun TypeSelectChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun CabinDndDialog(
    cabinName: String,
    isCurrentlyDnd: Boolean,
    onDismiss: () -> Unit,
    onSetDndMode: (Boolean, Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تخصيص وضع عدم الإزعاج (DND)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سيتم إسكات جميع الإشعارات الواردة لكبينة \"$cabinName\" للفترة المحددة:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                DndOptionCard(
                    title = "إسكات الإشعارات لمدة ساعة واحدة",
                    subtitle = "يتم إعادة تفعيل الإشعارات تلقائياً بعد ساعة",
                    onClick = {
                        onSetDndMode(true, 1)
                        onDismiss()
                    }
                )

                DndOptionCard(
                    title = "إسكات الإشعارات لمدة 8 ساعات",
                    subtitle = "مناسب لأوقات العمل أو النوم",
                    onClick = {
                        onSetDndMode(true, 8)
                        onDismiss()
                    }
                )

                DndOptionCard(
                    title = "إسكات الإشعارات لمدة 24 ساعة",
                    subtitle = "كتم التنبيهات طوال اليوم",
                    onClick = {
                        onSetDndMode(true, 24)
                        onDismiss()
                    }
                )

                DndOptionCard(
                    title = "كتم الإشعارات دائمًا",
                    subtitle = "حتى تقوم بإلغاء الكتم يدويًا",
                    onClick = {
                        onSetDndMode(true, 0)
                        onDismiss()
                    }
                )

                if (isCurrentlyDnd) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onSetDndMode(false, 0)
                            onDismiss()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء كتم الإشعارات (تشغيل التنبيهات)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun DndOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExportBackupDialog(
    onDismiss: () -> Unit,
    onGenerateBackup: (String) -> Unit,
    exportedText: String?,
    onCopyToClipboard: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تصدير نسخة احتياطية مشفرة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "سيتم تشفير بيانات الكبينات، الرسائل، والمهام باستخدام مفتاح خوارزمية AES-256-GCM المحفوظ بـ Android Keystore وكلمة المرور الخاصة بك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة مرور حماية النسخة الاحتياطية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (exportedText == null) {
                    Button(
                        onClick = {
                            if (password.isNotBlank()) {
                                onGenerateBackup(password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = password.isNotBlank()
                    ) {
                        Text("تشفير وتوليد النسخة الاحتياطية الآن")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = exportedText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Button(
                        onClick = {
                            onCopyToClipboard(exportedText)
                            isCopied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isCopied) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isCopied) "تم نسخ النص المشفر بالحافظة!" else "نسخ النص المشفر لنقله لجهاز آخر")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestore: (String, String) -> Unit,
    resultMessage: String?
) {
    var backupJson by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استعادة نسخة احتياطية مشفرة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الصق النص المشفر للنسخة الاحتياطية وأدخل كلمة المرور لفك التشفير واستعادة البيانات فوراً:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = backupJson,
                    onValueChange = { backupJson = it },
                    label = { Text("النص المشفر للنسخة الاحتياطية (JSON)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 5
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة مرور النسخة الاحتياطية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (resultMessage != null) {
                    Text(
                        text = resultMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (resultMessage.contains("نجاح")) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (backupJson.isNotBlank() && password.isNotBlank()) {
                            onRestore(backupJson, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = backupJson.isNotBlank() && password.isNotBlank()
                ) {
                    Text("فك التشفير واستعادة البيانات الآن")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
