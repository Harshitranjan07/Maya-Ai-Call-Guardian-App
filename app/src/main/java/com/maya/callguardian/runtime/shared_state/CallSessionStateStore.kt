package com.maya.callguardian.runtime.shared_state

import com.maya.callguardian.domain.events.SessionEvent
import com.maya.callguardian.domain.models.*
import com.maya.callguardian.domain.policies.SecurityPolicyEngine
import com.maya.callguardian.domain.risk.RiskEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Authoritative single-source-of-truth state store for live call security.
 * Enforces atomic state transitions, sequence tracking to prevent stale AI updates,
 * and automatic synchronization between RiskEngine, SecurityPolicyEngine, and UI.
 */
class CallSessionStateStore(
    private val riskEngine: RiskEngine = RiskEngine(),
    private val securityPolicyEngine: SecurityPolicyEngine = SecurityPolicyEngine()
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(CallSessionState())
    val state: StateFlow<CallSessionState> = _state.asStateFlow()

    private var currentSequenceNumber: Long = 0L

    /**
     * Atomically process a typed session event.
     */
    suspend fun reduce(event: SessionEvent) = mutex.withLock {
        val current = _state.value
        val newState = when (event) {
            is SessionEvent.CallStarted -> {
                currentSequenceNumber = 0L
                val baseCallerIntel = current.callerIntelligence
                val baseScam = ScamAssessment()
                val baseObs = ConversationObservation()
                val baseVoice = current.voiceAuthenticity
                val risk = riskEngine.calculateRisk(baseCallerIntel, baseScam, baseObs, baseVoice)
                val policy = securityPolicyEngine.evaluatePolicy(risk, baseScam, baseObs)

                current.copy(
                    sessionId = event.sessionId,
                    callState = CallState.ACTIVE,
                    operationMode = event.mode,
                    callerInfo = event.callerInfo,
                    transcriptEvents = emptyList(),
                    scamAssessment = baseScam,
                    conversationObservation = baseObs,
                    riskAssessment = risk,
                    securityPolicy = policy,
                    agentExecutionState = AgentExecutionState(lastUpdatedMs = System.currentTimeMillis()),
                    isUserIntervened = false,
                    callStartTimeMs = event.startTimeMs,
                    callEndTimeMs = 0L
                )
            }

            is SessionEvent.CallEnded -> {
                current.copy(
                    callState = CallState.DISCONNECTED,
                    callEndTimeMs = event.endTimeMs,
                    agentExecutionState = current.agentExecutionState.copy(
                        isScamDetectorRunning = false,
                        isConversationAgentRunning = false,
                        isTranscribing = false
                    )
                )
            }

            is SessionEvent.CallerIntelligenceEvaluated -> {
                val risk = riskEngine.calculateRisk(
                    event.result,
                    current.scamAssessment,
                    current.conversationObservation,
                    current.voiceAuthenticity
                )
                val policy = securityPolicyEngine.evaluatePolicy(risk, current.scamAssessment, current.conversationObservation)
                current.copy(
                    callerIntelligence = event.result,
                    riskAssessment = risk,
                    securityPolicy = policy
                )
            }

            is SessionEvent.TranscriptChunkReceived -> {
                val updatedTranscripts = current.transcriptEvents + event.transcript
                current.copy(transcriptEvents = updatedTranscripts)
            }

            is SessionEvent.ScamAssessmentUpdated -> {
                // Reject stale AI responses if an older sequence number arrives
                if (event.sequenceNumber < current.agentExecutionState.updateSequenceNumber) {
                    current // Reject stale result
                } else {
                    currentSequenceNumber = maxOf(currentSequenceNumber, event.sequenceNumber)
                    val risk = riskEngine.calculateRisk(
                        current.callerIntelligence,
                        event.assessment,
                        current.conversationObservation,
                        current.voiceAuthenticity
                    )
                    val policy = securityPolicyEngine.evaluatePolicy(risk, event.assessment, current.conversationObservation)

                    current.copy(
                        scamAssessment = event.assessment,
                        riskAssessment = risk,
                        securityPolicy = policy,
                        agentExecutionState = current.agentExecutionState.copy(
                            isScamDetectorRunning = false,
                            updateSequenceNumber = currentSequenceNumber,
                            lastUpdatedMs = System.currentTimeMillis()
                        )
                    )
                }
            }

            is SessionEvent.ConversationObservationProduced -> {
                if (event.sequenceNumber < current.agentExecutionState.updateSequenceNumber) {
                    current // Reject stale result
                } else {
                    currentSequenceNumber = maxOf(currentSequenceNumber, event.sequenceNumber)
                    val risk = riskEngine.calculateRisk(
                        current.callerIntelligence,
                        current.scamAssessment,
                        event.observation,
                        current.voiceAuthenticity
                    )
                    val policy = securityPolicyEngine.evaluatePolicy(risk, current.scamAssessment, event.observation)

                    current.copy(
                        conversationObservation = event.observation,
                        riskAssessment = risk,
                        securityPolicy = policy,
                        agentExecutionState = current.agentExecutionState.copy(
                            isConversationAgentRunning = false,
                            updateSequenceNumber = currentSequenceNumber,
                            lastUpdatedMs = System.currentTimeMillis()
                        )
                    )
                }
            }

            is SessionEvent.VoiceAuthenticityEvaluated -> {
                val risk = riskEngine.calculateRisk(
                    current.callerIntelligence,
                    current.scamAssessment,
                    current.conversationObservation,
                    event.result
                )
                val policy = securityPolicyEngine.evaluatePolicy(risk, current.scamAssessment, current.conversationObservation)
                current.copy(
                    voiceAuthenticity = event.result,
                    riskAssessment = risk,
                    securityPolicy = policy
                )
            }

            is SessionEvent.RiskEvaluated -> {
                val policy = securityPolicyEngine.evaluatePolicy(event.assessment, current.scamAssessment, current.conversationObservation)
                current.copy(riskAssessment = event.assessment, securityPolicy = policy)
            }

            is SessionEvent.SecurityPolicyEvaluated -> {
                current.copy(securityPolicy = event.policy)
            }

            is SessionEvent.AgentStatusChanged -> {
                current.copy(
                    agentExecutionState = current.agentExecutionState.copy(
                        isScamDetectorRunning = event.isScamDetectorRunning ?: current.agentExecutionState.isScamDetectorRunning,
                        isConversationAgentRunning = event.isConversationAgentRunning ?: current.agentExecutionState.isConversationAgentRunning,
                        isTranscribing = event.isTranscribing ?: current.agentExecutionState.isTranscribing
                    )
                )
            }

            is SessionEvent.UserIntervened -> {
                current.copy(isUserIntervened = true)
            }

            is SessionEvent.ResetSession -> {
                currentSequenceNumber = 0L
                CallSessionState()
            }
        }

        _state.value = newState
    }

    fun nextSequenceNumber(): Long {
        return ++currentSequenceNumber
    }
}
