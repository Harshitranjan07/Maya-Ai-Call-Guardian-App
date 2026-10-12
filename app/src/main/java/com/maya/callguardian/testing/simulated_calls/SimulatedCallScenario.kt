package com.maya.callguardian.testing.simulated_calls

import com.maya.callguardian.domain.models.Speaker
import com.maya.callguardian.domain.models.TranscriptEvent

/**
 * Deterministic 12-stage simulated call demonstration scenario.
 * Allows reproducible validation of the entire multi-agent security pipeline
 * without requiring live cellular audio hardware.
 */
data class DemoStep(
    val stepIndex: Int,
    val title: String,
    val transcript: TranscriptEvent,
    val expectedMinRiskScore: Int,
    val expectedUrgency: String,
    val notes: String
)

object SimulatedCallScenario {
    const val DEMO_CALLER_NUMBER = "+91 98765 43210"
    const val DEMO_CALLER_NAME = "Unknown Number (1600 Series Spoof Claim)"

    val steps = listOf(
        DemoStep(
            stepIndex = 1,
            title = "Unknown Caller Connects",
            transcript = TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "Hello, good afternoon. Am I speaking with the primary savings account holder?",
                isFinal = true
            ),
            expectedMinRiskScore = 5,
            expectedUrgency = "LOW",
            notes = "Baseline unknown caller signal evaluated."
        ),
        DemoStep(
            stepIndex = 2,
            title = "Authority Impersonation",
            transcript = TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "I am calling officially from HDFC Bank Central Security and Fraud Prevention division.",
                isFinal = true
            ),
            expectedMinRiskScore = 15,
            expectedUrgency = "LOW",
            notes = "Caller introduces banking authority claim."
        ),
        DemoStep(
            stepIndex = 3,
            title = "Fake KYC Expiration Claim",
            transcript = TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "Our database indicates your mandatory KYC verification has expired today. Your PAN linking is incomplete.",
                isFinal = true
            ),
            expectedMinRiskScore = 35,
            expectedUrgency = "MEDIUM",
            notes = "Triggers FAKE_KYC signal. Escalates to MEDIUM risk band."
        ),
        DemoStep(
            stepIndex = 4,
            title = "Severe Psychological Urgency & Threat",
            transcript = TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "Your account will be blocked within 15 minutes and all transactions frozen if we do not update your profile right now.",
                isFinal = true
            ),
            expectedMinRiskScore = 65,
            expectedUrgency = "HIGH",
            notes = "Triggers ACCOUNT_SUSPENSION_THREAT and PRESSURE_TACTIC. Escalates to HIGH risk band."
        ),
        DemoStep(
            stepIndex = 5,
            title = "Direct OTP Credential Demand",
            transcript = TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "I have dispatched a 6-digit one-time password OTP to your registered phone. Please share the OTP code immediately to stop the block.",
                isFinal = true
            ),
            expectedMinRiskScore = 85,
            expectedUrgency = "HIGH",
            notes = "Triggers OTP_REQUEST. Triggers CRITICAL risk band & Protective Policy Shield."
        )
    )
}
