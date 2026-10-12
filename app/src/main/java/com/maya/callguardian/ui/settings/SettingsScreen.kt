package com.maya.callguardian.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.ai.model_registry.ModelRegistry
import com.maya.callguardian.data.preferences.SettingsRepository
import com.maya.callguardian.domain.models.ModelStatus
import com.maya.callguardian.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modelRegistry: ModelRegistry,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val localOnly by settingsRepository.isLocalOnlyProcessing.collectAsState(initial = true)
    val historyRetention by settingsRepository.isHistoryRetentionEnabled.collectAsState(initial = true)
    val notifications by settingsRepository.isNotificationsEnabled.collectAsState(initial = true)
    val audioInspection by settingsRepository.isAudioInspectionEnabled.collectAsState(initial = true)

    val models = remember { modelRegistry.getRegisteredModels() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "SETTINGS & PRIVACY SAFEGUARDS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "MAYA security architecture and on-device privacy preferences",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Privacy Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DATA PRIVACY & LOCAL PROCESSING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Strict Local-Only Processing",
                    subtitle = "All transcription, heuristics, and semantic checks remain strictly on this device.",
                    checked = localOnly,
                    onCheckedChange = { coroutineScope.launch { settingsRepository.setLocalOnlyProcessing(it) } }
                )

                HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingToggleRow(
                    title = "Retain Call History Locally",
                    subtitle = "Save call risk scores and identified scam signals in local Room database.",
                    checked = historyRetention,
                    onCheckedChange = { coroutineScope.launch { settingsRepository.setHistoryRetentionEnabled(it) } }
                )

                HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingToggleRow(
                    title = "High-Priority Threat Alerts",
                    subtitle = "Receive notifications when caller behavior crosses MEDIUM or HIGH thresholds.",
                    checked = notifications,
                    onCheckedChange = { coroutineScope.launch { settingsRepository.setNotificationsEnabled(it) } }
                )

                HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 12.dp))

                SettingToggleRow(
                    title = "Speech-To-Text Audio Inspection",
                    subtitle = "Enable real-time speech transcription when microphone capture is active.",
                    checked = audioInspection,
                    onCheckedChange = { coroutineScope.launch { settingsRepository.setAudioInspectionEnabled(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // AI Model Registry & Graceful Degradation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI MODEL REGISTRY & RUNTIME STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VioletLight
                )
                Spacer(modifier = Modifier.height(12.dp))

                models.forEach { model ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val statusColor = when (model.status) {
                                ModelStatus.ACTIVE -> RiskLowGreen
                                ModelStatus.FALLBACK_ACTIVE -> VioletLight
                                else -> TextTertiary
                            }
                            Text(
                                text = model.status.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Text(
                            text = "${model.taskType} • ${model.runtime}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Fallback: ${model.fallbackStrategy}",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                    HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Privacy Policy & Security Guarantee
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SECURITY GUARANTEE & OS COMPLIANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskLowGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MAYA operates strictly within Google Play and Android security policies. It will never intercept cellular call audio without platform authorization, never send raw audio to external cloud servers by default, and never allow AI models to override safety policies or disclose credentials.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanAccent,
                checkedTrackColor = VioletAccent
            )
        )
    }
}
