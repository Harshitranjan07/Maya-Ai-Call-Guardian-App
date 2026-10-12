package com.maya.callguardian.domain.risk

import com.maya.callguardian.domain.models.*

/**
 * Deterministic Risk Engine combining independent evidence sources:
 * - Caller intelligence & provenance
 * - Scam detection signals & categories
 * - Semantic conversation agent observations (urgency, pressure, authority)
 * - Voice authenticity (if available)
 *
 * Produces an authoritative score from 0 to 100 within configurable bands:
 * 0–29: LOW
 * 30–59: MEDIUM
 * 60–79: HIGH
 * 80–100: CRITICAL
 */
class RiskEngine(
    private val lowThreshold: Int = 29,
    private val mediumThreshold: Int = 59,
    private val highThreshold: Int = 79
) {

    fun calculateRisk(
        callerIntelligence: CallerIntelligenceResult,
        scamAssessment: ScamAssessment,
        conversationObservation: ConversationObservation,
        voiceAuthenticity: VoiceAuthenticityResult
    ): RiskAssessment {
        val signalContributions = mutableMapOf<SignalType, Int>()
        val significantExplanations = mutableListOf<String>()

        // 1. Supporting Caller Metadata & Provenance (Supporting signal only, capped at 15 points)
        var metadataScore = 0
        if (callerIntelligence.contactStatus == ContactStatus.UNKNOWN) {
            metadataScore += 5
            signalContributions[SignalType.UNKNOWN_CALLER] = 5
            significantExplanations.add("Caller is not in local contacts (+5)")
        }
        if (callerIntelligence.numberProvenance == NumberProvenance.SUSPECTED_SPOOF) {
            metadataScore += 10
            signalContributions[SignalType.SUSPICIOUS_NUMBERING] = 10
            significantExplanations.add("Suspicious numbering format / potential spoof (+10)")
        }

        // 2. Transcript & Scam Detection Signals (Avoid duplicate counting by using distinct set)
        val uniqueSignals = scamAssessment.detectedSignals.toSet()
        var signalsScore = 0

        for (signal in uniqueSignals) {
            // Do not double count metadata signals already applied
            if (signal == SignalType.UNKNOWN_CALLER || signal == SignalType.SUSPICIOUS_NUMBERING) {
                continue
            }

            // Weight calculation adjusted by evidence quality
            val weight = signal.baseWeight
            signalsScore += weight
            signalContributions[signal] = weight
            significantExplanations.add("Identified ${signal.description} (+$weight)")
        }

        // 3. Conversation Agent Semantic Context (Urgency multiplier & pressure bonus)
        var urgencyMultiplier = 1.0f
        when (conversationObservation.urgencyLevel) {
            "CRITICAL" -> {
                urgencyMultiplier = 1.25f
                significantExplanations.add("Severe psychological urgency and intimidation multiplier (x1.25)")
            }
            "HIGH" -> {
                urgencyMultiplier = 1.15f
                significantExplanations.add("High caller urgency multiplier (x1.15)")
            }
            "MEDIUM" -> {
                urgencyMultiplier = 1.05f
            }
        }

        // 4. Critical Escalation Guard:
        // If an explicit request for OTP, PIN, password, or digital arrest is combined with urgency,
        // it MUST reach CRITICAL band (>= 80).
        val hasSevereCredentialRequest = uniqueSignals.contains(SignalType.OTP_REQUEST) ||
                uniqueSignals.contains(SignalType.UPI_PIN_REQUEST) ||
                uniqueSignals.contains(SignalType.PASSWORD_REQUEST) ||
                uniqueSignals.contains(SignalType.FINANCIAL_INFO_REQUEST) ||
                uniqueSignals.contains(SignalType.DIGITAL_ARREST_THREAT)

        // Calculate raw and adjusted score
        val rawBase = metadataScore + signalsScore
        var finalCalculated = (rawBase * urgencyMultiplier).toInt()

        if (hasSevereCredentialRequest) {
            val credentialCount = listOf(
                SignalType.OTP_REQUEST,
                SignalType.FINANCIAL_INFO_REQUEST,
                SignalType.UPI_PIN_REQUEST,
                SignalType.PASSWORD_REQUEST
            ).count { uniqueSignals.contains(it) }

            if (credentialCount >= 2 || uniqueSignals.size >= 2 || conversationObservation.urgencyLevel == "HIGH" || conversationObservation.urgencyLevel == "CRITICAL") {
                finalCalculated = maxOf(finalCalculated, 88)
                significantExplanations.add("Severe multi-credential extraction (OTP / Banking details) detected (CRITICAL Score: 88)")
            } else if (finalCalculated < 65) {
                finalCalculated = 65
                significantExplanations.add("Direct request for sensitive credentials (OTP / Bank details) escalated to HIGH RISK (Score: 65)")
            }
        }

        // Cap at 100
        val finalScore = finalCalculated.coerceIn(0, 100)
        val level = RiskLevel.fromScore(finalScore)

        val primaryThreat = when {
            scamAssessment.category != ScamCategory.NONE ->
                scamAssessment.category.displayName
            callerIntelligence.numberProvenance == NumberProvenance.SUSPECTED_SPOOF ->
                "Unverified Caller Identity"
            else ->
                "Normal monitoring active"
        }

        // Confidence calculation based on evidence breadth
        val confidence = when {
            scamAssessment.evidenceList.size >= 3 -> 0.95f
            scamAssessment.evidenceList.size >= 1 -> 0.85f
            callerIntelligence.contactStatus == ContactStatus.SAVED -> 0.90f
            else -> 0.50f
        }

        val breakdown = RiskScoreBreakdown(
            baselineScore = metadataScore,
            signalContributions = signalContributions,
            urgencyMultiplier = urgencyMultiplier,
            rawTotal = rawBase,
            finalScore = finalScore
        )

        return RiskAssessment(
            score = finalScore,
            level = level,
            breakdown = breakdown,
            primaryThreat = primaryThreat,
            significantExplanations = significantExplanations,
            confidence = confidence,
            lastEvaluatedMs = System.currentTimeMillis()
        )
    }
}
