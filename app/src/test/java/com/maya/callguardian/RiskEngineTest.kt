package com.maya.callguardian

import com.maya.callguardian.domain.models.*
import com.maya.callguardian.domain.risk.RiskEngine
import org.junit.Assert.*
import org.junit.Test

class RiskEngineTest {

    private val riskEngine = RiskEngine()

    @Test
    fun testLowRiskOnBenignCall() {
        val callerIntel = CallerIntelligenceResult(
            contactStatus = ContactStatus.SAVED,
            numberProvenance = NumberProvenance.VERIFIED,
            signals = emptyList()
        )
        val scamAssessment = ScamAssessment(
            detectedSignals = emptyList(),
            category = ScamCategory.NONE
        )
        val observation = ConversationObservation(
            callerIntent = "FRIENDLY_GREETING",
            urgencyLevel = "LOW"
        )
        val voiceAuthenticity = VoiceAuthenticityResult()

        val risk = riskEngine.calculateRisk(callerIntel, scamAssessment, observation, voiceAuthenticity)

        assertEquals(RiskLevel.LOW, risk.level)
        assertTrue(risk.score in 0..29)
    }

    @Test
    fun testMediumRiskOnFakeKyc() {
        val callerIntel = CallerIntelligenceResult(
            contactStatus = ContactStatus.UNKNOWN,
            numberProvenance = NumberProvenance.UNVERIFIED,
            signals = listOf(SignalType.UNKNOWN_CALLER)
        )
        val scamAssessment = ScamAssessment(
            detectedSignals = listOf(SignalType.FAKE_KYC),
            category = ScamCategory.FAKE_KYC
        )
        val observation = ConversationObservation(
            callerIntent = "SUSPICIOUS_KYC_VERIFICATION",
            urgencyLevel = "MEDIUM"
        )
        val voiceAuthenticity = VoiceAuthenticityResult()

        val risk = riskEngine.calculateRisk(callerIntel, scamAssessment, observation, voiceAuthenticity)

        assertEquals(RiskLevel.MEDIUM, risk.level)
        assertTrue(risk.score in 30..59)
    }

    @Test
    fun testHighRiskOnSuspensionThreatAndPressure() {
        val callerIntel = CallerIntelligenceResult(
            contactStatus = ContactStatus.UNKNOWN,
            signals = listOf(SignalType.UNKNOWN_CALLER)
        )
        val scamAssessment = ScamAssessment(
            detectedSignals = listOf(SignalType.FAKE_KYC, SignalType.ACCOUNT_SUSPENSION_THREAT),
            category = ScamCategory.FAKE_KYC
        )
        val observation = ConversationObservation(
            callerIntent = "COERCIVE_ACCOUNT_THREAT",
            urgencyLevel = "HIGH",
            pressureBehavior = true
        )
        val voiceAuthenticity = VoiceAuthenticityResult()

        val risk = riskEngine.calculateRisk(callerIntel, scamAssessment, observation, voiceAuthenticity)

        assertEquals(RiskLevel.HIGH, risk.level)
        assertTrue(risk.score in 60..79)
    }

    @Test
    fun testCriticalRiskOnDirectOtpDemand() {
        val callerIntel = CallerIntelligenceResult(
            contactStatus = ContactStatus.UNKNOWN,
            signals = listOf(SignalType.UNKNOWN_CALLER)
        )
        val scamAssessment = ScamAssessment(
            detectedSignals = listOf(
                SignalType.FAKE_KYC,
                SignalType.ACCOUNT_SUSPENSION_THREAT,
                SignalType.OTP_REQUEST
            ),
            sensitiveRequestDetected = true,
            category = ScamCategory.BANKING_FRAUD
        )
        val observation = ConversationObservation(
            callerIntent = "CREDENTIAL_EXTRACTION_FRAUD",
            urgencyLevel = "HIGH",
            requestedAction = "SHARE_OTP"
        )
        val voiceAuthenticity = VoiceAuthenticityResult()

        val risk = riskEngine.calculateRisk(callerIntel, scamAssessment, observation, voiceAuthenticity)

        assertEquals(RiskLevel.CRITICAL, risk.level)
        assertTrue(risk.score >= 80)
        assertTrue(risk.significantExplanations.any { it.contains("CRITICAL") || it.contains("OTP") })
    }

    @Test
    fun testDuplicateSignalsAreNotDoubleCounted() {
        val callerIntel = CallerIntelligenceResult(
            signals = listOf(SignalType.UNKNOWN_CALLER)
        )
        // Pass duplicate signals
        val scamAssessment = ScamAssessment(
            detectedSignals = listOf(SignalType.FAKE_KYC, SignalType.FAKE_KYC, SignalType.FAKE_KYC)
        )
        val observation = ConversationObservation(urgencyLevel = "LOW")
        val voiceAuthenticity = VoiceAuthenticityResult()

        val risk = riskEngine.calculateRisk(callerIntel, scamAssessment, observation, voiceAuthenticity)

        // Signal contribution for FAKE_KYC should appear only once
        val fkycContributions = risk.breakdown.signalContributions.filterKeys { it == SignalType.FAKE_KYC }
        assertEquals(1, fkycContributions.size)
    }
}
