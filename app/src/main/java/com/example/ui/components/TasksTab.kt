package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Cabin
import com.example.data.CabinTask
import com.example.data.TaskPriority
import com.example.data.TaskStatus

enum class TaskFilter {
    ALL,
    TODO,
    IN_PROGRESS,
    DONE,
    HIGH_PRIORITY
}

@Composable
fun TasksTab(
    tasks: List<CabinTask>,
    cabinName: String = "الكبينة الحالية",
    onUpdateTaskStatus: (CabinTask, TaskStatus) -> Unit,
    onDeleteTask: (CabinTask) -> Unit,
    onOpenAddTaskDialog: () -> Unit,
    onUpdateTask: ((CabinTask) -> Unit)? = null,
    allTasksAcrossCabins: List<CabinTask> = emptyList(),
    allCabins: List<Cabin> = emptyList(),
    onToggleTaskCompletion: ((CabinTask) -> Unit)? = null,
    onSelectCabin: ((Cabin) -> Unit)? = null,
    initialCentralizedView: Boolean = false
) {
    var isCentralizedView by remember(initialCentralizedView) { mutableStateOf(initialCentralizedView) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<CabinTask?>(null) }

    val effectiveAllTasks = if (allTasksAcrossCabins.isNotEmpty()) allTasksAcrossCabins else tasks

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tasks_tab")
    ) {
        // Mode Switcher: Single Cabin vs Centralized Dashboard for All Cabins
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp)
                .testTag("tasks_view_mode_switcher")
        ) {
            val activeBg = MaterialTheme.colorScheme.primary
            val activeFg = MaterialTheme.colorScheme.onPrimary
            val inactiveFg = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isCentralizedView) activeBg else Color.Transparent)
                    .clickable { isCentralizedView = false }
                    .padding(vertical = 8.dp)
                    .testTag("tab_mode_single_cabin"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "مهام $cabinName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (!isCentralizedView) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isCentralizedView) activeFg else inactiveFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCentralizedView) activeBg else Color.Transparent)
                    .clickable { isCentralizedView = true }
                    .padding(vertical = 8.dp)
                    .testTag("tab_mode_centralized_tasks"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "لوحة المهام المركزية",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isCentralizedView) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCentralizedView) activeFg else inactiveFg
                    )
                    if (effectiveAllTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isCentralizedView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${effectiveAllTasks.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isCentralizedView) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isCentralizedView) {
            CentralizedTaskDashboard(
                tasks = effectiveAllTasks,
                cabins = allCabins,
                onToggleTaskCompletion = { task ->
                    onToggleTaskCompletion?.invoke(task) ?: run {
                        val next = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
                        onUpdateTaskStatus(task, next)
                    }
                },
                onUpdateTaskStatus = onUpdateTaskStatus,
                onDeleteTask = onDeleteTask,
                onSelectCabin = { cabin ->
                    onSelectCabin?.invoke(cabin)
                    isCentralizedView = false
                },
                onOpenAddTaskDialog = onOpenAddTaskDialog,
                onOpenAnalytics = { showAnalyticsDialog = true }
            )
        } else {
            val completedCount = tasks.count { it.status == TaskStatus.DONE }
            val totalCount = tasks.size
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

            val filteredTasks = tasks.filter { task ->
                val matchesFilter = when (selectedFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.TODO -> task.status == TaskStatus.TODO
                    TaskFilter.IN_PROGRESS -> task.status == TaskStatus.IN_PROGRESS
                    TaskFilter.DONE -> task.status == TaskStatus.DONE
                    TaskFilter.HIGH_PRIORITY -> task.priority == TaskPriority.HIGH
                }
                val matchesQuery = searchQuery.isBlank() ||
                        task.title.contains(searchQuery, ignoreCase = true) ||
                        task.description.contains(searchQuery, ignoreCase = true) ||
                        task.assignee.contains(searchQuery, ignoreCase = true)
                matchesFilter && matchesQuery
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Summary Card with Room Statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Task Manager",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مهام $cabinName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "قاعدة بيانات محلية خاصة (Room DB)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Analytics Chart Dialog Button
                        IconButton(
                            onClick = { showAnalyticsDialog = true },
                            modifier = Modifier.testTag("task_analytics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "عرض تحليلات ورسوم المهام",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = onOpenAddTaskDialog,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_task_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة مهمة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نسبة الإنجاز: ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedCount من $totalCount مكتملة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن مهمة أو مسند إليه...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipItem("الكل ($totalCount)", selectedFilter == TaskFilter.ALL) { selectedFilter = TaskFilter.ALL }
            FilterChipItem("قيد الانتظار", selectedFilter == TaskFilter.TODO) { selectedFilter = TaskFilter.TODO }
            FilterChipItem("جاري العمل", selectedFilter == TaskFilter.IN_PROGRESS) { selectedFilter = TaskFilter.IN_PROGRESS }
            FilterChipItem("مكتملة ($completedCount)", selectedFilter == TaskFilter.DONE) { selectedFilter = TaskFilter.DONE }
            FilterChipItem("عالية ⚡", selectedFilter == TaskFilter.HIGH_PRIORITY) { selectedFilter = TaskFilter.HIGH_PRIORITY }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث" else "لا توجد مهام في كبينة $cabinName حالياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCardItem(
                        task = task,
                        onStatusChange = { newStatus -> onUpdateTaskStatus(task, newStatus) },
                        onToggleCompletion = {
                            val nextStatus = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
                            onUpdateTaskStatus(task, nextStatus)
                        },
                        onEdit = { editingTask = task },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

    if (showAnalyticsDialog) {
        TaskAnalyticsDialog(
            tasks = tasks,
            cabinName = cabinName,
            onDismiss = { showAnalyticsDialog = false }
        )
    }

    if (editingTask != null) {
        EditTaskDialog(
            task = editingTask!!,
            onDismiss = { editingTask = null },
            onSaveTask = { updated ->
                onUpdateTask?.invoke(updated)
                editingTask = null
            }
        )
    }
}
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TaskCardItem(
    task: CabinTask,
    onStatusChange: (TaskStatus) -> Unit,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.DONE

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF5252)
        TaskPriority.MEDIUM -> Color(0xFFFFB300)
        TaskPriority.LOW -> Color(0xFF00E676)
    }

    val priorityLabel = when (task.priority) {
        TaskPriority.HIGH -> "عالية الأهمية"
        TaskPriority.MEDIUM -> "متوسطة"
        TaskPriority.LOW -> "منخفضة"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox Toggle for Room DB completion status
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = priorityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 44.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.assignee,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = task.dueDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Granular Interactive Status Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusChip(
                        label = "قيد الانتظار",
                        isSelected = task.status == TaskStatus.TODO,
                        onClick = { onStatusChange(TaskStatus.TODO) }
                    )
                    StatusChip(
                        label = "جاري العمل",
                        isSelected = task.status == TaskStatus.IN_PROGRESS,
                        onClick = { onStatusChange(TaskStatus.IN_PROGRESS) }
                    )
                    StatusChip(
                        label = "مكتملة",
                        isSelected = task.status == TaskStatus.DONE,
                        onClick = { onStatusChange(TaskStatus.DONE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 10.sp
        )
    }
}
