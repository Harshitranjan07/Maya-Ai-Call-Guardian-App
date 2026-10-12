package com.maya.callguardian.intelligence.caller_intelligence

import com.maya.callguardian.domain.models.*

/**
 * Caller identity and number intelligence engine.
 * Treats numbers as contextual signals, not absolute proof.
 * Explicitly understands Indian numbering plans without falsely presuming
 * 1600 or any other series is verified or authenticated.
 */
class CallerIntelligenceEngine(
    private val localBlocklist: Set<String> = emptySet(),
    private val localAllowlist: Set<String> = emptySet()
) {

    fun analyze(
        rawNumber: String,
        isSavedContact: Boolean,
        contactName: String? = null
    ): CallerIntelligenceResult {
        val sanitized = sanitizePhoneNumber(rawNumber)
        val signals = mutableListOf<SignalType>()

        // 1. Contact list evaluation
        val contactStatus = when {
            isSavedContact -> ContactStatus.SAVED
            localBlocklist.contains(sanitized) -> ContactStatus.BLOCKED
            else -> {
                signals.add(SignalType.UNKNOWN_CALLER)
                ContactStatus.UNKNOWN
            }
        }

        // 2. Allowlist / Blocklist checks
        if (localAllowlist.contains(sanitized)) {
            return CallerIntelligenceResult(
                contactStatus = ContactStatus.SAVED,
                numberProvenance = NumberProvenance.VERIFIED,
                signals = emptyList(),
                confidence = 0.9f,
                carrierOrSeriesInfo = "User Allowlist",
                notes = "Explicitly trusted by user allowlist"
            )
        }

        if (localBlocklist.contains(sanitized)) {
            signals.add(SignalType.SUSPICIOUS_NUMBERING)
            return CallerIntelligenceResult(
                contactStatus = ContactStatus.BLOCKED,
                numberProvenance = NumberProvenance.SUSPECTED_SPOOF,
                signals = signals,
                confidence = 0.95f,
                carrierOrSeriesInfo = "User Blocklist",
                notes = "Matched explicit user blocklist entry"
            )
        }

        // 3. Indian numbering analysis
        val (seriesInfo, isSuspiciousFormat) = evaluateIndianNumbering(sanitized)
        if (isSuspiciousFormat) {
            signals.add(SignalType.SUSPICIOUS_NUMBERING)
        }

        // 4. Provenance evaluation
        val provenance = when {
            isSavedContact -> NumberProvenance.VERIFIED
            isSuspiciousFormat -> NumberProvenance.SUSPECTED_SPOOF
            else -> NumberProvenance.UNVERIFIED
        }

        val confidence = when {
            isSavedContact -> 0.85f
            isSuspiciousFormat -> 0.70f
            else -> 0.40f
        }

        return CallerIntelligenceResult(
            contactStatus = contactStatus,
            numberProvenance = provenance,
            signals = signals,
            confidence = confidence,
            carrierOrSeriesInfo = seriesInfo,
            notes = when {
                sanitized.startsWith("1600") || sanitized.startsWith("+911600") ->
                    "1600 series (Transactional/Toll-Free). Note: 1600 series is not inherently safe or authenticated against caller ID spoofing."
                sanitized.startsWith("140") || sanitized.startsWith("+91140") ->
                    "140 series (Commercial telemarketing prefix)."
                isSavedContact -> "Number identified in local device contacts."
                else -> "Unverified incoming caller. Number reputation lookup unverified."
            }
        )
    }

    /**
     * Formats Indian and international numbers cleanly.
     */
    fun formatDisplayNumber(rawNumber: String): String {
        val clean = rawNumber.trim().replace(" ", "").replace("-", "")
        return when {
            clean.startsWith("+91") && clean.length == 13 ->
                "+91 ${clean.substring(3, 8)} ${clean.substring(8)}"
            clean.length == 10 && clean.all { it.isDigit() } ->
                "+91 ${clean.substring(0, 5)} ${clean.substring(5)}"
            else -> rawNumber
        }
    }

    private fun sanitizePhoneNumber(raw: String): String {
        return raw.trim().replace(" ", "").replace("-", "")
    }

    private fun evaluateIndianNumbering(number: String): Pair<String, Boolean> {
        val standard = when {
            number.startsWith("+91") -> number.removePrefix("+91")
            number.startsWith("0") && number.length == 11 -> number.removePrefix("0")
            else -> number
        }

        return when {
            // 1600 series: Often used for banking IVR or toll-free, but commonly spoofed!
            standard.startsWith("1600") ->
                Pair("National 1600 Series (Transaction/Toll-Free, vulnerable to spoofing)", false)
            // 140 series: Telecom Regulatory Authority of India (TRAI) commercial telemarketing
            standard.startsWith("140") ->
                Pair("TRAI 140 Telemarketing Series", false)
            // 1800 series: Toll-free
            standard.startsWith("1800") ->
                Pair("Toll-Free 1800 Series", false)
            // Standard Indian 10-digit mobile (starts with 6, 7, 8, 9)
            standard.length == 10 && standard[0] in listOf('6', '7', '8', '9') ->
                Pair("Standard Mobile Subscriber", false)
            // Suspicious lengths or prefixes
            standard.length < 8 ->
                Pair("Abnormally Short Dialing Code / Potential CLI Mask", true)
            standard.startsWith("00") || standard.startsWith("+0") ->
                Pair("Irregular International Routing Prefix", true)
            else ->
                Pair("General Number", false)
        }
    }
}
