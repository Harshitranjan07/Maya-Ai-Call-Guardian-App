package com.maya.callguardian.domain.events

import com.maya.callguardian.domain.models.*

/**
 * Typed events for communicating between concurrent AI agents,
 * call listeners, and the shared state store.
 */
sealed interface SessionEvent {
    data class CallStarted(
        val sessionId: String,
        val mode: OperationMode,
        val callerInfo: CallerInfo,
        val startTimeMs: Long = System.currentTimeMillis()
    ) : SessionEvent

    data class CallEnded(
        val endTimeMs: Long = System.currentTimeMillis()
    ) : SessionEvent

    data class CallerIntelligenceEvaluated(
        val result: CallerIntelligenceResult
    ) : SessionEvent

    data class TranscriptChunkReceived(
        val transcript: TranscriptEvent
    ) : SessionEvent

    data class ScamAssessmentUpdated(
        val assessment: ScamAssessment,
        val sequenceNumber: Long
    ) : SessionEvent

    data class ConversationObservationProduced(
        val observation: ConversationObservation,
        val sequenceNumber: Long
    ) : SessionEvent

    data class VoiceAuthenticityEvaluated(
        val result: VoiceAuthenticityResult
    ) : SessionEvent

    data class RiskEvaluated(
        val assessment: RiskAssessment
    ) : SessionEvent

    data class SecurityPolicyEvaluated(
        val policy: SecurityPolicyDecision
    ) : SessionEvent

    data class AgentStatusChanged(
        val isScamDetectorRunning: Boolean? = null,
        val isConversationAgentRunning: Boolean? = null,
        val isTranscribing: Boolean? = null
    ) : SessionEvent

    data class UserIntervened(
        val action: String
    ) : SessionEvent

    object ResetSession : SessionEvent
}
