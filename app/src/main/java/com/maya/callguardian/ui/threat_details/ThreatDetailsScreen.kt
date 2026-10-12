package com.maya.callguardian.ui.threat_details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.runtime.session_manager.SessionManager
import com.maya.callguardian.ui.components.EvidenceCard
import com.maya.callguardian.ui.theme.*

@Composable
fun ThreatDetailsScreen(
    sessionManager: SessionManager,
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
        // Top Risk Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RISK ASSESSMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    val badgeColor = when (state.riskAssessment.level) {
                        RiskLevel.LOW -> RiskLowGreen
                        RiskLevel.MEDIUM -> RiskMediumAmber
                        RiskLevel.HIGH -> RiskHighOrange
                        RiskLevel.CRITICAL -> RiskCriticalRed
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${state.riskAssessment.score} / 100 • ${state.riskAssessment.level.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = state.scamAssessment.category.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.scamAssessment.explanation,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Semantic Agent Observations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SEMANTIC CONVERSATION AGENT FINDINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VioletLight
                )
                Spacer(modifier = Modifier.height(10.dp))

                ObservationRow("Caller Intent:", state.conversationObservation.callerIntent)
                ObservationRow("Claimed Organization:", state.conversationObservation.claimedOrganization.ifBlank { "None detected" })
                ObservationRow("Urgency Level:", state.conversationObservation.urgencyLevel)
                ObservationRow("Requested Action:", state.conversationObservation.requestedAction)
                ObservationRow("Coercive Pressure:", if (state.conversationObservation.pressureBehavior) "YES (Intimidation/Rush detected)" else "NO")

                if (state.conversationObservation.userFacingWarning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RiskCriticalRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = state.conversationObservation.userFacingWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RiskCriticalRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step Explanations & Score Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EVIDENCE & FACTOR CONTRIBUTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.riskAssessment.significantExplanations.isEmpty()) {
                    Text(
                        text = "No contributing threat factors identified yet.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                } else {
                    state.riskAssessment.significantExplanations.forEach { exp ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = "• ", color = CyanAccent, fontWeight = FontWeight.Bold)
                            Text(text = exp, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supporting Evidence Items
        Text(
            text = "EVIDENCE LOG (${state.scamAssessment.evidenceList.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.scamAssessment.evidenceList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No concrete evidence items collected yet.", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.scamAssessment.evidenceList.forEach { ev ->
                    EvidenceCard(evidence = ev)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safe Next Steps
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RiskLowGreen.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SAFE NEXT STEPS & PROTECTIVE GUIDANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskLowGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.conversationObservation.recommendedNextStep,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If in doubt, disconnect the phone call and dial the bank's official toll-free customer support number printed on the back of your physical debit or credit card.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ObservationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
