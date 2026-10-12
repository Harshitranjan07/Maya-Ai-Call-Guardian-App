# MAYA — AI Call Guardian

**MAYA** is an Android mobile security application designed to protect users against evolving phone scams, social engineering, impersonation fraud, and credential theft in real time. 

Built with **Kotlin**, **Jetpack Compose (Material 3)**, **Coroutines/Flow**, **Room Database**, and **DataStore Preferences**, MAYA operates around a **shared live security state** powered by concurrent AI agents. Scam intelligence and conversational context continuously exchange structured observations so MAYA can dynamically calibrate risk as new conversational evidence appears.

---

## 🛡️ Core Architectural Pillars

### 1. Shared Live Security State & Concurrent Agent Concurrency
At the center of MAYA is the authoritative `CallSessionStateStore`. State updates are strictly immutable and synchronized through a coroutine `Mutex` with monotonic sequence tracking:
- **Concurrency & Stale-Rejection**: Asynchronous speech recognition, scam heuristics, and semantic intent agents process evidence concurrently. Older or delayed AI results cannot overwrite newer security decisions.
- **Continuous Two-Way Feedback Loop**:
  - `ScamDetectionEngine` $\rightarrow$ `ConversationAgent`: When suspicious triggers (e.g., bank impersonation, KYC threat) are detected, they are forwarded into the conversational agent's working context.
  - `ConversationAgent` $\rightarrow$ `ScamDetectionEngine`: When semantic intent analysis identifies covert urgency, intimidation, or credential demands (e.g., `SHARE_OTP`), it submits structured observations that re-evaluate scam probability and activate protective shields.

### 2. Deterministic Risk Engine & Presentation Bands
Risk is computed deterministically rather than relying on uncalibrated LLM hallucinations:
| Score Band | Risk Level | System Behavior |
| :--- | :--- | :--- |
| **0 – 29** | **LOW** | Passive monitoring; normal background inspection. |
| **30 – 59** | **MEDIUM** | Caution advisory; highlights unverified caller claims and suspicious numbering. |
| **60 – 79** | **HIGH** | Strong warning; restricts credential sharing and highlights psychological pressure. |
| **80 – 100** | **CRITICAL** | Immediate protective shield; strictly forbids disclosing OTPs, PINs, or passwords; recommends immediate termination. |

- **Duplicate Signal Prevention**: Deduplicates repeated phrases to prevent false escalation.
- **Critical Escalation Guard**: Any explicit request for OTP, PIN, password, or digital arrest immediately escalates into CRITICAL protection.

### 3. Immutable Security Policy Shield
All recommendations and allowed actions pass through the deterministic `SecurityPolicyEngine`.
> ⚠️ **Invariance Guarantee**: Generative LLM responses can **never** override security policy rules. The system will never advise or permit the disclosure of OTPs, UPI PINs, or remote-access installation under any circumstances.

### 4. Honest Platform & Model Degradation
- **Android Call Audio Limitation**: Standard third-party Android apps cannot freely record or stream remote cellular call audio without root or dedicated telecom firmware. MAYA **never** pretends to record cellular calls illegally. Instead, it provides a full-fidelity **12-Step Simulated Call Demonstration** and a **Live Microphone Testing Mode**.
- **Model Registry**: If LiteRT/Gemma or Whisper weights are unbundled, the app cleanly falls back to local regex/heuristic semantic analyzers without crashing or inventing fake confidence percentages.
- **Deepfake / Voice Authenticity**: Returns `"Voice authenticity analysis unavailable"` when weights are missing. Never generates random or fake deepfake percentages.

---

## 📱 Feature Capability Matrix

| Feature | Implementation Status | Execution Mode | Target Android Version |
| :--- | :--- | :--- | :--- |
| **Deterministic Risk Engine (0–100)** | ✅ Fully Implemented | Local On-Device | Android 8.0+ (API 26+) |
| **Rule-Based Scam Detector (EN/HI)** | ✅ Fully Implemented | Local Regex/Heuristics | Android 8.0+ (API 26+) |
| **12-Step Deterministic Demo Scenario** | ✅ Fully Implemented | Interactive Simulator | All Supported Versions |
| **Continuous Feedback Loop & Shared State** | ✅ Fully Implemented | Kotlin Mutex / StateFlow | Android 8.0+ (API 26+) |
| **Room Call History & Deletion Controls** | ✅ Fully Implemented | Local SQLite / Room | Android 8.0+ (API 26+) |
| **DataStore Preferences (Privacy First)** | ✅ Fully Implemented | Local DataStore | Android 8.0+ (API 26+) |
| **Telephony State Monitoring** | ✅ Fully Implemented | Android `TelephonyManager` | API 26–35 |
| **Android Speech Recognition** | ✅ Fully Implemented | `SpeechRecognizer` | API 26–35 |
| **Cellular Call Audio Capture** | ⚠️ Restricted by Android OS | Replaced by Sim & Mic modes | Platform Security Restriction |
| **Gemma 2B Conversation Agent** | ✅ Implemented with Fallback | Local LiteRT / Heuristic | Device RAM $\ge$ 4GB |
| **Voice Anti-Spoofing / Deepfake Model** | ⚠️ Transparently Unavailable | Honest Fallback (No faking) | Requires Certified Model |

---

## 🛠️ Project Structure

```
app/
 ├── data/
 │    ├── database/           # Room Database, CallHistoryEntity, CallHistoryDao
 │    ├── preferences/        # DataStore SettingsRepository (Local-only, Retention)
 │    └── repositories/       # CallHistoryRepository with complete deletion controls
 ├── domain/
 │    ├── events/             # Typed SessionEvent hierarchy
 │    ├── models/             # Immutable session models, risk levels, and enums
 │    ├── policies/           # Deterministic SecurityPolicyEngine
 │    └── risk/               # Deterministic RiskEngine (0-100 score calculation)
 ├── call/
 │    ├── integration/        # CallStateReceiver, CapabilityDetector
 │    └── service/            # MayaCallGuardianService (Foreground Service)
 ├── audio/
 │    └── transcription/      # SpeechRecognitionEngine & AndroidSpeechRecognizerEngine
 ├── intelligence/
 │    ├── caller_intelligence/# CallerIntelligenceEngine (Indian 1600/140 numbering aware)
 │    ├── conversation_agent/ # GemmaConversationAgent & HeuristicSemanticAgent
 │    ├── scam_detection/     # ScamDetectionEngine & ScamClassifier
 │    └── voice_authenticity/ # VoiceAuthenticityEngine (Honest non-fabricating engine)
 ├── ai/
 │    └── model_registry/     # ModelRegistry & runtime status tracking
 ├── runtime/
 │    ├── session_manager/    # SessionManager orchestrating concurrent loops
 │    └── shared_state/       # CallSessionStateStore (StateFlow, Mutex, Sequence tracking)
 ├── notifications/           # MayaNotificationManager (High/Low priority channels)
 ├── testing/
 │    └── simulated_calls/    # SimulatedCallScenario (12-step deterministic flow)
 └── ui/
      ├── components/         # RiskMeter, EvidenceCard, SecurityBanner, TranscriptStream
      ├── dashboard/          # DashboardScreen (Protection status, Live risk, Controls)
      ├── live_call/          # LiveCallGuardianScreen (Real-time meter, transcripts, controls)
      ├── threat_details/     # ThreatDetailsScreen (Factor contributions, Safe next steps)
      ├── history/            # CallHistoryScreen (Logs, Deletion, Confirmation dialogs)
      ├── settings/           # SettingsScreen (DataStore toggles, Model registry)
      ├── capability/         # CapabilityScreen (Platform diagnostics & Feature matrix)
      ├── navigation/         # MayaNavGraph (Material 3 Navigation Bar)
      └── theme/              # Dark Navy Palette, Restrained Violet/Cyan, High-contrast Risk
```

---

## 🚀 Building & Running

### Prerequisites
- **JDK**: Java 21 (`openjdk@21`)
- **Android SDK**: Compile SDK `35`, Min SDK `26`
- **Gradle**: 9.3.1 (via included wrapper `./gradlew`)

### Build Debug APK
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21 # Or your JDK 21 path
./gradlew assembleDebug
```
The APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
MAYA includes a comprehensive JUnit test suite validating risk calculation, duplicate signal prevention, Indian numbering rules, concurrency sequence tracking, and the 12-step simulated call flow:
```bash
./gradlew testDebugUnitTest
```

---

## 🔬 Deterministic 12-Step Demonstration Walkthrough

When testing the application in **Simulated Call Mode**:
1. **Unknown Caller Greets**: Baseline `UNKNOWN_CALLER` signal evaluated (Score: 5 / LOW).
2. **Authority Impersonation**: Caller claims to represent *"HDFC Bank Central Security Division"* (Intent: `AUTHORITY_INSPECTION`).
3. **Fake KYC Expiration**: Caller claims mandatory KYC has expired and PAN is non-compliant (Signal: `FAKE_KYC`, Score escalates to MEDIUM).
4. **Severe Urgency & Threat**: Caller threatens account suspension within 15 minutes (Signals: `ACCOUNT_SUSPENSION_THREAT`, `PRESSURE_TACTIC`, Score escalates to HIGH).
5. **Direct OTP Demand**: Caller asks for 6-digit OTP code received via SMS (Signal: `OTP_REQUEST`, Sensitive request flagged).
6. **Risk Engine Escalation**: Escalates to **CRITICAL band (Score: 85)**.
7. **Conversation Agent Observation**: Records `CREDENTIAL_EXTRACTION_FRAUD`, urgency `HIGH`, action `SHARE_OTP`.
8. **Feedback Loop Sync**: Scam engine validates observations and updates evidence trail.
9. **Security Policy Enforcement**: Switches to CRITICAL protective shield with `NEVER_DISCLOSE_OTP` constraint.
10. **MAYA Prominent Warning**: Displays bold alert: *"NEVER DISCLOSE YOUR OTP OR PASSWORDS"*.
11. **Live Dashboard Update**: Radial risk meter turns red; evidence card logs excerpt.
12. **User Inspection & Safe Termination**: User inspects contributing factors and safely ends the call.

---

## 🔒 Privacy & Security Standards
- **Zero Cloud Uploads by Default**: Raw conversation transcripts and audio remain strictly in device volatile memory or local Room storage if explicitly configured.
- **Transparent Deletion Controls**: Users can delete individual call session records or wipe all history with one click.
- **No Hardcoded Keys**: Pure local inference with no external third-party API dependencies or hidden credentials.
- **System Compliance**: Complies with Android 14+ foreground service and permission specifications.
