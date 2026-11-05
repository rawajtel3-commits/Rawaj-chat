package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Cabin
import com.example.data.CabinType

@Composable
fun CabinDrawerContent(
    cabins: List<Cabin>,
    selectedCabin: Cabin?,
    onSelectCabin: (Cabin) -> Unit,
    onOpenCreateCabinDialog: () -> Unit,
    modifier: Modifier = Modifier,
    onArchiveCabin: ((Cabin) -> Unit)? = null,
    onUnarchiveCabin: ((Cabin) -> Unit)? = null,
    onOpenQrScanner: (() -> Unit)? = null,
    onOpenQrShare: (() -> Unit)? = null,
    onOpenCentralizedTasks: (() -> Unit)? = null,
    totalTasksCount: Int = 0
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<CabinType?>(null) }
    var showArchivedCabins by remember { mutableStateOf(false) }

    val activeCabins = cabins.filter { !it.isArchived }
    val archivedCabins = cabins.filter { it.isArchived }

    val allParticipantTags = remember(activeCabins) {
        activeCabins.flatMap { it.getParticipantTagsList() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredCabins = remember(activeCabins, selectedFilter, searchQuery, selectedTagFilter) {
        activeCabins.filter { cabin ->
            val matchesType = selectedFilter == null || cabin.type == selectedFilter
            val matchesQuery = searchQuery.isBlank() || cabin.matchesSearch(searchQuery)
            val matchesTag = selectedTagFilter == null || cabin.getParticipantTagsList().any {
                it.equals(selectedTagFilter, ignoreCase = true)
            }
            matchesType && matchesQuery && matchesTag
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .testTag("cabin_navigation_drawer"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .semantics { contentDescription = "رأس قائمة كبائن التواصل" }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "شعار كبائن التواصل المشفرة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "شات رواج",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشفر بالنواة (Signal E2EE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Action Button: Create New Cabin
            Button(
                onClick = onOpenCreateCabinDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("create_cabin_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة كبينة خاصة جديدة",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "إنشاء كبينة خاصة جديدة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            // Centralized Tasks Navigation Entry
            if (onOpenCentralizedTasks != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onOpenCentralizedTasks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("drawer_centralized_tasks_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = "لوحة المهام المركزية",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "لوحة المهام المركزية",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalTasksCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(text = "$totalTasksCount", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            if (onOpenQrScanner != null || onOpenQrShare != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenQrScanner != null) {
                        Button(
                            onClick = onOpenQrScanner,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("drawer_qr_scanner_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "مسح QR", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (onOpenQrShare != null && selectedCabin != null) {
                        Button(
                            onClick = onOpenQrShare,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("drawer_qr_share_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "مشاركة QR", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cabin Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "بحث بالاسم أو الوسوم (@سارة، #هندسة)...",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث في الكبائن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank() || selectedTagFilter != null) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                selectedTagFilter = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cabin_search_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            )

            // Participant Tags Filter Row
            if (allParticipantTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTagFilter == null && searchQuery.isBlank(),
                            onClick = {
                                selectedTagFilter = null
                                searchQuery = ""
                            },
                            label = { Text("الكل", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                            modifier = Modifier.testTag("cabin_tag_chip_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(allParticipantTags) { tag ->
                        val isTagSelected = selectedTagFilter.equals(tag, ignoreCase = true) ||
                                (searchQuery.isNotBlank() && searchQuery.trim().equals(tag.trim(), ignoreCase = true))
                        FilterChip(
                            selected = isTagSelected,
                            onClick = {
                                if (isTagSelected) {
                                    selectedTagFilter = null
                                    if (searchQuery.trim().equals(tag.trim(), ignoreCase = true)) {
                                        searchQuery = ""
                                    }
                                } else {
                                    selectedTagFilter = tag
                                }
                            },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                            modifier = Modifier.testTag("cabin_tag_chip_${tag.removePrefix("@").removePrefix("#")}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("الكل", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_chip_all"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == CabinType.WORK,
                    onClick = { selectedFilter = CabinType.WORK },
                    label = { Text("عمل", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_chip_work"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == CabinType.FAMILY,
                    onClick = { selectedFilter = CabinType.FAMILY },
                    label = { Text("عائلة", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_chip_family"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == CabinType.SECRET_VAULT,
                    onClick = { selectedFilter = CabinType.SECRET_VAULT },
                    label = { Text("سرية", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_chip_secret"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cabins Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedTagFilter != null)
                        "نتائج البحث (${filteredCabins.size})"
                    else
                        "الكبائن النشطة (${filteredCabins.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isNotBlank() || selectedTagFilter != null) {
                    Text(
                        text = "إلغاء الفلترة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                searchQuery = ""
                                selectedTagFilter = null
                            }
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cabins List with M3 NavigationDrawerItem
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredCabins.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .testTag("no_cabins_found_message"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "لا توجد كبائن تطابق البحث",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedTagFilter = null
                                        selectedFilter = null
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("عرض كل الكبائن", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredCabins, key = { it.id }) { cabin ->
                        val isSelected = selectedCabin?.id == cabin.id
                        val customColor = if (cabin.colorHex.isNotBlank()) parseHexColor(cabin.colorHex) else null
                        val icon = when (cabin.type) {
                            CabinType.WORK -> Icons.Default.Work
                            CabinType.FAMILY -> Icons.Default.FamilyRestroom
                            CabinType.SECRET_VAULT -> Icons.Default.Lock
                            CabinType.PUBLIC -> Icons.Default.Public
                        }

                        val iconTint = customColor ?: when (cabin.type) {
                            CabinType.WORK -> MaterialTheme.colorScheme.primary
                            CabinType.FAMILY -> Color(0xFFFFB300)
                            CabinType.SECRET_VAULT -> MaterialTheme.colorScheme.secondary
                            CabinType.PUBLIC -> Color(0xFF00E5FF)
                        }

                        val targetContainerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        } else {
                            Color.Transparent
                        }
                        val animatedContainerColor by animateColorAsState(
                            targetValue = targetContainerColor,
                            animationSpec = tween(250),
                            label = "drawer_item_bg_anim"
                        )

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f),
                            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f)
                        ) {
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cabin.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            if (cabin.isAudioPodActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "غرفة صوتية نشطة",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (cabin.isDndActive()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsOff,
                                                    contentDescription = "وضع عدم الإزعاج مفعل",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${cabin.memberCount} أعضاء • ${cabin.securityLevel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )

                                        // Render participant tags
                                        val tags = cabin.getParticipantTagsList()
                                        if (tags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                tags.take(3).forEach { tag ->
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.padding(vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (tags.size > 3) {
                                                    Text(
                                                        text = "+${tags.size - 3}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    if (cabin.iconEmoji.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(iconTint.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cabin.iconEmoji,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "أيقونة الكبينة",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else iconTint
                                        )
                                    }
                                },
                                badge = {
                                    if (cabin.unreadCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(
                                                text = cabin.unreadCount.toString(),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                },
                                selected = isSelected,
                                onClick = { onSelectCabin(cabin) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cabin_item_${cabin.id}"),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = animatedContainerColor,
                                    unselectedContainerColor = animatedContainerColor,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Archived Cabins Section
            if (archivedCabins.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archived_cabins_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                                .clickable { showArchivedCabins = !showArchivedCabins }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "الكبائن المؤرشفة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الكبائن المؤرشفة (${archivedCabins.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showArchivedCabins) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = showArchivedCabins,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                archivedCabins.forEach { cabin ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (cabin.iconEmoji.isNotBlank()) {
                                                    Text(
                                                        text = cabin.iconEmoji,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = cabin.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "محفوظة دون حذف البيانات",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { onUnarchiveCabin?.invoke(cabin) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("restore_cabin_${cabin.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Unarchive,
                                                    contentDescription = "استعادة الكبينة",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("استعادة", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Vault Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keystore_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "أمان عتاد أندرويد",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Android Keystore Verified",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مفاتيح التشفير مخزنة بأمان عتادي",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
