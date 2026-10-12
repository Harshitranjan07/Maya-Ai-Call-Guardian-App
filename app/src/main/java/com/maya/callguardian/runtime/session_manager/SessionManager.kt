package com.maya.callguardian.runtime.session_manager

import com.maya.callguardian.audio.transcription.SpeechRecognitionEngine
import com.maya.callguardian.data.preferences.SettingsRepository
import com.maya.callguardian.data.repositories.CallHistoryRepository
import com.maya.callguardian.domain.events.SessionEvent
import com.maya.callguardian.domain.models.*
import com.maya.callguardian.intelligence.caller_intelligence.CallerIntelligenceEngine
import com.maya.callguardian.intelligence.conversation_agent.ConversationAgent
import com.maya.callguardian.intelligence.conversation_agent.HeuristicSemanticAgent
import com.maya.callguardian.intelligence.scam_detection.ScamDetectionEngine
import com.maya.callguardian.intelligence.voice_authenticity.DefaultVoiceAuthenticityEngine
import com.maya.callguardian.intelligence.voice_authenticity.VoiceAuthenticityEngine
import com.maya.callguardian.notifications.MayaNotificationManager
import com.maya.callguardian.runtime.shared_state.CallSessionStateStore
import com.maya.callguardian.testing.simulated_calls.SimulatedCallScenario
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Orchestrates call sessions, concurrent AI agents, the continuous feedback loop,
 * persistence, and notification dispatching.
 */
class SessionManager(
    val stateStore: CallSessionStateStore = CallSessionStateStore(),
    private val callerIntelligenceEngine: CallerIntelligenceEngine = CallerIntelligenceEngine(),
    private val scamDetectionEngine: ScamDetectionEngine = ScamDetectionEngine(),
    private val conversationAgent: ConversationAgent = HeuristicSemanticAgent(),
    private val voiceAuthenticityEngine: VoiceAuthenticityEngine = DefaultVoiceAuthenticityEngine(),
    private val speechRecognitionEngine: SpeechRecognitionEngine? = null,
    private val historyRepository: CallHistoryRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val notificationManager: MayaNotificationManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    val sessionState: StateFlow<CallSessionState> = stateStore.state

    // Simulation state
    private var simulationJob: Job? = null
    private var currentSimStepIndex: Int = 0
    private var isSimPaused: Boolean = false
    private var isListeningToSpeech: Boolean = false

    /**
     * Starts a simulated call demonstration.
     */
    fun startSimulation(autoAdvance: Boolean = true) {
        stopSimulation()
        currentSimStepIndex = 0
        isSimPaused = false

        coroutineScope.launch {
            startSimulationInternal()
            if (autoAdvance) {
                runAutomatedSimulation()
            }
        }
    }

    suspend fun startSimulationInternal() {
        val sessionId = "sim_" + System.currentTimeMillis()
        val caller = CallerInfo(
            phoneNumber = SimulatedCallScenario.DEMO_CALLER_NUMBER,
            displayName = SimulatedCallScenario.DEMO_CALLER_NAME,
            contactStatus = ContactStatus.UNKNOWN,
            countryCode = "+91",
            formattedNumber = callerIntelligenceEngine.formatDisplayNumber(SimulatedCallScenario.DEMO_CALLER_NUMBER)
        )

        stateStore.reduce(
            SessionEvent.CallStarted(
                sessionId = sessionId,
                mode = OperationMode.SIMULATED_DEMO,
                callerInfo = caller
            )
        )

        // Evaluate caller intelligence
        val intelResult = callerIntelligenceEngine.analyze(
            rawNumber = caller.phoneNumber,
            isSavedContact = false
        )
        stateStore.reduce(SessionEvent.CallerIntelligenceEvaluated(intelResult))
    }

    private fun runAutomatedSimulation() {
        simulationJob = coroutineScope.launch {
            while (currentSimStepIndex < SimulatedCallScenario.steps.size) {
                if (!isSimPaused) {
                    val step = SimulatedCallScenario.steps[currentSimStepIndex]
                    processTranscriptEvent(step.transcript)
                    currentSimStepIndex++
                }
                delay(3500) // Delay between conversational dialogue turns for live observation
            }
        }
    }

    /**
     * Advances single step in simulation mode (for interactive manual stepping).
     */
    fun stepSimulation() {
        if (sessionState.value.callState != CallState.ACTIVE) {
            startSimulation(autoAdvance = false)
            return
        }

        if (currentSimStepIndex < SimulatedCallScenario.steps.size) {
            val step = SimulatedCallScenario.steps[currentSimStepIndex]
            coroutineScope.launch {
                processTranscriptEvent(step.transcript)
                currentSimStepIndex++
            }
        }
    }

    fun pauseSimulation() {
        isSimPaused = true
    }

    fun resumeSimulation() {
        isSimPaused = false
    }

    fun resetSimulation() {
        stopSimulation()
        coroutineScope.launch {
            stateStore.reduce(SessionEvent.ResetSession)
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Start passive or active real call monitoring with actual caller ID from Telephony.
     */
    fun startRealCall(
        rawNumber: String,
        isContact: Boolean,
        contactName: String? = null
    ) {
        stopSimulation()
        stopSpeechRecognition()

        coroutineScope.launch {
            val sessionId = "real_" + System.currentTimeMillis()
            val formatted = callerIntelligenceEngine.formatDisplayNumber(rawNumber)
            val displayName = contactName ?: if (rawNumber.isNotBlank() && !rawNumber.contains("Unknown")) formatted else "Incoming Caller"

            val caller = CallerInfo(
                phoneNumber = rawNumber,
                displayName = displayName,
                contactStatus = if (isContact) ContactStatus.SAVED else ContactStatus.UNKNOWN,
                formattedNumber = formatted
            )

            stateStore.reduce(
                SessionEvent.CallStarted(
                    sessionId = sessionId,
                    mode = OperationMode.REAL_CALL_MONITOR,
                    callerInfo = caller
                )
            )

            // Evaluate caller intelligence
            val intelResult = callerIntelligenceEngine.analyze(
                rawNumber = rawNumber,
                isSavedContact = isContact,
                contactName = contactName
            )
            stateStore.reduce(SessionEvent.CallerIntelligenceEvaluated(intelResult))
        }
    }

    /**
     * Called when the incoming or outgoing call is connected / answered.
     */
    fun onCallConnected() {
        if (sessionState.value.operationMode == OperationMode.REAL_CALL_MONITOR) {
            startSpeechRecognition()
        }
    }

    /**
     * Activates live speech recognition (microphone / speakerphone stream).
     */
    fun startSpeechRecognition() {
        if (isListeningToSpeech || speechRecognitionEngine == null) return
        isListeningToSpeech = true

        coroutineScope.launch {
            stateStore.reduce(
                SessionEvent.AgentStatusChanged(isTranscribing = true)
            )
            speechRecognitionEngine.startListening(
                onTranscriptChunk = { transcript ->
                    processTranscriptEvent(transcript)
                },
                onError = { _ ->
                    // Recoverable error handled by engine
                }
            )
        }
    }

    /**
     * Stops live speech recognition safely.
     */
    fun stopSpeechRecognition() {
        if (!isListeningToSpeech) return
        isListeningToSpeech = false
        coroutineScope.launch {
            speechRecognitionEngine?.stopListening()
            stateStore.reduce(
                SessionEvent.AgentStatusChanged(isTranscribing = false)
            )
        }
    }

    /**
     * Start live microphone monitoring mode.
     */
    fun startMicrophoneSession() {
        stopSimulation()
        stopSpeechRecognition()
        coroutineScope.launch {
            val sessionId = "mic_" + System.currentTimeMillis()
            val caller = CallerInfo(
                phoneNumber = "Mic Live Input",
                displayName = "Microphone Test Mode",
                contactStatus = ContactStatus.UNKNOWN,
                formattedNumber = "Direct Mic Stream"
            )

            stateStore.reduce(
                SessionEvent.CallStarted(
                    sessionId = sessionId,
                    mode = OperationMode.MICROPHONE_TEST,
                    callerInfo = caller
                )
            )

            startSpeechRecognition()
        }
    }

    /**
     * Injects a spoken or reported dialogue turn directly into the AI analysis pipeline.
     */
    fun injectTranscript(text: String, speaker: Speaker = Speaker.CALLER) {
        coroutineScope.launch {
            if (sessionState.value.callState != CallState.ACTIVE) {
                stopSimulation()
                val sessionId = "active_" + System.currentTimeMillis()
                val caller = CallerInfo(
                    phoneNumber = "+91 98765 43210",
                    displayName = "Caller Speech Input",
                    contactStatus = ContactStatus.UNKNOWN,
                    formattedNumber = "+91 98765 43210"
                )
                stateStore.reduce(
                    SessionEvent.CallStarted(
                        sessionId = sessionId,
                        mode = OperationMode.REAL_CALL_MONITOR,
                        callerInfo = caller
                    )
                )
                val intelResult = callerIntelligenceEngine.analyze(
                    rawNumber = "+91 98765 43210",
                    isSavedContact = false,
                    contactName = null
                )
                stateStore.reduce(SessionEvent.CallerIntelligenceEvaluated(intelResult))
            }

            processTranscriptEvent(
                TranscriptEvent(
                    speaker = speaker,
                    text = text,
                    timestampMs = System.currentTimeMillis(),
                    isFinal = true
                )
            )
        }
    }

    /**
     * Ingests a new transcript chunk and executes the concurrent feedback loop.
     */
    suspend fun processTranscriptEvent(transcript: TranscriptEvent) {
        // 1. Add transcript to session
        stateStore.reduce(SessionEvent.TranscriptChunkReceived(transcript))

        val current = sessionState.value
        val transcripts = current.transcriptEvents

        // 2. Concurrently trigger Scam Detection and Conversation Agent
        executeConcurrentFeedbackPipeline(transcripts)
    }

    /**
     * The Continuous Feedback Loop:
     * Scam Intelligence and Conversation Agent exchange observations asynchronously.
     */
    private suspend fun executeConcurrentFeedbackPipeline(transcripts: List<TranscriptEvent>) {
        val currentSnapshot = sessionState.value
        val sequenceNum = stateStore.nextSequenceNumber()

        stateStore.reduce(
            SessionEvent.AgentStatusChanged(
                isScamDetectorRunning = true,
                isConversationAgentRunning = true
            )
        )

        // Step A: Run Scam Detection and Conversation Agent concurrently
        val scamDeferred = coroutineScope.async {
            scamDetectionEngine.evaluate(
                transcripts = transcripts,
                observation = currentSnapshot.conversationObservation
            )
        }

        val conversationDeferred = coroutineScope.async {
            conversationAgent.analyze(
                recentTranscripts = transcripts.takeLast(4),
                callerInfo = currentSnapshot.callerInfo,
                currentSignals = currentSnapshot.scamAssessment.detectedSignals,
                currentRiskLevel = currentSnapshot.riskAssessment.level,
                previousObservation = currentSnapshot.conversationObservation
            )
        }

        val scamResult = scamDeferred.await()
        val convObservation = conversationDeferred.await()

        // Step B: Feedback Loop - Update scam detector with latest observation
        val feedbackScamResult = scamDetectionEngine.evaluate(
            transcripts = transcripts,
            observation = convObservation
        )

        // Step C: Atomically update state store with sequence checks
        stateStore.reduce(
            SessionEvent.ConversationObservationProduced(
                observation = convObservation,
                sequenceNumber = sequenceNum
            )
        )

        stateStore.reduce(
            SessionEvent.ScamAssessmentUpdated(
                assessment = feedbackScamResult,
                sequenceNumber = sequenceNum
            )
        )

        // Step D: Trigger notifications if risk has reached medium or higher
        val updatedRisk = sessionState.value.riskAssessment
        if (updatedRisk.level != RiskLevel.LOW) {
            val shouldNotify = settingsRepository?.isNotificationsEnabled?.first() ?: true
            if (shouldNotify) {
                notificationManager?.showRiskAlert(
                    riskLevel = updatedRisk.level,
                    threatReason = updatedRisk.primaryThreat,
                    score = updatedRisk.score
                )
            }
        }
    }

    /**
     * Ends the current active session and commits to Room database if enabled.
     */
    suspend fun endCallSync() {
        stopSimulation()
        stopSpeechRecognition()
        stateStore.reduce(SessionEvent.CallEnded())
        val finalSession = sessionState.value

        val shouldRetain = settingsRepository?.isHistoryRetentionEnabled?.first() ?: true
        if (shouldRetain && historyRepository != null) {
            historyRepository.saveSession(finalSession)
        }
    }

    fun endCall() {
        coroutineScope.launch {
            endCallSync()
        }
    }

    /**
     * User intervention button ("Take Over" / "I'm Safe").
     */
    fun userIntervene(action: String) {
        coroutineScope.launch {
            stateStore.reduce(SessionEvent.UserIntervened(action))
        }
    }
}
