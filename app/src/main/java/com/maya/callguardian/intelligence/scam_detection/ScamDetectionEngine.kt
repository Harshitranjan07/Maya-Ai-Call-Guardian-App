package com.maya.callguardian.intelligence.scam_detection

import com.maya.callguardian.domain.models.*
import java.util.regex.Pattern

/**
 * Hybrid Scam Detection Engine.
 * Combines rule-based pattern matching (English & Hindi),
 * an optional local classifier, and semantic conversation agent observations.
 */
class ScamDetectionEngine(
    private val classifier: ScamClassifier = NoOpScamClassifier()
) {

    // Compiled Regex patterns covering English and Hindi fraud phrasing
    private val otpPattern = Pattern.compile(
        "\\b(otp|one time password|six digit code|6[- ]digit|sms code|batao otp|otp send kiya|otp share|verification code|share otp|send otp|give otp|tell me otp|what is the otp|otp number|otp do|otp bata|otp chahiye)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val financialDetailsPattern = Pattern.compile(
        "\\b(bank details|bank account|account number|account details|banking details|credit card|debit card|card number|ifsc|net banking|passcode|bank info|account info|card details)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val upiPinPattern = Pattern.compile(
        "\\b(upi pin|mpin|atm pin|pin enter|pin daaliye|gpay pin|phonepe pin|bhim pin)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val passwordPattern = Pattern.compile(
        "\\b(password|cvv|card expiry|netbanking login|security code)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val kycPattern = Pattern.compile(
        "\\b(kyc|kyc expire|kyc update|pan card link|aadhaar update|account verify|kyc pending|kyc verification expired)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val accountSuspensionPattern = Pattern.compile(
        "\\b(block ho jayega|account freeze|account suspend|sim block|card block|within 24 hours|within 15 minutes|services stopped)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val digitalArrestPattern = Pattern.compile(
        "\\b(digital arrest|cbi|crime branch|cyber cell|narcotics|police station|arrest warrant|illegal parcel|money laundering|supreme court notice)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val remoteAccessPattern = Pattern.compile(
        "\\b(anydesk|teamviewer|quicksupport|rustdesk|screen share|app download karo|apk file)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val urgentTransferPattern = Pattern.compile(
        "\\b(turant bhejo|urgent transfer|pay immediately|send money|scan qr|refund claim|link par click)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val secrecyDemandPattern = Pattern.compile(
        "\\b(kisi ko mat batana|do not tell anyone|confidential inquiry|line par rahiye|do not disconnect|isolate yourself)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val pressurePattern = Pattern.compile(
        "\\b(immediately|right now|hurry up|last warning|aakhri mauka|warna jail|action will be taken)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val prizeLotteryPattern = Pattern.compile(
        "\\b(lottery|lucky winner|kbc prize|cash reward|car prize|gift voucher)\\b",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Evaluates all cumulative transcript events and integrates semantic observations.
     */
    fun evaluate(
        transcripts: List<TranscriptEvent>,
        observation: ConversationObservation? = null
    ): ScamAssessment {
        val detectedSignals = linkedSetOf<SignalType>()
        val evidenceList = mutableListOf<EvidenceItem>()
        var primaryCategory = ScamCategory.NONE
        var sensitiveDetected = false

        // 1. Analyze each transcript event for temporal evidence
        for (event in transcripts) {
            val text = event.text
            val ts = event.timestampMs

            if (otpPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.OTP_REQUEST)
                evidenceList.add(EvidenceItem(SignalType.OTP_REQUEST, "TRANSCRIPT", ts, text))
                sensitiveDetected = true
                if (primaryCategory == ScamCategory.NONE) primaryCategory = ScamCategory.BANKING_FRAUD
            }

            if (financialDetailsPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.FINANCIAL_INFO_REQUEST)
                evidenceList.add(EvidenceItem(SignalType.FINANCIAL_INFO_REQUEST, "TRANSCRIPT", ts, text))
                sensitiveDetected = true
                primaryCategory = ScamCategory.BANKING_FRAUD
            }

            if (upiPinPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.UPI_PIN_REQUEST)
                evidenceList.add(EvidenceItem(SignalType.UPI_PIN_REQUEST, "TRANSCRIPT", ts, text))
                sensitiveDetected = true
                primaryCategory = ScamCategory.BANKING_FRAUD
            }

            if (passwordPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.PASSWORD_REQUEST)
                evidenceList.add(EvidenceItem(SignalType.PASSWORD_REQUEST, "TRANSCRIPT", ts, text))
                sensitiveDetected = true
                primaryCategory = ScamCategory.BANKING_FRAUD
            }

            if (kycPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.FAKE_KYC)
                evidenceList.add(EvidenceItem(SignalType.FAKE_KYC, "TRANSCRIPT", ts, text))
                if (primaryCategory == ScamCategory.NONE) primaryCategory = ScamCategory.FAKE_KYC
            }

            if (accountSuspensionPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.ACCOUNT_SUSPENSION_THREAT)
                evidenceList.add(EvidenceItem(SignalType.ACCOUNT_SUSPENSION_THREAT, "TRANSCRIPT", ts, text))
                if (primaryCategory == ScamCategory.NONE) primaryCategory = ScamCategory.FAKE_KYC
            }

            if (digitalArrestPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.DIGITAL_ARREST_THREAT)
                evidenceList.add(EvidenceItem(SignalType.DIGITAL_ARREST_THREAT, "TRANSCRIPT", ts, text))
                primaryCategory = ScamCategory.DIGITAL_ARREST
            }

            if (remoteAccessPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.REMOTE_ACCESS_APP)
                evidenceList.add(EvidenceItem(SignalType.REMOTE_ACCESS_APP, "TRANSCRIPT", ts, text))
                sensitiveDetected = true
                primaryCategory = ScamCategory.REMOTE_ACCESS
            }

            if (urgentTransferPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.URGENT_TRANSFER)
                evidenceList.add(EvidenceItem(SignalType.URGENT_TRANSFER, "TRANSCRIPT", ts, text))
                if (primaryCategory == ScamCategory.NONE) primaryCategory = ScamCategory.BANKING_FRAUD
            }

            if (secrecyDemandPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.SECRECY_DEMAND)
                evidenceList.add(EvidenceItem(SignalType.SECRECY_DEMAND, "TRANSCRIPT", ts, text))
            }

            if (pressurePattern.matcher(text).find()) {
                detectedSignals.add(SignalType.PRESSURE_TACTIC)
                evidenceList.add(EvidenceItem(SignalType.PRESSURE_TACTIC, "TRANSCRIPT", ts, text))
            }

            if (prizeLotteryPattern.matcher(text).find()) {
                detectedSignals.add(SignalType.PRIZE_LOTTERY_CLAIM)
                evidenceList.add(EvidenceItem(SignalType.PRIZE_LOTTERY_CLAIM, "TRANSCRIPT", ts, text))
                if (primaryCategory == ScamCategory.NONE) primaryCategory = ScamCategory.LOTTERY_PRIZE
            }
        }

        // 2. Incorporate ConversationAgent semantic observations (continuous feedback loop)
        if (observation != null) {
            val obsTs = observation.timestampMs

            if (observation.authorityClaim) {
                detectedSignals.add(SignalType.GOVERNMENT_IMPERSONATION)
                evidenceList.add(
                    EvidenceItem(
                        SignalType.GOVERNMENT_IMPERSONATION,
                        "CONVERSATION_AGENT",
                        obsTs,
                        "Caller claimed authority: ${observation.claimedOrganization}"
                    )
                )
                if (primaryCategory == ScamCategory.NONE) {
                    primaryCategory = ScamCategory.IMPERSONATION
                }
            }

            if (observation.pressureBehavior || observation.urgencyLevel == "HIGH" || observation.urgencyLevel == "CRITICAL") {
                detectedSignals.add(SignalType.PRESSURE_TACTIC)
                evidenceList.add(
                    EvidenceItem(
                        SignalType.PRESSURE_TACTIC,
                        "CONVERSATION_AGENT",
                        obsTs,
                        "High urgency/pressure detected: ${observation.urgencyLevel}"
                    )
                )
            }

            if (observation.sensitiveInformationRequested || observation.requestedAction.contains("OTP", ignoreCase = true)) {
                detectedSignals.add(SignalType.OTP_REQUEST)
                sensitiveDetected = true
                evidenceList.add(
                    EvidenceItem(
                        SignalType.OTP_REQUEST,
                        "CONVERSATION_AGENT",
                        obsTs,
                        "Sensitive request identified: ${observation.requestedAction}"
                    )
                )
            }
        }

        // 3. Optional Classifier Inference
        var modelProbability: Float? = null
        if (classifier.isModelLoaded && transcripts.isNotEmpty()) {
            val fullText = transcripts.joinToString(" ") { it.text }
            val classResult = classifier.classify(fullText)
            if (classResult != null) {
                modelProbability = classResult.probability
                if (primaryCategory == ScamCategory.NONE) {
                    primaryCategory = classResult.topCategory
                }
            }
        }

        // 4. Calculate calibrated probability or heuristic score
        val isHeuristic = modelProbability == null
        val score = if (modelProbability != null) {
            modelProbability
        } else {
            calculateHeuristicProbability(detectedSignals, sensitiveDetected)
        }

        // Deduplicate evidence items by signal and excerpt
        val distinctEvidence = evidenceList.distinctBy { it.signal.name + "_" + it.excerpt }

        val explanation = buildExplanation(detectedSignals, primaryCategory, sensitiveDetected)

        return ScamAssessment(
            scamProbability = score,
            isHeuristicScore = isHeuristic,
            category = primaryCategory,
            detectedSignals = detectedSignals.toList(),
            sensitiveRequestDetected = sensitiveDetected,
            evidenceList = distinctEvidence,
            explanation = explanation
        )
    }

    private fun calculateHeuristicProbability(
        signals: Set<SignalType>,
        sensitiveDetected: Boolean
    ): Float {
        if (signals.isEmpty()) return 0.05f

        var points = 0
        for (s in signals) {
            points += s.baseWeight
        }
        if (sensitiveDetected) points += 20

        // Normalize points to a heuristic 0.0 to 1.0 probability
        return (points / 100.0f).coerceIn(0.10f, 0.98f)
    }

    private fun buildExplanation(
        signals: Set<SignalType>,
        category: ScamCategory,
        sensitiveDetected: Boolean
    ): String {
        if (signals.isEmpty()) return "Passive monitoring: No scam patterns identified."

        val parts = mutableListOf<String>()
        if (category != ScamCategory.NONE) {
            parts.add("Categorized as ${category.displayName}.")
        }
        if (sensitiveDetected) {
            parts.add("High-risk demand for sensitive credentials (OTP/PIN/password) detected.")
        }
        val topSignals = signals.take(3).joinToString(", ") { it.description }
        parts.add("Active triggers: $topSignals.")

        return parts.joinToString(" ")
    }
}
