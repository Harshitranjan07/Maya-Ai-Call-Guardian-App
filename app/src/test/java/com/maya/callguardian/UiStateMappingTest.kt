package com.maya.callguardian

import com.maya.callguardian.domain.models.*
import org.junit.Assert.*
import org.junit.Test

class UiStateMappingTest {

    @Test
    fun testRiskLevelPresentationLabels() {
        assertEquals("LOW RISK", RiskLevel.LOW.label)
        assertEquals("SUSPICIOUS", RiskLevel.MEDIUM.label)
        assertEquals("HIGH RISK", RiskLevel.HIGH.label)
        assertEquals("CRITICAL SCAM DETECTED", RiskLevel.CRITICAL.label)
    }

    @Test
    fun testRiskLevelScoreBands() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(29))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(30))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(59))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(79))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(80))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(150)) // Clamped
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(-10)) // Clamped
    }

    @Test
    fun testScamCategoryDisplayNames() {
        assertEquals("Banking & Financial Fraud", ScamCategory.BANKING_FRAUD.displayName)
        assertEquals("Digital Arrest / Law Enforcement Impersonation", ScamCategory.DIGITAL_ARREST.displayName)
        assertEquals("Fake KYC / Account Suspension", ScamCategory.FAKE_KYC.displayName)
        assertEquals("Remote Access Tool Coercion", ScamCategory.REMOTE_ACCESS.displayName)
    }

    @Test
    fun testTranscriptEventFormatting() {
        val event = TranscriptEvent(
            speaker = Speaker.CALLER,
            text = "Please share the OTP",
            isFinal = true
        )
        assertEquals(Speaker.CALLER, event.speaker)
        assertEquals("Please share the OTP", event.text)
        assertTrue(event.isFinal)
    }
}
