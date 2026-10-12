package com.maya.callguardian.audio.transcription

import com.maya.callguardian.domain.models.TranscriptEvent

/**
 * Interchangeable Speech-To-Text interface.
 * Supports local Whisper, Android SpeechRecognizer, or Mock/Simulated engines.
 */
interface SpeechRecognitionEngine {
    val engineName: String
    val isSupportedOnDevice: Boolean
    val isListening: Boolean

    suspend fun startListening(
        onTranscriptChunk: suspend (TranscriptEvent) -> Unit,
        onError: (String) -> Unit
    )

    suspend fun stopListening()
}
