package com.maya.callguardian

import com.maya.callguardian.domain.events.SessionEvent
import com.maya.callguardian.domain.models.*
import com.maya.callguardian.runtime.shared_state.CallSessionStateStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ContinuousFeedbackLoopTest {

    private val stateStore = CallSessionStateStore()

    @Test
    fun testInitialCallStart() = runBlocking {
        val caller = CallerInfo(phoneNumber = "+91 99999 00000", displayName = "Test Caller")
        stateStore.reduce(
            SessionEvent.CallStarted(
                sessionId = "test_sess_1",
                mode = OperationMode.SIMULATED_DEMO,
                callerInfo = caller
            )
        )

        val state = stateStore.state.value
        assertEquals(CallState.ACTIVE, state.callState)
        assertEquals("test_sess_1", state.sessionId)
        assertEquals(RiskLevel.LOW, state.riskAssessment.level)
    }

    @Test
    fun testStaleAiResultRejection() = runBlocking {
        // Start call
        stateStore.reduce(
            SessionEvent.CallStarted("sess_stale", OperationMode.SIMULATED_DEMO, CallerInfo())
        )

        // Modern update with sequence 5
        val newerAssessment = ScamAssessment(
            scamProbability = 0.85f,
            category = ScamCategory.BANKING_FRAUD,
            detectedSignals = listOf(SignalType.OTP_REQUEST, SignalType.ACCOUNT_SUSPENSION_THREAT),
            sensitiveRequestDetected = true
        )
        stateStore.reduce(
            SessionEvent.ScamAssessmentUpdated(newerAssessment, sequenceNumber = 5L)
        )

        val expectedScore = stateStore.state.value.riskAssessment.score
        assertTrue(expectedScore >= 80)
        assertEquals(5L, stateStore.state.value.agentExecutionState.updateSequenceNumber)

        // Now an older delayed result with sequence 2 arrives
        val olderStaleAssessment = ScamAssessment(
            scamProbability = 0.10f,
            category = ScamCategory.NONE,
            detectedSignals = emptyList()
        )
        stateStore.reduce(
            SessionEvent.ScamAssessmentUpdated(olderStaleAssessment, sequenceNumber = 2L)
        )

        // The older stale result MUST NOT overwrite the newer security decision!
        assertEquals(expectedScore, stateStore.state.value.riskAssessment.score)
        assertEquals(ScamCategory.BANKING_FRAUD, stateStore.state.value.scamAssessment.category)
        assertEquals(5L, stateStore.state.value.agentExecutionState.updateSequenceNumber)
    }

    @Test
    fun testStateStoreAutomaticPolicySync() = runBlocking {
        stateStore.reduce(
            SessionEvent.CallStarted("sess_sync", OperationMode.SIMULATED_DEMO, CallerInfo())
        )

        // When critical scam assessment is reduced:
        val criticalAssessment = ScamAssessment(
            scamProbability = 0.95f,
            category = ScamCategory.BANKING_FRAUD,
            detectedSignals = listOf(SignalType.OTP_REQUEST, SignalType.ACCOUNT_SUSPENSION_THREAT),
            sensitiveRequestDetected = true
        )
        stateStore.reduce(
            SessionEvent.ScamAssessmentUpdated(criticalAssessment, sequenceNumber = 1L)
        )

        val state = stateStore.state.value
        assertEquals(RiskLevel.CRITICAL, state.riskAssessment.level)
        assertEquals(RiskLevel.CRITICAL, state.securityPolicy.level)
        assertTrue(state.securityPolicy.recommendEndCall)
        assertTrue(state.securityPolicy.restrictedActions.contains("NEVER_DISCLOSE_OTP"))
    }
}
