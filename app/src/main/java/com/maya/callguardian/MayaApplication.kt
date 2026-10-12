package com.maya.callguardian

import android.app.Application
import com.maya.callguardian.ai.model_registry.ModelRegistry
import com.maya.callguardian.call.integration.CapabilityDetector
import android.content.IntentFilter
import android.os.Build
import android.telephony.TelephonyManager
import com.maya.callguardian.audio.transcription.AndroidSpeechRecognizerEngine
import com.maya.callguardian.call.integration.CallStateReceiver
import com.maya.callguardian.data.database.MayaDatabase
import com.maya.callguardian.data.preferences.SettingsRepository
import com.maya.callguardian.data.repositories.CallHistoryRepository
import com.maya.callguardian.intelligence.caller_intelligence.CallerIntelligenceEngine
import com.maya.callguardian.intelligence.conversation_agent.GemmaConversationAgent
import com.maya.callguardian.intelligence.scam_detection.ScamDetectionEngine
import com.maya.callguardian.intelligence.voice_authenticity.DefaultVoiceAuthenticityEngine
import com.maya.callguardian.notifications.MayaNotificationManager
import com.maya.callguardian.runtime.session_manager.SessionManager
import com.maya.callguardian.runtime.shared_state.CallSessionStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MayaApplication : Application() {

    lateinit var database: MayaDatabase private set
    lateinit var historyRepository: CallHistoryRepository private set
    lateinit var settingsRepository: SettingsRepository private set
    lateinit var notificationManager: MayaNotificationManager private set
    lateinit var stateStore: CallSessionStateStore private set
    lateinit var sessionManager: SessionManager private set
    lateinit var modelRegistry: ModelRegistry private set
    lateinit var capabilityDetector: CapabilityDetector private set
    lateinit var speechEngine: AndroidSpeechRecognizerEngine private set

    private val callReceiver = CallStateReceiver()

    override fun onCreate() {
        super.onCreate()

        database = MayaDatabase.getInstance(this)
        historyRepository = CallHistoryRepository(database.callHistoryDao())
        settingsRepository = SettingsRepository(this)
        notificationManager = MayaNotificationManager(this)
        stateStore = CallSessionStateStore()
        modelRegistry = ModelRegistry()
        capabilityDetector = CapabilityDetector(this)

        val callerIntelligenceEngine = CallerIntelligenceEngine()
        val scamDetectionEngine = ScamDetectionEngine()
        val conversationAgent = GemmaConversationAgent() // Auto-checks for LiteRT weights, falls back gracefully
        val voiceAuthenticityEngine = DefaultVoiceAuthenticityEngine()
        speechEngine = AndroidSpeechRecognizerEngine(this, CoroutineScope(Dispatchers.Default + SupervisorJob()))

        sessionManager = SessionManager(
            stateStore = stateStore,
            callerIntelligenceEngine = callerIntelligenceEngine,
            scamDetectionEngine = scamDetectionEngine,
            conversationAgent = conversationAgent,
            voiceAuthenticityEngine = voiceAuthenticityEngine,
            speechRecognitionEngine = speechEngine,
            historyRepository = historyRepository,
            settingsRepository = settingsRepository,
            notificationManager = notificationManager
        )

        // Dynamically register CallStateReceiver to ensure instant phone state callbacks
        try {
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(callReceiver, filter)
            }
        } catch (e: Throwable) {
            // Manifest receiver acts as primary fallback
        }
    }
}
