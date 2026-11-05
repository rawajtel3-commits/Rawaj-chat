package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Cabin
import com.example.ui.viewmodel.AudioPodState
import com.example.ui.viewmodel.CabinTabType

@Composable
fun HeaderSpeakingEqualizer(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_equalizer")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(bar1Height, bar2Height, bar3Height).forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((12 * h).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun CabinHeader(
    cabin: Cabin?,
    selectedTab: CabinTabType,
    onTabSelected: (CabinTabType) -> Unit,
    onOpenDrawer: () -> Unit,
    isDecryptedView: Boolean,
    onToggleCipherView: () -> Unit,
    taskCount: Int = 0,
    audioPodState: AudioPodState = AudioPodState(),
    onArchiveCabin: (() -> Unit)? = null,
    onOpenDndDialog: (() -> Unit)? = null,
    onOpenCustomizeAppearance: (() -> Unit)? = null,
    onOpenQrShare: (() -> Unit)? = null,
    onOpenQrScanner: (() -> Unit)? = null
) {
    val isSomeoneSpeaking = audioPodState.isConnected && (audioPodState.isSpeaking || audioPodState.participants.any { it.isSpeaking })
    val isDndActive = cabin?.isDndActive() == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "فتح القائمة",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (cabin?.iconEmoji?.isNotBlank() == true) {
                        Text(
                            text = cabin.iconEmoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = cabin?.name ?: "اختر كبينة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSomeoneSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cabin?.securityLevel ?: "مشفر بالنواة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Animated Speaker Indicator Badge in Top Bar
            AnimatedVisibility(
                visible = isSomeoneSpeaking,
                enter = fadeIn(tween(250)) + slideInHorizontally { it / 2 },
                exit = fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("active_speaker_header_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderSpeakingEqualizer(
                            barColor = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = audioPodState.activeSpeakerName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // E2EE Cipher View Toggle Button
            IconButton(
                onClick = onToggleCipherView
            ) {
                Icon(
                    imageVector = if (isDecryptedView) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "عرض التشفير",
                    tint = if (isDecryptedView) MaterialTheme.colorScheme.secondary else Color(0xFFFFB300)
                )
            }

            if (onOpenQrShare != null && cabin != null) {
                IconButton(
                    onClick = onOpenQrShare,
                    modifier = Modifier.testTag("qr_share_cabin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "مشاركة رمز QR للكبينة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onOpenQrScanner != null) {
                IconButton(
                    onClick = onOpenQrScanner,
                    modifier = Modifier.testTag("qr_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "ماسح رمز QR",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onOpenDndDialog != null && cabin != null) {
                IconButton(
                    onClick = onOpenDndDialog,
                    modifier = Modifier.testTag("dnd_cabin_button")
                ) {
                    Icon(
                        imageVector = if (isDndActive) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                        contentDescription = "وضع عدم الإزعاج",
                        tint = if (isDndActive) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onOpenCustomizeAppearance != null && cabin != null) {
                IconButton(
                    onClick = onOpenCustomizeAppearance,
                    modifier = Modifier.testTag("customize_cabin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "تخصيص المظهر والرمز",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onArchiveCabin != null && cabin != null) {
                IconButton(
                    onClick = onArchiveCabin,
                    modifier = Modifier.testTag("archive_cabin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "أرشفة الكبينة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Tabs
        val tabs = listOf(
            CabinTabType.CHAT to "الرسائل",
            CabinTabType.TASKS to "المهام",
            CabinTabType.AUDIO_POD to "الصوت المشفر",
            CabinTabType.SECURITY_VAULT to "الخزنة والأمن"
        )

        val selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, (tabType, label) ->
                val isSelected = index == selectedTabIndex
                val icon = when (tabType) {
                    CabinTabType.CHAT -> Icons.Default.Chat
                    CabinTabType.TASKS -> Icons.Default.Assignment
                    CabinTabType.AUDIO_POD -> Icons.Default.Mic
                    CabinTabType.SECURITY_VAULT -> Icons.Default.Key
                }

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tabType) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tabType == CabinTabType.AUDIO_POD && isSomeoneSpeaking) {
                                HeaderSpeakingEqualizer(
                                    barColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tabType == CabinTabType.TASKS && taskCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(text = taskCount.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

