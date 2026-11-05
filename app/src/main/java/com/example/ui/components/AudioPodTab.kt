package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioParticipant
import com.example.ui.viewmodel.AudioPodState
import com.example.ui.viewmodel.WebRtcConnectionStatus

@Composable
fun AudioPodTab(
    state: AudioPodState,
    onToggleJoin: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeakerMute: () -> Unit = {},
    onToggleConnectionStatus: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("audio_pod_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // WebRTC Active Connection Status Card & Indicator Toggle
        WebRtcConnectionHeaderCard(
            state = state,
            onToggleConnectionStatus = onToggleConnectionStatus
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Soundwave Waveform Animation Canvas
        AnimatedVisibility(
            visible = state.isConnected || state.connectionStatus == WebRtcConnectionStatus.CONNECTING,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            Column {
                SoundwaveVisualizerCard(
                    activeSpeaker = state.activeSpeakerName,
                    isMuted = state.isMicMuted || state.isMuted,
                    isSpeakerMuted = state.isSpeakerMuted,
                    connectionStatus = state.connectionStatus
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Participants Grid Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المشاركون في الغرفة (${state.participants.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (state.isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "DTLS Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DTLS-SRTP 256-bit P2P",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .testTag("audio_participants_grid"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.participants, key = { it.id }) { participant ->
                ParticipantCard(participant = participant)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // WebRTC Control Toolbar: Mic & Speaker Mute Controls + Call Join/Leave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_pod_controls_toolbar"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isConnected || state.connectionStatus == WebRtcConnectionStatus.CONNECTING || state.connectionStatus == WebRtcConnectionStatus.RECONNECTING) {
                // Mic Mute Control Button
                val micBgColor by animateColorAsState(
                    targetValue = if (state.isMicMuted || state.isMuted) Color(0xFFFF5252).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                    label = "mic_bg_color"
                )
                val micIconColor by animateColorAsState(
                    targetValue = if (state.isMicMuted || state.isMuted) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                    label = "mic_icon_color"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(micBgColor)
                            .border(1.5.dp, micIconColor.copy(alpha = 0.5f), CircleShape)
                            .testTag("mute_mic_button")
                            .semantics {
                                contentDescription = if (state.isMicMuted || state.isMuted) "إلغاء كتم الميكروفون" else "كتم الميكروفون"
                            }
                    ) {
                        Icon(
                            imageVector = if (state.isMicMuted || state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint = micIconColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.isMicMuted || state.isMuted) "مايك مكتوم" else "المايك يعمل",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speaker Mute Control Button (Deafen)
                val speakerBgColor by animateColorAsState(
                    targetValue = if (state.isSpeakerMuted) Color(0xFFFF9800).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    label = "speaker_bg_color"
                )
                val speakerIconColor by animateColorAsState(
                    targetValue = if (state.isSpeakerMuted) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "speaker_icon_color"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleSpeakerMute,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(speakerBgColor)
                            .border(1.5.dp, speakerIconColor.copy(alpha = 0.5f), CircleShape)
                            .testTag("mute_speaker_button")
                            .semantics {
                                contentDescription = if (state.isSpeakerMuted) "تشغيل الصوت" else "كتم صوت السماعة"
                            }
                    ) {
                        Icon(
                            imageVector = if (state.isSpeakerMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = speakerIconColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.isSpeakerMuted) "السماعة مكتومة" else "السماعة تعمل",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Leave Room Button
                Button(
                    onClick = onToggleJoin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                        .testTag("leave_pod_button")
                        .semantics {
                            contentDescription = "مغادرة الغرفة الصوتية المشفرة"
                        }
                ) {
                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مغادرة الغرفة",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Join Call Button
                Button(
                    onClick = onToggleJoin,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("join_pod_button")
                        .semantics {
                            contentDescription = "الانضمام للغرفة الصوتية المشفرة"
                        }
                ) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الانضمام للغرفة الصوتية المشفرة (WebRTC P2P)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun WebRtcConnectionHeaderCard(
    state: AudioPodState,
    onToggleConnectionStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_security_banner"),
        colors = CardDefaults.cardColors(
            containerColor = when (state.connectionStatus) {
                WebRtcConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                WebRtcConnectionStatus.CONNECTING -> Color(0xFFFFC107).copy(alpha = 0.15f)
                WebRtcConnectionStatus.RECONNECTING -> Color(0xFFFF5722).copy(alpha = 0.15f)
                WebRtcConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Zero Server Storage Lock",
                        tint = when (state.connectionStatus) {
                            WebRtcConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                            WebRtcConnectionStatus.CONNECTING -> Color(0xFFFFB300)
                            WebRtcConnectionStatus.RECONNECTING -> Color(0xFFFF5722)
                            WebRtcConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الغرفة الصوتية المشفرة (WebRTC P2P Pod)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "البث المباشر مشفر طرف لآخر (DTLS-SRTP) بدون أي تخزين سحابي",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Connection Status Toggle Chip for testing active states
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleConnectionStatus() }
                        .testTag("webrtc_connection_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تبديل حالة الاتصال",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تبديل الحالة",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Connection Status Indicator Badge Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webrtc_status_chip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when (state.connectionStatus) {
                            WebRtcConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                            WebRtcConnectionStatus.CONNECTING -> Color(0xFFFFB300)
                            WebRtcConnectionStatus.RECONNECTING -> Color(0xFFFF5722)
                            WebRtcConnectionStatus.DISCONNECTED -> Color.Gray
                        }

                        if (state.connectionStatus == WebRtcConnectionStatus.CONNECTING || state.connectionStatus == WebRtcConnectionStatus.RECONNECTING) {
                            val infiniteTransition = rememberInfiniteTransition(label = "spin")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "angle"
                            )
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(angle)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val statusText = when (state.connectionStatus) {
                            WebRtcConnectionStatus.CONNECTED -> "الاتصال نشط (WebRTC Connected • P2P)"
                            WebRtcConnectionStatus.CONNECTING -> "جاري إنشاء اتصال WebRTC P2P..."
                            WebRtcConnectionStatus.RECONNECTING -> "جاري إعادة الاتصال بالأقران (ICE Re-negotiation)..."
                            WebRtcConnectionStatus.DISCONNECTED -> "الغرفة الصوتية غير متصلة"
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (state.isConnected) {
                        Text(
                            text = "${state.latencyMs} ms | ${state.bitrateKbps} kbps",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundwaveVisualizerCard(
    activeSpeaker: String,
    isMuted: Boolean,
    isSpeakerMuted: Boolean,
    connectionStatus: WebRtcConnectionStatus
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveValue"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundwave_visualizer_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSpeakerMuted -> Color(0xFFFF9800)
                                isMuted -> Color(0xFFFF5252)
                                connectionStatus == WebRtcConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.secondary
                                else -> Color.Gray
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSpeakerMuted -> "السماعة مكتومة (تم كتم الصوت الوارد)"
                        isMuted -> "ميكروفونك مكتوم • يتحدث الآن: $activeSpeaker"
                        connectionStatus == WebRtcConnectionStatus.CONNECTED -> "يتحدث الآن: $activeSpeaker"
                        else -> "جاري تجهيز الصوت..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Equalizer Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                repeat(16) { index ->
                    val factor = if (index % 2 == 0) waveAnim else (1.3f - waveAnim).coerceIn(0.2f, 1f)
                    val isAudioDisabled = isSpeakerMuted || (isMuted && activeSpeaker == "أنت") || connectionStatus != WebRtcConnectionStatus.CONNECTED
                    val heightDp = if (isAudioDisabled) 6.dp else (32 * factor).dp
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(heightDp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isAudioDisabled) Color.LightGray else primaryColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantCard(participant: AudioParticipant) {
    val isSpeaking = participant.isSpeaking
    val borderColor = if (isSpeaking) MaterialTheme.colorScheme.secondary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag("participant_card_${participant.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSpeaking) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = participant.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (participant.isMuted) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "مكتوم",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${participant.pingMs}ms • Signal Verified",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
