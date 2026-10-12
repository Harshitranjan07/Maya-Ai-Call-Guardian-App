package com.maya.callguardian.domain.policies

import com.maya.callguardian.domain.models.*

/**
 * Deterministic Security Policy Engine.
 * Enforces protective rules and user safeguards based strictly on current risk level.
 * CRITICAL RULE: Generated LLM responses can NEVER override or bypass these security policies.
 */
class SecurityPolicyEngine {

    fun evaluatePolicy(
        riskAssessment: RiskAssessment,
        scamAssessment: ScamAssessment,
        conversationObservation: ConversationObservation
    ): SecurityPolicyDecision {
        val level = riskAssessment.level

        return when (level) {
            RiskLevel.LOW -> SecurityPolicyDecision(
                level = RiskLevel.LOW,
                bannerMessage = "Passive call guardian active. No immediate security concerns.",
                allowedActions = listOf("CONTINUE_CALL", "INSPECT_AUDIO", "VIEW_CALLER_INFO"),
                restrictedActions = emptyList(),
                immediateSafetyWarning = null,
                showTakeOverAction = false,
                recommendEndCall = false
            )

            RiskLevel.MEDIUM -> SecurityPolicyDecision(
                level = RiskLevel.MEDIUM,
                bannerMessage = "Caution advised: Unverified caller claims or irregular numbering detected.",
                allowedActions = listOf("CONTINUE_CALL", "INSPECT_AUDIO", "VERIFY_CALLER_ONLINE"),
                restrictedActions = listOf("DO_NOT_SHARE_PERSONAL_DETAILS"),
                immediateSafetyWarning = "Verify caller claims through official phone numbers published on their official website.",
                showTakeOverAction = true,
                recommendEndCall = false
            )

            RiskLevel.HIGH -> SecurityPolicyDecision(
                level = RiskLevel.HIGH,
                bannerMessage = "HIGH RISK: Caller is requesting sensitive actions or exhibiting coercive pressure.",
                allowedActions = listOf("VIEW_SAFETY_ADVICE", "END_CALL", "MUTE_CALL"),
                restrictedActions = listOf(
                    "DO_NOT_TRANSFER_MONEY",
                    "DO_NOT_INSTALL_ANY_APP",
                    "DO_NOT_SCAN_QR_CODES"
                ),
                immediateSafetyWarning = "DO NOT install remote access apps (AnyDesk, TeamViewer) or transfer money under caller guidance.",
                showTakeOverAction = true,
                recommendEndCall = true
            )

            RiskLevel.CRITICAL -> SecurityPolicyDecision(
                level = RiskLevel.CRITICAL,
                bannerMessage = "CRITICAL SCAM THREAT: IMMEDIATE FRAUD IN PROGRESS",
                allowedActions = listOf("IMMEDIATE_TERMINATION", "VIEW_EVIDENCE_LOG", "REPORT_CYBER_FRAUD"),
                restrictedActions = listOf(
                    "NEVER_DISCLOSE_OTP",
                    "NEVER_DISCLOSE_UPI_PIN",
                    "NEVER_DISCLOSE_PASSWORDS",
                    "NEVER_AUTHORIZE_PAYMENT",
                    "NEVER_BELIEVE_DIGITAL_ARREST"
                ),
                immediateSafetyWarning = "CRITICAL: NEVER DISCLOSE YOUR OTP OR BANKING PIN. Banks and Police NEVER ask for OTPs or perform digital arrests over phone calls. Disconnect immediately.",
                showTakeOverAction = true,
                recommendEndCall = true
            )
        }
    }
}
