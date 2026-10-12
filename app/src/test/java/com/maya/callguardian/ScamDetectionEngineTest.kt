package com.maya.callguardian

import com.maya.callguardian.domain.models.*
import com.maya.callguardian.intelligence.scam_detection.ScamDetectionEngine
import org.junit.Assert.*
import org.junit.Test

class ScamDetectionEngineTest {

    private val engine = ScamDetectionEngine()

    @Test
    fun testOtpDetectionEnglish() {
        val transcript = listOf(
            TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "I have sent a 6-digit one-time password OTP code to your mobile. Share it now."
            )
        )
        val result = engine.evaluate(transcript)

        assertTrue(result.detectedSignals.contains(SignalType.OTP_REQUEST))
        assertTrue(result.sensitiveRequestDetected)
        assertEquals(ScamCategory.BANKING_FRAUD, result.category)
        assertTrue(result.evidenceList.isNotEmpty())
    }

    @Test
    fun testOtpDetectionHindi() {
        val transcript = listOf(
            TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "Sir aapke mobile par verification code send kiya hai, jaldi se batao otp."
            )
        )
        val result = engine.evaluate(transcript)

        assertTrue(result.detectedSignals.contains(SignalType.OTP_REQUEST))
        assertTrue(result.sensitiveRequestDetected)
    }

    @Test
    fun testUpiPinDetection() {
        val transcript = listOf(
            TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "To receive your cash refund, open Google Pay and apna UPI PIN daaliye."
            )
        )
        val result = engine.evaluate(transcript)

        assertTrue(result.detectedSignals.contains(SignalType.UPI_PIN_REQUEST))
        assertTrue(result.sensitiveRequestDetected)
        assertEquals(ScamCategory.BANKING_FRAUD, result.category)
    }

    @Test
    fun testDigitalArrestThreat() {
        val transcript = listOf(
            TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "This is Crime Branch Cyber Cell. An illegal narcotics parcel was seized in your name. You are under digital arrest."
            )
        )
        val result = engine.evaluate(transcript)

        assertTrue(result.detectedSignals.contains(SignalType.DIGITAL_ARREST_THREAT))
        assertEquals(ScamCategory.DIGITAL_ARREST, result.category)
    }

    @Test
    fun testRemoteAccessAppDetection() {
        val transcript = listOf(
            TranscriptEvent(
                speaker = Speaker.CALLER,
                text = "Go to play store and install AnyDesk screen share app for verification."
            )
        )
        val result = engine.evaluate(transcript)

        assertTrue(result.detectedSignals.contains(SignalType.REMOTE_ACCESS_APP))
        assertEquals(ScamCategory.REMOTE_ACCESS, result.category)
    }

    @Test
    fun testConversationObservationFeedbackIncorporation() {
        // Transcript has vague speech
        val transcript = listOf(
            TranscriptEvent(speaker = Speaker.CALLER, text = "We need you to complete the identity verification.")
        )
        // But Conversation Agent detected high urgency and authority claims
        val observation = ConversationObservation(
            authorityClaim = true,
            claimedOrganization = "State Bank",
            pressureBehavior = true,
            urgencyLevel = "HIGH",
            requestedAction = "SHARE_OTP",
            sensitiveInformationRequested = true
        )

        val result = engine.evaluate(transcript, observation)

        // The feedback loop incorporates agent signals
        assertTrue(result.detectedSignals.contains(SignalType.GOVERNMENT_IMPERSONATION))
        assertTrue(result.detectedSignals.contains(SignalType.PRESSURE_TACTIC))
        assertTrue(result.detectedSignals.contains(SignalType.OTP_REQUEST))
        assertTrue(result.sensitiveRequestDetected)
    }
}
