package com.maya.callguardian.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.data.database.CallHistoryEntity
import com.maya.callguardian.data.repositories.CallHistoryRepository
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallHistoryScreen(
    historyRepository: CallHistoryRepository,
    modifier: Modifier = Modifier
) {
    val sessions by historyRepository.allSessions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CALL SECURITY LOGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${sessions.size} sessions stored locally on device",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (sessions.isNotEmpty()) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All History",
                        tint = RiskCriticalRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Call History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Analyzed calls and simulated demonstrations will appear here.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions) { session ->
                    HistoryItemCard(
                        session = session,
                        onDeleteClick = {
                            coroutineScope.launch {
                                historyRepository.deleteSession(session.sessionId)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Call History?", color = TextPrimary) },
            text = { Text("This will permanently remove all stored call records and evidence from this device.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            historyRepository.clearHistory()
                            showClearConfirm = false
                        }
                    }
                ) {
                    Text("Clear All", color = RiskCriticalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
private fun HistoryItemCard(
    session: CallHistoryEntity,
    onDeleteClick: () -> Unit
) {
    val level = try {
        RiskLevel.valueOf(session.finalRiskLevel)
    } catch (e: Exception) {
        RiskLevel.LOW
    }

    val badgeColor = when (level) {
        RiskLevel.LOW -> RiskLowGreen
        RiskLevel.MEDIUM -> RiskMediumAmber
        RiskLevel.HIGH -> RiskHighOrange
        RiskLevel.CRITICAL -> RiskCriticalRed
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(session.startTimeMs) { dateFormat.format(Date(session.startTimeMs)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${session.finalRiskScore} / 100",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.callerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${session.scamCategory} • ${session.durationSeconds}s",
                    fontSize = 12.sp,
                    color = CyanAccent
                )

                Text(
                    text = "$dateStr • ${session.operationMode.replace("_", " ")}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Record",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
