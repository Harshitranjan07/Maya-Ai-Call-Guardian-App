package com.maya.callguardian.domain.models

/**
 * Caller details and status.
 */
data class CallerInfo(
    val phoneNumber: String = "",
    val displayName: String = "Unknown Caller",
    val contactStatus: ContactStatus = ContactStatus.UNKNOWN,
    val countryCode: String = "+91",
    val formattedNumber: String = ""
)

/**
 * Output of CallerIntelligenceEngine.
 */
data class CallerIntelligenceResult(
    val contactStatus: ContactStatus = ContactStatus.UNKNOWN,
    val numberProvenance: NumberProvenance = NumberProvenance.UNVERIFIED,
    val signals: List<SignalType> = emptyList(),
    val confidence: Float = 0.5f,
    val carrierOrSeriesInfo: String = "",
    val notes: String = ""
)

/**
 * Single chunk of speech-to-text transcript.
 */
data class TranscriptEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: Speaker = Speaker.CALLER,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isFinal: Boolean = true,
    val confidence: Float = 1.0f
)

/**
 * Specific piece of evidence supporting a detected signal.
 */
data class EvidenceItem(
    val signal: SignalType,
    val source: String, // "TRANSCRIPT", "CALLER_ID", "SEMANTIC_ANALYSIS", "AUDIO_FEATURES"
    val timestampMs: Long,
    val excerpt: String,
    val confidence: Float = 1.0f
)

/**
 * Output of the ScamDetectionEngine.
 */
data class ScamAssessment(
    val scamProbability: Float = 0.0f, // 0.0 - 1.0 (calibrated or heuristic)
    val isHeuristicScore: Boolean = true, // True if based purely on rule heuristics
    val category: ScamCategory = ScamCategory.NONE,
    val detectedSignals: List<SignalType> = emptyList(),
    val sensitiveRequestDetected: Boolean = false,
    val evidenceList: List<EvidenceItem> = emptyList(),
    val explanation: String = "No indicators detected."
)

/**
 * Structured observation produced by ConversationAgent (Gemma or fallback).
 */
data class ConversationObservation(
    val callerIntent: String = "UNDETERMINED",
    val authorityClaim: Boolean = false,
    val claimedOrganization: String = "",
    val urgencyLevel: String = "LOW", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val pressureBehavior: Boolean = false,
    val requestedAction: String = "NONE",
    val sensitiveInformationRequested: Boolean = false,
    val recommendedNextStep: String = "Continue monitoring",
    val userFacingWarning: String = "",
    val conversationalResponse: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Output of VoiceAuthenticityEngine.
 */
data class VoiceAuthenticityResult(
    val syntheticVoiceIndicatorScore: Float = 0.0f, // 0.0 - 1.0
    val confidence: Float = 0.0f,
    val status: VoiceAuthenticityStatus = VoiceAuthenticityStatus.UNAVAILABLE,
    val supportingEvidence: String = "Voice authenticity analysis unavailable",
    val isModelAvailable: Boolean = false
)

/**
 * Detailed breakdown of the deterministic risk calculation.
 */
data class RiskScoreBreakdown(
    val baselineScore: Int = 0,
    val signalContributions: Map<SignalType, Int> = emptyMap(),
    val urgencyMultiplier: Float = 1.0f,
    val rawTotal: Int = 0,
    val finalScore: Int = 0
)

/**
 * Authoritative risk assessment produced by RiskEngine.
 */
data class RiskAssessment(
    val score: Int = 0, // 0 - 100
    val level: RiskLevel = RiskLevel.LOW,
    val breakdown: RiskScoreBreakdown = RiskScoreBreakdown(),
    val primaryThreat: String = "Normal monitoring",
    val significantExplanations: List<String> = emptyList(),
    val confidence: Float = 0.5f,
    val lastEvaluatedMs: Long = System.currentTimeMillis()
)

/**
 * Deterministic policy rules produced by SecurityPolicyEngine.
 * LLM CAN NEVER OVERRIDE THESE ACTIONS.
 */
data class SecurityPolicyDecision(
    val level: RiskLevel = RiskLevel.LOW,
    val bannerMessage: String = "Passive protection active",
    val allowedActions: List<String> = listOf("CONTINUE_CALL", "INSPECT_AUDIO"),
    val restrictedActions: List<String> = emptyList(),
    val immediateSafetyWarning: String? = null,
    val showTakeOverAction: Boolean = false,
    val recommendEndCall: Boolean = false
)

/**
 * Status of concurrent agents.
 */
data class AgentExecutionState(
    val isScamDetectorRunning: Boolean = false,
    val isConversationAgentRunning: Boolean = false,
    val isTranscribing: Boolean = false,
    val pendingObservationFeedback: Boolean = false,
    val updateSequenceNumber: Long = 0L,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

/**
 * Central domain model representing the current call session.
 * Immutable state reduced exclusively through CallSessionStateStore.
 */
data class CallSessionState(
    val sessionId: String = java.util.UUID.randomUUID().toString(),
    val callState: CallState = CallState.IDLE,
    val operationMode: OperationMode = OperationMode.SIMULATED_DEMO,
    val callerInfo: CallerInfo = CallerInfo(),
    val callerIntelligence: CallerIntelligenceResult = CallerIntelligenceResult(),
    val transcriptEvents: List<TranscriptEvent> = emptyList(),
    val scamAssessment: ScamAssessment = ScamAssessment(),
    val conversationObservation: ConversationObservation = ConversationObservation(),
    val voiceAuthenticity: VoiceAuthenticityResult = VoiceAuthenticityResult(),
    val riskAssessment: RiskAssessment = RiskAssessment(),
    val securityPolicy: SecurityPolicyDecision = SecurityPolicyDecision(),
    val agentExecutionState: AgentExecutionState = AgentExecutionState(),
    val isUserIntervened: Boolean = false,
    val callStartTimeMs: Long = 0L,
    val callEndTimeMs: Long = 0L
) {
    val durationSeconds: Long
        get() {
            return if (callStartTimeMs == 0L) 0L
            else {
                val end = if (callEndTimeMs == 0L) System.currentTimeMillis() else callEndTimeMs
                ((end - callStartTimeMs) / 1000).coerceAtLeast(0L)
            }
        }
}
