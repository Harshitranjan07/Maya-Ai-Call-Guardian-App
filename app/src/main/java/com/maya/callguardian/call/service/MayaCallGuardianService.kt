package com.maya.callguardian.call.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.maya.callguardian.MayaApplication
import com.maya.callguardian.domain.models.CallState
import com.maya.callguardian.domain.models.OperationMode
import com.maya.callguardian.notifications.MayaNotificationManager

class MayaCallGuardianService : Service {
    constructor() : super()

    companion object {
        const val ACTION_START = "com.maya.callguardian.START_GUARDIAN"
        const val ACTION_STOP = "com.maya.callguardian.STOP_GUARDIAN"
    }

    private var telephonyCallback: Any? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerTelephonyStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as? MayaApplication
        val notificationManager = app?.notificationManager

        when (intent?.action) {
            ACTION_STOP -> {
                unregisterTelephonyStateListener()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val notification = notificationManager?.buildServiceNotification(
                    "Real-time scam protection active."
                )
                if (notification != null) {
                    try {
                        startForeground(MayaNotificationManager.NOTIFICATION_ID_SERVICE, notification)
                    } catch (e: Throwable) {
                        Log.w("MayaService", "Foreground start: ${e.message}")
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterTelephonyStateListener()
        super.onDestroy()
    }

    private fun registerTelephonyStateListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state, null)
                    }
                }
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                telephonyCallback = listener
            }
        } catch (e: Throwable) {
            Log.w("MayaService", "Failed to register telephony listener: ${e.message}")
        }
    }

    private fun unregisterTelephonyStateListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                (telephonyCallback as? PhoneStateListener)?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (e: Throwable) {
            // Ignore unregister errors
        }
    }

    private fun handleCallState(state: Int, incomingNumber: String? = null) {
        val app = applicationContext as? MayaApplication ?: return
        val sessionManager = app.sessionManager
        val notificationManager = app.notificationManager

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val rawNumber = incomingNumber ?: "Incoming Call"
                val (isContact, contactName) = lookupContact(this, rawNumber)

                sessionManager.startRealCall(
                    rawNumber = rawNumber,
                    isContact = isContact,
                    contactName = contactName
                )

                val risk = sessionManager.sessionState.value.riskAssessment
                notificationManager.showIncomingCallGuardianAlert(
                    callerNumber = rawNumber,
                    displayName = contactName ?: rawNumber,
                    riskLevel = risk.level,
                    riskScore = risk.score
                )
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                val current = sessionManager.sessionState.value
                if (current.callState == CallState.IDLE || current.callState == CallState.DISCONNECTED) {
                    sessionManager.startRealCall(
                        rawNumber = "Active Voice Call",
                        isContact = false
                    )
                }
                sessionManager.onCallConnected()
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (sessionManager.sessionState.value.operationMode == OperationMode.REAL_CALL_MONITOR) {
                    sessionManager.endCall()
                }
                notificationManager.cancelIncomingCallAlert()
            }
        }
    }

    private fun lookupContact(context: Context, phoneNumber: String): Pair<Boolean, String?> {
        if (phoneNumber.isBlank() || phoneNumber.contains("Unknown") || phoneNumber.contains("Incoming")) {
            return Pair(false, null)
        }
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        Pair(true, cursor.getString(nameIndex))
                    } else Pair(true, null)
                } else Pair(false, null)
            } ?: Pair(false, null)
        } catch (e: Throwable) {
            Pair(false, null)
        }
    }
}
