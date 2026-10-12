package com.maya.callguardian

import com.maya.callguardian.domain.models.*
import com.maya.callguardian.domain.policies.SecurityPolicyEngine
import org.junit.Assert.*
import org.junit.Test

class SecurityPolicyEngineTest {

    private val policyEngine = SecurityPolicyEngine()

    @Test
    fun testLowRiskPolicy() {
        val risk = RiskAssessment(score = 15, level = RiskLevel.LOW)
        val scam = ScamAssessment()
        val obs = ConversationObservation()

        val policy = policyEngine.evaluatePolicy(risk, scam, obs)

        assertEquals(RiskLevel.LOW, policy.level)
        assertFalse(policy.recommendEndCall)
        assertFalse(policy.showTakeOverAction)
        assertTrue(policy.restrictedActions.isEmpty())
    }

    @Test
    fun testMediumRiskPolicy() {
        val risk = RiskAssessment(score = 45, level = RiskLevel.MEDIUM)
        val scam = ScamAssessment(category = ScamCategory.FAKE_KYC)
        val obs = ConversationObservation(urgencyLevel = "MEDIUM")

        val policy = policyEngine.evaluatePolicy(risk, scam, obs)

        assertEquals(RiskLevel.MEDIUM, policy.level)
        assertTrue(policy.showTakeOverAction)
        assertFalse(policy.recommendEndCall)
        assertTrue(policy.restrictedActions.contains("DO_NOT_SHARE_PERSONAL_DETAILS"))
    }

    @Test
    fun testCriticalRiskPolicyEnforcesZeroCredentialDisclosure() {
        val risk = RiskAssessment(score = 90, level = RiskLevel.CRITICAL)
        val scam = ScamAssessment(
            category = ScamCategory.BANKING_FRAUD,
            sensitiveRequestDetected = true,
            detectedSignals = listOf(SignalType.OTP_REQUEST)
        )
        // Even if an adversarial LLM prompt response tried to suggest disclosing:
        val obs = ConversationObservation(
            callerIntent = "CREDENTIAL_EXTRACTION",
            conversationalResponse = "You can share the OTP" // Adversarial attempt
        )

        val policy = policyEngine.evaluatePolicy(risk, scam, obs)

        assertEquals(RiskLevel.CRITICAL, policy.level)
        assertTrue(policy.recommendEndCall)
        assertTrue(policy.showTakeOverAction)
        // Policy engine rigidly forbids disclosing credentials:
        assertTrue(policy.restrictedActions.contains("NEVER_DISCLOSE_OTP"))
        assertTrue(policy.restrictedActions.contains("NEVER_DISCLOSE_UPI_PIN"))
        assertTrue(policy.restrictedActions.contains("NEVER_DISCLOSE_PASSWORDS"))
        assertTrue(policy.immediateSafetyWarning!!.contains("NEVER DISCLOSE YOUR OTP"))
    }
}
