package com.maya.callguardian.ui.live_call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.domain.models.CallState
import com.maya.callguardian.domain.models.OperationMode
import com.maya.callguardian.domain.models.RiskLevel
import com.maya.callguardian.domain.models.SignalType
import com.maya.callguardian.runtime.session_manager.SessionManager
import com.maya.callguardian.ui.components.EvidenceCard
import com.maya.callguardian.ui.components.RiskMeter
import com.maya.callguardian.ui.components.SecurityBanner
import com.maya.callguardian.ui.components.TranscriptStream
import com.maya.callguardian.ui.theme.*

@Composable
fun LiveCallGuardianScreen(
    sessionManager: SessionManager,
    onNavigateToThreatDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by sessionManager.sessionState.collectAsState()
    val scrollState = rememberScrollState()
    var manualInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header: Caller Details & Status
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
                    Column {
                        Text(
                            text = state.callerInfo.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = state.callerInfo.formattedNumber.ifBlank { state.callerInfo.phoneNumber.ifBlank { "No Caller ID" } },
                            fontSize = 13.sp,
                            color = CyanAccent
                        )
                    }

                    // Mode Badge
                    Box(
                        modifier = Modifier
                            .background(VioletAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.operationMode == OperationMode.SIMULATED_DEMO) "SIMULATED CALL" else state.operationMode.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Provenance & Notes
                Text(
                    text = state.callerIntelligence.notes,
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Speakerphone Guidance for Real Phone Calls
                if (state.operationMode == OperationMode.REAL_CALL_MONITOR) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speakerphone",
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Turn on Speakerphone for Live Voice Analysis",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Android isolates cellular audio; speakerphone lets MAYA's AI listener transcribe & detect scam phrases.",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Agent Concurrent Activity Indicator
        if (state.agentExecutionState.isScamDetectorRunning || state.agentExecutionState.isConversationAgentRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VioletAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Agents evaluating live evidence & caller context...",
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Risk Gauge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RiskMeter(
                    score = state.riskAssessment.score,
                    level = state.riskAssessment.level,
                    size = 170
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.riskAssessment.primaryThreat,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Confidence: ${(state.riskAssessment.confidence * 100).toInt()}% • Signals: ${state.scamAssessment.detectedSignals.size}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Policy Banner
        SecurityBanner(
            policy = state.securityPolicy,
            onTakeOverClick = { sessionManager.userIntervene("USER_TOOK_OVER") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Detected Signal Chips
        if (state.scamAssessment.detectedSignals.isNotEmpty()) {
            Text(
                text = "DETECTED SIGNALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.scamAssessment.detectedSignals) { signal ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (signal == SignalType.OTP_REQUEST || signal == SignalType.DIGITAL_ARREST_THREAT)
                                    RiskCriticalRed.copy(alpha = 0.2f)
                                else VioletAccent.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (signal == SignalType.OTP_REQUEST) RiskCriticalRed else VioletAccent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = signal.name.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (signal == SignalType.OTP_REQUEST) RiskCriticalRed else VioletLight
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Transcript Stream
        Text(
            text = "LIVE SPEECH TRANSCRIPT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        TranscriptStream(transcripts = state.transcriptEvents)

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Dialogue Input & Quick Fraud Verification
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEST CALLER SPEECH & PHRASES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Text(
                        text = "Instant AI Evaluation",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Asking for OTP and bank details", fontSize = 11.sp, color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                sessionManager.injectTranscript(manualInput)
                                manualInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Analyze", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Microphone live toggle & status
                val isMicActive = state.agentExecutionState.isTranscribing
                Button(
                    onClick = {
                        if (isMicActive) {
                            sessionManager.stopSpeechRecognition()
                        } else {
                            sessionManager.startMicrophoneSession()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMicActive) RiskCriticalRed else NavySurfaceElevated
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMicActive) RiskCriticalRed else CyanAccent)
                ) {
                    Icon(
                        imageVector = if (isMicActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (isMicActive) TextPrimary else CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMicActive) "🔴 Microphone Active (Listening...) — Tap to Stop" else "🎙️ Start Live Mic Listening (Speak Scam Phrases)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMicActive) TextPrimary else CyanAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TAP COMMON SCAM PHRASES TO VERIFY:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Fraud Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { sessionManager.injectTranscript("Sir, please share your 6-digit OTP and bank account details immediately.") },
                        label = { Text("🔑 OTP + Bank Details", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NavySurfaceElevated, labelColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RiskCriticalRed.copy(alpha = 0.5f))
                    )
                    AssistChip(
                        onClick = { sessionManager.injectTranscript("Your bank account will be blocked within 15 minutes unless KYC is updated.") },
                        label = { Text("⚠️ Account Block Threat", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NavySurfaceElevated, labelColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RiskHighOrange.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { sessionManager.injectTranscript("This is CBI Cyber Cell. You are placed under digital arrest for illegal money laundering.") },
                        label = { Text("👮 Digital Arrest / Police", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NavySurfaceElevated, labelColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RiskCriticalRed.copy(alpha = 0.5f))
                    )
                    AssistChip(
                        onClick = { sessionManager.injectTranscript("Install AnyDesk QuickSupport app on your mobile right now.") },
                        label = { Text("📲 Install AnyDesk", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NavySurfaceElevated, labelColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VioletLight.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (state.operationMode == OperationMode.REAL_CALL_MONITOR) "LIVE CALL CONTROLS" else "SIMULATION & SESSION CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.operationMode == OperationMode.REAL_CALL_MONITOR) {
                        FilledTonalButton(
                            onClick = { sessionManager.startSpeechRecognition() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NavySurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Listen", modifier = Modifier.size(16.dp), tint = CyanAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconnect Mic", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { sessionManager.endCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Monitoring", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Monitoring", fontSize = 11.sp)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { sessionManager.stepSimulation() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NavySurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Step", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next Step", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { sessionManager.startSimulation(autoAdvance = true) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = VioletAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Replay", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replay", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { sessionManager.endCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Call", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Jump to Threat Details
        OutlinedButton(
            onClick = onNavigateToThreatDetails,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
        ) {
            Icon(Icons.Default.Security, contentDescription = "Threat Details")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Inspect Detailed Evidence & Threat Analysis")
        }
    }
}
