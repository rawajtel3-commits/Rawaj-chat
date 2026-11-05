package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.CabinType
import com.example.data.TaskPriority
import com.example.data.TaskStatus

enum class CentralizedTaskSortOrder(val titleArabic: String) {
    DUE_DATE("تاريخ الاستحقاق (الأقرب)"),
    PRIORITY_HIGH_FIRST("الأولوية (الأعلى أولاً)"),
    CABIN_NAME("اسم الكبينة"),
    NEWEST_FIRST("الأحدث إنشاءً"),
    STATUS("قيد التنفيذ أولاً")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CentralizedTaskDashboard(
    tasks: List<CabinTask>,
    cabins: List<Cabin>,
    onToggleTaskCompletion: (CabinTask) -> Unit,
    onUpdateTaskStatus: (CabinTask, TaskStatus) -> Unit,
    onDeleteTask: (CabinTask) -> Unit,
    onSelectCabin: (Cabin) -> Unit,
    onOpenAddTaskDialog: () -> Unit,
    onOpenAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOrder by remember { mutableStateOf(CentralizedTaskSortOrder.PRIORITY_HIGH_FIRST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var selectedCabinIdFilter by remember { mutableStateOf<String?>(null) }

    // Map cabin ID to cabin object for quick lookup
    val cabinMap = remember(cabins) { cabins.associateBy { it.id } }

    val totalCount = tasks.size
    val completedCount = tasks.count { it.status == TaskStatus.DONE }
    val inProgressCount = tasks.count { it.status == TaskStatus.IN_PROGRESS }
    val highPriorityPendingCount = tasks.count { it.priority == TaskPriority.HIGH && it.status != TaskStatus.DONE }
    val overallProgress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    // Filter tasks
    val filteredTasks = tasks.filter { task ->
        val cabin = cabinMap[task.cabinId]
        val cabinName = cabin?.name ?: ""

        val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true) ||
                task.assignee.contains(searchQuery, ignoreCase = true) ||
                cabinName.contains(searchQuery, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == null || task.status == selectedStatusFilter
        val matchesPriority = selectedPriorityFilter == null || task.priority == selectedPriorityFilter
        val matchesCabin = selectedCabinIdFilter == null || task.cabinId == selectedCabinIdFilter

        matchesQuery && matchesStatus && matchesPriority && matchesCabin
    }

    // Sort tasks
    val sortedTasks = when (selectedSortOrder) {
        CentralizedTaskSortOrder.DUE_DATE -> filteredTasks.sortedWith(
            compareBy<CabinTask> {
                when {
                    it.dueDate.contains("اليوم") -> 0
                    it.dueDate.contains("غداً") -> 1
                    it.dueDate.contains("يومين") -> 2
                    else -> 3
                }
            }.thenByDescending { it.createdAt }
        )
        CentralizedTaskSortOrder.PRIORITY_HIGH_FIRST -> filteredTasks.sortedWith(
            compareBy<CabinTask> {
                when (it.priority) {
                    TaskPriority.HIGH -> 0
                    TaskPriority.MEDIUM -> 1
                    TaskPriority.LOW -> 2
                }
            }.thenBy { it.status == TaskStatus.DONE }
        )
        CentralizedTaskSortOrder.CABIN_NAME -> filteredTasks.sortedBy { task ->
            cabinMap[task.cabinId]?.name ?: ""
        }
        CentralizedTaskSortOrder.NEWEST_FIRST -> filteredTasks.sortedByDescending { it.createdAt }
        CentralizedTaskSortOrder.STATUS -> filteredTasks.sortedWith(
            compareBy<CabinTask> {
                when (it.status) {
                    TaskStatus.IN_PROGRESS -> 0
                    TaskStatus.TODO -> 1
                    TaskStatus.DONE -> 2
                }
            }.thenByDescending { it.createdAt }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("centralized_task_dashboard"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dashboard Header Card with Overview Stats
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("centralized_tasks_overview_card"),
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = "لوحة المهام المركزية",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "لوحة المهام المركزية لجميع الكبائن",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تجميع وإدارة مهام Room DB المشتركة عبر الكبائن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (onOpenAnalytics != null) {
                                IconButton(
                                    onClick = onOpenAnalytics,
                                    modifier = Modifier.testTag("centralized_analytics_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = "رسوم التحليلات",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenAddTaskDialog,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("centralized_add_task_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة مهمة", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Metric Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadgeCard(
                            title = "إجمالي المهام",
                            value = "$totalCount",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeCard(
                            title = "مكتملة",
                            value = "$completedCount",
                            containerColor = Color(0xFF1B5E20).copy(alpha = 0.15f),
                            contentColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeCard(
                            title = "قيد التنفيذ",
                            value = "$inProgressCount",
                            containerColor = Color(0xFF0D47A1).copy(alpha = 0.15f),
                            contentColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadgeCard(
                            title = "عالية الأهمية",
                            value = "$highPriorityPendingCount",
                            containerColor = Color(0xFFB71C1C).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF5252),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Overall Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "نسبة إنجاز المهام العامة: ${(overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
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
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        // Search and Sorting Controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("بحث عن مهمة، مسند إليه، أو اسم الكبينة...", style = MaterialTheme.typography.bodySmall)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("centralized_task_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sorting Row with Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الترتيب حسب:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("sort_tasks_selector")
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedSortOrder.titleArabic,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                CentralizedTaskSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.titleArabic,
                                                fontWeight = if (selectedSortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedSortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedSortOrder = order
                                            showSortMenu = false
                                        },
                                        modifier = Modifier.testTag("sort_option_${order.name}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filters
                    Text(
                        text = "فلترة الحالة:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == null,
                                onClick = { selectedStatusFilter = null },
                                label = { Text("الكل ($totalCount)") },
                                modifier = Modifier.testTag("centralized_filter_status_all"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == TaskStatus.TODO,
                                onClick = { selectedStatusFilter = TaskStatus.TODO },
                                label = { Text("قيد الانتظار") },
                                modifier = Modifier.testTag("centralized_filter_status_todo")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == TaskStatus.IN_PROGRESS,
                                onClick = { selectedStatusFilter = TaskStatus.IN_PROGRESS },
                                label = { Text("قيد التنفيذ ($inProgressCount)") },
                                modifier = Modifier.testTag("centralized_filter_status_inprogress")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == TaskStatus.DONE,
                                onClick = { selectedStatusFilter = TaskStatus.DONE },
                                label = { Text("مكتملة ($completedCount)") },
                                modifier = Modifier.testTag("centralized_filter_status_done")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cabin Filters Row (Filter by specific cabin)
                    if (cabins.isNotEmpty()) {
                        Text(
                            text = "فلترة حسب الكبينة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCabinIdFilter == null,
                                    onClick = { selectedCabinIdFilter = null },
                                    label = { Text("جميع الكبائن") },
                                    modifier = Modifier.testTag("centralized_filter_cabin_all")
                                )
                            }
                            items(cabins, key = { it.id }) { cabin ->
                                val taskCountForCabin = tasks.count { it.cabinId == cabin.id }
                                FilterChip(
                                    selected = selectedCabinIdFilter == cabin.id,
                                    onClick = {
                                        selectedCabinIdFilter = if (selectedCabinIdFilter == cabin.id) null else cabin.id
                                    },
                                    label = {
                                        Text("${cabin.iconEmoji.ifBlank { "🏕️" }} ${cabin.name} ($taskCountForCabin)")
                                    },
                                    modifier = Modifier.testTag("centralized_filter_cabin_${cabin.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filtered Results Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المهام المعروضة (${sortedTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedStatusFilter != null || selectedPriorityFilter != null || selectedCabinIdFilter != null || searchQuery.isNotBlank()) {
                    Text(
                        text = "إعادة ضبط الفلاتر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                searchQuery = ""
                                selectedStatusFilter = null
                                selectedPriorityFilter = null
                                selectedCabinIdFilter = null
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Tasks List
        if (sortedTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("centralized_tasks_empty_state"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedStatusFilter != null || selectedCabinIdFilter != null)
                                "لا توجد مهام تطابق معايير الفلترة الحالية"
                            else
                                "رائع! لا توجد مهام مسجلة حالياً",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك إضافة مهمة جديدة وتعيينها لأي كبينة خاصة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenAddTaskDialog,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة أول مهمة")
                        }
                    }
                }
            }
        } else {
            items(sortedTasks, key = { it.id }) { task ->
                val cabin = cabinMap[task.cabinId]
                CentralizedTaskCardItem(
                    task = task,
                    cabin = cabin,
                    onToggleCompletion = { onToggleTaskCompletion(task) },
                    onUpdateStatus = { newStatus -> onUpdateTaskStatus(task, newStatus) },
                    onDelete = { onDeleteTask(task) },
                    onNavigateToCabin = {
                        if (cabin != null) {
                            onSelectCabin(cabin)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MetricBadgeCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CentralizedTaskCardItem(
    task: CabinTask,
    cabin: Cabin?,
    onToggleCompletion: () -> Unit,
    onUpdateStatus: (TaskStatus) -> Unit,
    onDelete: () -> Unit,
    onNavigateToCabin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = task.status == TaskStatus.DONE
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFFF5252)
        TaskPriority.MEDIUM -> Color(0xFFFFB300)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }
    val priorityText = when (task.priority) {
        TaskPriority.HIGH -> "عالية"
        TaskPriority.MEDIUM -> "متوسطة"
        TaskPriority.LOW -> "منخفضة"
    }

    val cabinColor = if (cabin != null && cabin.colorHex.isNotBlank()) {
        parseHexColor(cabin.colorHex)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val containerBg by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        label = "card_bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("centralized_task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Cabin Origin Tag + Priority Badge + Open Cabin Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cabin Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = cabinColor.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onNavigateToCabin() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cabin?.iconEmoji?.ifBlank { "🏕️" } ?: "🏕️",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cabin?.name ?: "الكبينة المشفرة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = cabinColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "الانتقال للكبينة",
                            tint = cabinColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Priority Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = priorityText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content: Checkbox + Title + Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Large Checkbox for Instant One-Tap Completion
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF4CAF50),
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("task_complete_checkbox_${task.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meta: Assignee + Due Date + Status Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👤 ${task.assignee.ifBlank { "غير مسند" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📅 ${task.dueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Chip: Click to Cycle Status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (task.status) {
                                TaskStatus.DONE -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                TaskStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                TaskStatus.TODO -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .clickable {
                                    val next = when (task.status) {
                                        TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                                        TaskStatus.IN_PROGRESS -> TaskStatus.DONE
                                        TaskStatus.DONE -> TaskStatus.TODO
                                    }
                                    onUpdateStatus(next)
                                }
                                .testTag("task_status_chip_${task.id}")
                        ) {
                            Text(
                                text = when (task.status) {
                                    TaskStatus.DONE -> "✓ مكتملة"
                                    TaskStatus.IN_PROGRESS -> "⏳ قيد التنفيذ"
                                    TaskStatus.TODO -> "○ قيد الانتظار"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = when (task.status) {
                                    TaskStatus.DONE -> Color(0xFF4CAF50)
                                    TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                                    TaskStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_task_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف المهمة",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
