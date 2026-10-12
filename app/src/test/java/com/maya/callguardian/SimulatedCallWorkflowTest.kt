package com.maya.callguardian

import com.maya.callguardian.domain.models.*
import com.maya.callguardian.runtime.session_manager.SessionManager
import com.maya.callguardian.testing.simulated_calls.SimulatedCallScenario
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SimulatedCallWorkflowTest {

    @Test
    fun testEndToEndSimulatedCallProgression() = runBlocking {
        val sessionManager = SessionManager()

        // 1. Start simulation
        sessionManager.startSimulationInternal()
        val initial = sessionManager.sessionState.value
        assertEquals(CallState.ACTIVE, initial.callState)
        assertEquals(OperationMode.SIMULATED_DEMO, initial.operationMode)
        assertEquals(RiskLevel.LOW, initial.riskAssessment.level)

        // Step 1: Unknown caller greeting
        sessionManager.processTranscriptEvent(SimulatedCallScenario.steps[0].transcript)
        var state = sessionManager.sessionState.value
        assertEquals(RiskLevel.LOW, state.riskAssessment.level)

        // Step 2: Authority claim (HDFC Bank)
        sessionManager.processTranscriptEvent(SimulatedCallScenario.steps[1].transcript)
        state = sessionManager.sessionState.value
        assertTrue(state.conversationObservation.authorityClaim)

        // Step 3: Fake KYC expiration claim
        sessionManager.processTranscriptEvent(SimulatedCallScenario.steps[2].transcript)
        state = sessionManager.sessionState.value
        assertTrue(state.riskAssessment.score >= 30)
        assertTrue(state.scamAssessment.detectedSignals.contains(SignalType.FAKE_KYC))

        // Step 4: Account suspension threat & urgency
        sessionManager.processTranscriptEvent(SimulatedCallScenario.steps[3].transcript)
        state = sessionManager.sessionState.value
        assertTrue(state.riskAssessment.score >= 60)
        assertTrue(state.scamAssessment.detectedSignals.contains(SignalType.ACCOUNT_SUSPENSION_THREAT))
        assertEquals("HIGH", state.conversationObservation.urgencyLevel)

        // Step 5: Direct OTP request
        sessionManager.processTranscriptEvent(SimulatedCallScenario.steps[4].transcript)
        state = sessionManager.sessionState.value
        assertEquals(RiskLevel.CRITICAL, state.riskAssessment.level)
        assertTrue(state.riskAssessment.score >= 80)
        assertTrue(state.scamAssessment.sensitiveRequestDetected)
        assertTrue(state.scamAssessment.detectedSignals.contains(SignalType.OTP_REQUEST))

        // Policy engine must be in CRITICAL mode with protective shield
        assertEquals(RiskLevel.CRITICAL, state.securityPolicy.level)
        assertTrue(state.securityPolicy.recommendEndCall)
        assertTrue(state.securityPolicy.immediateSafetyWarning!!.contains("NEVER DISCLOSE YOUR OTP"))

        // End call
        sessionManager.endCallSync()
        assertEquals(CallState.DISCONNECTED, sessionManager.sessionState.value.callState)
    }
}
