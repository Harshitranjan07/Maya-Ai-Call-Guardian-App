package com.maya.callguardian.domain.models

/**
 * State of a monitored call session.
 */
enum class CallState {
    IDLE,
    RINGING,
    ACTIVE,
    DISCONNECTED
}

/**
 * Deterministic presentation bands for call risk assessment.
 * 0–29: LOW (Normal monitoring)
 * 30–59: MEDIUM (Show caution and verification suggestions)
 * 60–79: HIGH (Strong warning and protective guidance)
 * 80–100: CRITICAL (Immediate safety warning and sensitive-action restrictions)
 */
enum class RiskLevel(val minScore: Int, val maxScore: Int, val label: String) {
    LOW(0, 29, "LOW RISK"),
    MEDIUM(30, 59, "SUSPICIOUS"),
    HIGH(60, 79, "HIGH RISK"),
    CRITICAL(80, 100, "CRITICAL SCAM DETECTED");

    companion object {
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped >= 80 -> CRITICAL
                clamped >= 60 -> HIGH
                clamped >= 30 -> MEDIUM
                else -> LOW
            }
        }
    }
}

/**
 * Categories of phone fraud and suspicious caller behavior.
 */
enum class ScamCategory(val displayName: String) {
    NONE("No Suspicious Activity"),
    BANKING_FRAUD("Banking & Financial Fraud"),
    DIGITAL_ARREST("Digital Arrest / Law Enforcement Impersonation"),
    FAKE_KYC("Fake KYC / Account Suspension"),
    REMOTE_ACCESS("Remote Access Tool Coercion"),
    LOTTERY_PRIZE("Lottery, Prize & Reward Scheme"),
    TECH_SUPPORT("Fake Tech Support"),
    IMPERSONATION("Authority & Official Impersonation"),
    SUSPICIOUS_INVESTMENT("Unregulated Investment Scam"),
    GENERAL_SUSPICIOUS("Suspicious High-Pressure Call")
}

/**
 * Identified scam signals detected via rules, classifiers, or conversation intelligence.
 */
enum class SignalType(val description: String, val baseWeight: Int) {
    OTP_REQUEST("Request for One-Time Password (OTP)", 45),
    UPI_PIN_REQUEST("Request for UPI PIN or Banking PIN", 45),
    PASSWORD_REQUEST("Request for Account Password or CVV", 45),
    FINANCIAL_INFO_REQUEST("Request for Bank Account Details, Card Number, or Credentials", 40),
    FAKE_KYC("Claimed KYC expiration or document update required", 25),
    ACCOUNT_SUSPENSION_THREAT("Threat of immediate bank/SIM suspension", 30),
    DIGITAL_ARREST_THREAT("Threat of digital arrest or CBI/Police action", 45),
    REMOTE_ACCESS_APP("Request to install AnyDesk, TeamViewer, QuickSupport", 40),
    URGENT_TRANSFER("Urgent instruction to transfer money or scan QR", 35),
    SECRECY_DEMAND("Instruction to keep conversation secret or not disconnect", 20),
    PRESSURE_TACTIC("High pressure, aggressive tone or countdown timer", 20),
    GOVERNMENT_IMPERSONATION("Claiming to represent Police, CBI, Customs, or RBI", 30),
    PRIZE_LOTTERY_CLAIM("Claim of unexpected cash prize or lottery win", 25),
    SUSPICIOUS_LINK("Request to open unverified link or download APK", 30),
    UNKNOWN_CALLER("Caller not found in contacts", 5),
    SUSPICIOUS_NUMBERING("Number matching high-risk telemarketing or spoofing pattern", 15),
    SYNTHETIC_VOICE_DETECTED("Synthetic or voice clone artifact observed", 25)
}

/**
 * Speaker origin for transcription.
 */
enum class Speaker {
    CALLER,
    USER,
    SYSTEM
}

/**
 * Contact status of the caller on the device.
 */
enum class ContactStatus {
    SAVED,
    UNKNOWN,
    BLOCKED
}

/**
 * Provenance evaluation of the caller's phone number.
 */
enum class NumberProvenance {
    VERIFIED,
    UNVERIFIED,
    SUSPECTED_SPOOF
}

/**
 * Operational mode of the guardian session.
 */
enum class OperationMode {
    SIMULATED_DEMO,
    MICROPHONE_TEST,
    REAL_CALL_MONITOR
}

/**
 * Voice authenticity engine status.
 */
enum class VoiceAuthenticityStatus {
    UNAVAILABLE,
    ANALYZING,
    AUTHENTIC_PROBABLE,
    SYNTHETIC_SUSPECTED
}

/**
 * Model loading status.
 */
enum class ModelStatus {
    NOT_INSTALLED,
    AVAILABLE,
    INITIALIZING,
    ACTIVE,
    FAILED,
    FALLBACK_ACTIVE
}
