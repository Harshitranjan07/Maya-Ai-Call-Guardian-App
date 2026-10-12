package com.maya.callguardian.intelligence.voice_authenticity

import com.maya.callguardian.domain.models.*

/**
 * Interface for Voice Anti-Spoofing and deepfake audio detection.
 */
interface VoiceAuthenticityEngine {
    val isModelAvailable: Boolean
    val modelName: String

    fun analyzeAudioChunk(audioBytes: ByteArray, sampleRate: Int): VoiceAuthenticityResult
}

/**
 * Honest implementation that reports "Voice authenticity analysis unavailable"
 * when no certified on-device model weights are present.
 * Adheres strictly to the safety rule: NEVER invent fake percentages or random scores.
 */
class DefaultVoiceAuthenticityEngine(
    private val modelPath: String? = null
) : VoiceAuthenticityEngine {

    override val isModelAvailable: Boolean = modelPath != null && java.io.File(modelPath).exists()
    override val modelName: String = "EdgeSpoof-LinearClassifier (Unbundled)"

    override fun analyzeAudioChunk(audioBytes: ByteArray, sampleRate: Int): VoiceAuthenticityResult {
        if (!isModelAvailable) {
            return VoiceAuthenticityResult(
                syntheticVoiceIndicatorScore = 0.0f,
                confidence = 0.0f,
                status = VoiceAuthenticityStatus.UNAVAILABLE,
                supportingEvidence = "Voice authenticity analysis unavailable. (Requires certified on-device anti-spoofing model).",
                isModelAvailable = false
            )
        }

        // When physical model file is loaded on device:
        return VoiceAuthenticityResult(
            syntheticVoiceIndicatorScore = 0.0f,
            confidence = 0.0f,
            status = VoiceAuthenticityStatus.UNAVAILABLE,
            supportingEvidence = "Voice authenticity analysis unavailable.",
            isModelAvailable = false
        )
    }
}
