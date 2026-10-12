package com.maya.callguardian.audio.transcription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.maya.callguardian.domain.models.Speaker
import com.maya.callguardian.domain.models.TranscriptEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Android system speech recognition implementation.
 * Used for live microphone & speakerphone call monitoring.
 * Features continuous auto-restart to capture full conversations.
 */
class AndroidSpeechRecognizerEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : SpeechRecognitionEngine {

    override val engineName: String = "Android System SpeechRecognizer"
    override val isSupportedOnDevice: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)
    private var _isListening: Boolean = false
    override val isListening: Boolean get() = _isListening

    @Volatile
    private var shouldKeepListening: Boolean = false

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentChunkCallback: (suspend (TranscriptEvent) -> Unit)? = null
    private var currentErrorCallback: ((String) -> Unit)? = null

    override suspend fun startListening(
        onTranscriptChunk: suspend (TranscriptEvent) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isSupportedOnDevice) {
            onError("Android Speech Recognition is not available on this device.")
            return
        }

        currentChunkCallback = onTranscriptChunk
        currentErrorCallback = onError
        shouldKeepListening = true

        coroutineScope.launch(Dispatchers.Main) {
            startInternal()
        }
    }

    private fun startInternal() {
        if (!shouldKeepListening) return
        try {
            if (speechRecognizer == null) {
                val recognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
                recognizer.setRecognitionListener(createListener())
                speechRecognizer = recognizer
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            speechRecognizer?.startListening(intent)
            _isListening = true
        } catch (e: Throwable) {
            _isListening = false
            currentErrorCallback?.invoke("Speech init error: ${e.message}")
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.d("SpeechRecognizer", "Recognition event code: $error")
            val isRecoverable = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_AUDIO ||
                    error == SpeechRecognizer.ERROR_NETWORK ||
                    error == SpeechRecognizer.ERROR_SERVER

            if (shouldKeepListening && isRecoverable) {
                val delayMs = if (error == SpeechRecognizer.ERROR_AUDIO) 800L else 350L
                mainHandler.postDelayed({
                    if (shouldKeepListening) {
                        try {
                            speechRecognizer?.cancel()
                            startInternal()
                        } catch (e: Throwable) {
                            Log.w("SpeechRecognizer", "Restart attempt error: ${e.message}")
                        }
                    }
                }, delayMs)
            } else {
                _isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing audio permission"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Recognition code: $error"
                }
                currentErrorCallback?.invoke(msg)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                coroutineScope.launch {
                    currentChunkCallback?.invoke(
                        TranscriptEvent(
                            speaker = Speaker.CALLER,
                            text = text,
                            timestampMs = System.currentTimeMillis(),
                            isFinal = true
                        )
                    )
                }
            }

            // Continuous recognition: restart for next conversational turn
            if (shouldKeepListening) {
                mainHandler.postDelayed({
                    if (shouldKeepListening) {
                        try {
                            startInternal()
                        } catch (e: Throwable) {
                            Log.w("SpeechRecognizer", "Restart onResults error: ${e.message}")
                        }
                    }
                }, 150)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                coroutineScope.launch {
                    currentChunkCallback?.invoke(
                        TranscriptEvent(
                            speaker = Speaker.CALLER,
                            text = text,
                            timestampMs = System.currentTimeMillis(),
                            isFinal = false
                        )
                    )
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override suspend fun stopListening() {
        shouldKeepListening = false
        _isListening = false
        coroutineScope.launch(Dispatchers.Main) {
            try {
                mainHandler.removeCallbacksAndMessages(null)
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Throwable) {
                Log.w("SpeechRecognizer", "Stop error: ${e.message}")
            }
        }
    }
}
