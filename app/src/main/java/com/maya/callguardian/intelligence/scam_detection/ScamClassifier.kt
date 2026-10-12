package com.maya.callguardian.intelligence.scam_detection

import com.maya.callguardian.domain.models.ScamCategory

/**
 * Interface for optional on-device lightweight text / audio classifier (e.g., LiteRT, ONNX, Edge).
 * Will honestly report availability when model weights are not loaded.
 */
interface ScamClassifier {
    val isModelLoaded: Boolean
    val modelName: String

    fun classify(text: String): ClassifierResult?

    data class ClassifierResult(
        val topCategory: ScamCategory,
        val probability: Float,
        val inferenceTimeMs: Long
    )
}

/**
 * Fallback classifier when no binary weights are bundled or downloaded.
 */
class NoOpScamClassifier : ScamClassifier {
    override val isModelLoaded: Boolean = false
    override val modelName: String = "LiteRT-MobileScam-v1 (Unbundled)"

    override fun classify(text: String): ScamClassifier.ClassifierResult? {
        // Honestly return null when model file is not present on device
        return null
    }
}
