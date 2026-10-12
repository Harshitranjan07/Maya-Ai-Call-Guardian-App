package com.maya.callguardian.ui.capability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.callguardian.call.integration.CapabilityDetector
import com.maya.callguardian.call.integration.DeviceCapabilityReport
import com.maya.callguardian.ui.theme.*

@Composable
fun CapabilityScreen(
    capabilityDetector: CapabilityDetector,
    modifier: Modifier = Modifier
) {
    val report: DeviceCapabilityReport = remember { capabilityDetector.detectCapabilities() }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "PLATFORM CAPABILITIES & LIMITATIONS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Hardware diagnostics and Android OS permission boundaries",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Platform Constraints Card (Crucial for honest Android engineering!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RiskMediumAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CELLULAR CALL AUDIO RESTRICTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskMediumAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.cellularAudioCaptureLimitationReason,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resolution: MAYA provides a full-fidelity 12-stage Simulated Call Mode and a Live Microphone Mode to test and validate speech pipelines safely without violating Android security.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = CyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Device Diagnostics Matrix
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HARDWARE & PERMISSIONS AUDIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VioletLight
                )
                Spacer(modifier = Modifier.height(10.dp))

                CapabilityRow("Android OS Version", "${report.androidVersion} (API ${report.apiLevel})", true)
                CapabilityRow("Telephony Subsystem", if (report.hasTelephonyHardware) "AVAILABLE" else "EMULATOR / TABLET", report.hasTelephonyHardware)
                CapabilityRow("Phone State Monitoring", if (report.phoneStatePermissionGranted) "GRANTED" else "NEEDS PERMISSION", report.phoneStatePermissionGranted)
                CapabilityRow("Caller ID & Call Log", if (report.callLogPermissionGranted) "GRANTED" else "NEEDS PERMISSION", report.callLogPermissionGranted)
                CapabilityRow("Saved Contacts Access", if (report.contactsPermissionGranted) "GRANTED" else "NEEDS PERMISSION", report.contactsPermissionGranted)
                CapabilityRow("Microphone Testing Access", if (report.audioRecordPermissionGranted) "GRANTED" else "NEEDS PERMISSION", report.audioRecordPermissionGranted)
                CapabilityRow("Speech Recognizer Service", if (report.speechRecognitionAvailable) "ACTIVE" else "UNAVAILABLE", report.speechRecognitionAvailable)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Feature Matrix Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FEATURE CAPABILITY MATRIX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                MatrixItem("Deterministic Risk Engine (0-100)", "Fully Implemented (On-Device)", true)
                MatrixItem("Rule-Based Scam Detector (EN/HI)", "Fully Implemented (On-Device)", true)
                MatrixItem("Deterministic Policy Shield", "Fully Implemented (Immutable)", true)
                MatrixItem("12-Step Deterministic Simulation", "Fully Implemented (Interactive)", true)
                MatrixItem("Room Database & Deletion Control", "Fully Implemented (Local Storage)", true)
                MatrixItem("Concurrent Agent Feedback Loop", "Fully Implemented (Coroutines/Flow)", true)
                MatrixItem("Gemma Conversation Agent", "Fallback Intent Analyzer (Weights Optional)", true)
                MatrixItem("Voice Clone / Deepfake Analysis", "Unavailable (Awaiting Certified Model)", false)
            }
        }
    }
}

@Composable
private fun CapabilityRow(title: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = TextPrimary)
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOk) RiskLowGreen else RiskMediumAmber
        )
    }
}

@Composable
private fun MatrixItem(title: String, status: String, isImplemented: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                text = if (isImplemented) "READY" else "RESTRICTED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isImplemented) RiskLowGreen else RiskHighOrange
            )
        }
        Text(text = status, fontSize = 11.sp, color = TextSecondary)
    }
}
