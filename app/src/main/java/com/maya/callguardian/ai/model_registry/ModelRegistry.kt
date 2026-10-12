package com.maya.callguardian.ai.model_registry

import com.maya.callguardian.domain.models.ModelStatus

data class ModelInfo(
    val id: String,
    val displayName: String,
    val taskType: String,
    val status: ModelStatus,
    val runtime: String,
    val memoryRequirementMb: Int,
    val isBundled: Boolean,
    val lastError: String? = null,
    val fallbackStrategy: String
)

class ModelRegistry {

    fun getRegisteredModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "gemma_2b",
                displayName = "Gemma 2B Instruct",
                taskType = "Conversational Intent & Context Analysis",
                status = ModelStatus.FALLBACK_ACTIVE,
                runtime = "Google AI Edge / LiteRT",
                memoryRequirementMb = 1800,
                isBundled = false,
                lastError = "Model weights not downloaded locally. Operating in zero-dependency heuristic fallback mode.",
                fallbackStrategy = "Heuristic Semantic Intent Analyzer (Active)"
            ),
            ModelInfo(
                id = "whisper_tiny",
                displayName = "Whisper STT (Local)",
                taskType = "On-Device Multilingual Speech-To-Text",
                status = ModelStatus.FALLBACK_ACTIVE,
                runtime = "TFLite / ONNX Runtime",
                memoryRequirementMb = 250,
                isBundled = false,
                lastError = "Optional model weights unbundled to keep APK lightweight.",
                fallbackStrategy = "Android System SpeechRecognizer (Active)"
            ),
            ModelInfo(
                id = "edge_spoof",
                displayName = "Voice Authenticity Classifier",
                taskType = "Synthetic Voice & Voice Clone Detection",
                status = ModelStatus.NOT_INSTALLED,
                runtime = "LiteRT Audio Classifier",
                memoryRequirementMb = 85,
                isBundled = false,
                lastError = "No certified anti-spoofing model installed.",
                fallbackStrategy = "Analysis marked UNAVAILABLE (Strict honest reporting)"
            ),
            ModelInfo(
                id = "android_tts",
                displayName = "Android Text-To-Speech",
                taskType = "Voice Safety Guidance",
                status = ModelStatus.ACTIVE,
                runtime = "Android System Service",
                memoryRequirementMb = 20,
                isBundled = true,
                lastError = null,
                fallbackStrategy = "Standard Android TTS Engine"
            )
        )
    }
}
