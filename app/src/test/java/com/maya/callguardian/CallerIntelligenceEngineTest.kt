package com.maya.callguardian

import com.maya.callguardian.domain.models.ContactStatus
import com.maya.callguardian.domain.models.NumberProvenance
import com.maya.callguardian.intelligence.caller_intelligence.CallerIntelligenceEngine
import org.junit.Assert.*
import org.junit.Test

class CallerIntelligenceEngineTest {

    private val engine = CallerIntelligenceEngine(
        localBlocklist = setOf("+919999988888"),
        localAllowlist = setOf("+911234512345")
    )

    @Test
    fun test1600SeriesIsNotAutomaticallySafeOrAuthenticated() {
        val result = engine.analyze(
            rawNumber = "+91 1600 233 456",
            isSavedContact = false
        )

        // Must NOT be verified simply because it starts with 1600!
        assertNotEquals(NumberProvenance.VERIFIED, result.numberProvenance)
        assertEquals(ContactStatus.UNKNOWN, result.contactStatus)
        assertTrue(result.notes.contains("not inherently safe or authenticated"))
    }

    @Test
    fun test140SeriesIdentifiedAsTelemarketing() {
        val result = engine.analyze(
            rawNumber = "+91 140 123 4567",
            isSavedContact = false
        )

        assertTrue(result.carrierOrSeriesInfo.contains("140"))
    }

    @Test
    fun testSavedContactIsVerified() {
        val result = engine.analyze(
            rawNumber = "+91 98765 43210",
            isSavedContact = true,
            contactName = "Mom"
        )

        assertEquals(ContactStatus.SAVED, result.contactStatus)
        assertEquals(NumberProvenance.VERIFIED, result.numberProvenance)
    }

    @Test
    fun testBlockedNumberIsBlocked() {
        val result = engine.analyze(
            rawNumber = "+91 99999 88888",
            isSavedContact = false
        )

        assertEquals(ContactStatus.BLOCKED, result.contactStatus)
        assertEquals(NumberProvenance.SUSPECTED_SPOOF, result.numberProvenance)
    }
}
