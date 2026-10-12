package com.maya.callguardian.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.CallState
import com.maya.callguardian.domain.models.OperationMode
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.runtime.session_manager.SessionManager
import com.maya.callguardian.ui.components.RiskMeter
import com.maya.callguardian.ui.components.SecurityBanner
import com.maya.callguardian.ui.theme.*

@Composable
fun DashboardScreen(
    sessionManager: SessionManager,
    onNavigateToLiveCall: () -> Unit,
    onNavigateToThreatDetails: () -> Unit,
    onNavigateToCapabilities: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by sessionManager.sessionState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header: Logo & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NavySurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, VioletAccent, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "MAYA Shield",
                        tint = CyanAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MAYA",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "AI Call Guardian",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Protection Active Chip
            Box(
                modifier = Modifier
                    .background(RiskLowGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, RiskLowGreen, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RiskLowGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RiskLowGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Status Banner
        SecurityBanner(
            policy = state.securityPolicy,
            onTakeOverClick = if (state.callState == CallState.ACTIVE) {
                { onNavigateToLiveCall() }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Active Session or Ready Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToLiveCall() },
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLiveCall = state.callState == CallState.ACTIVE || state.callState == CallState.RINGING
                    Text(
                        text = when (state.callState) {
                            CallState.RINGING -> "📞 INCOMING CALL RINGING"
                            CallState.ACTIVE -> "ACTIVE CALL IN PROGRESS"
                            else -> "STANDBY MONITORING"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveCall) CyanAccent else TextSecondary
                    )
                    Text(
                        text = state.operationMode.name.replace("_", " "),
                        fontSize = 10.sp,
                        color = VioletLight
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                RiskMeter(
                    score = state.riskAssessment.score,
                    level = state.riskAssessment.level,
                    size = 150
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = state.riskAssessment.primaryThreat,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val isLive = state.callState == CallState.ACTIVE || state.callState == CallState.RINGING
                if (isLive) {
                    Text(
                        text = "Caller: ${state.callerInfo.displayName} (${state.callerInfo.formattedNumber.ifBlank { state.callerInfo.phoneNumber }})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToLiveCall,
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Live Guardian Screen")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Grid: Launch Demonstration / Controls
        Text(
            text = "PROTECTIVE ACTIONS & TEST MODES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Simulated Call",
                subtitle = "12-Step Scenario",
                icon = Icons.Default.PlayArrow,
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f),
                onClick = {
                    sessionManager.startSimulation(autoAdvance = true)
                    onNavigateToLiveCall()
                }
            )

            ActionTile(
                title = "Step Scenario",
                subtitle = "Manual Testing",
                icon = Icons.Default.SkipNext,
                accentColor = VioletLight,
                modifier = Modifier.weight(1f),
                onClick = {
                    sessionManager.stepSimulation()
                    onNavigateToLiveCall()
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Threat Details",
                subtitle = "Evidence & Rules",
                icon = Icons.Default.Warning,
                accentColor = RiskHighOrange,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToThreatDetails
            )

            ActionTile(
                title = "Capabilities",
                subtitle = "Device & Audio Matrix",
                icon = Icons.Default.Info,
                accentColor = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCapabilities
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
