package com.maya.callguardian.call.integration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.maya.callguardian.MayaApplication
import com.maya.callguardian.call.service.MayaCallGuardianService
import com.maya.callguardian.domain.models.*

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val rawIncomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?: intent.getStringExtra("incoming_number")
            ?: ""

        val app = context.applicationContext as? MayaApplication ?: return
        val sessionManager = app.sessionManager
        val notificationManager = app.notificationManager

        Log.d("CallStateReceiver", "Phone State Changed: state=$stateStr, incomingNumber=$rawIncomingNumber")

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val displayCallerNumber = if (rawIncomingNumber.isNotBlank()) rawIncomingNumber else "Unknown / Private Caller"
                val (isContact, contactName) = lookupContact(context, rawIncomingNumber)

                val currentState = sessionManager.sessionState.value
                // If already tracking this exact ringing call, avoid resetting state
                if (currentState.callState == CallState.RINGING && currentState.callerInfo.phoneNumber == displayCallerNumber) {
                    return
                }

                // Start real call session
                sessionManager.startRealCall(
                    rawNumber = displayCallerNumber,
                    isContact = isContact,
                    contactName = contactName
                )

                // Show high-priority heads-up notification with action to open live guardian screen
                val risk = sessionManager.sessionState.value.riskAssessment
                notificationManager.showIncomingCallGuardianAlert(
                    callerNumber = displayCallerNumber,
                    displayName = contactName ?: displayCallerNumber,
                    riskLevel = risk.level,
                    riskScore = risk.score
                )

                // Safely start foreground service if allowed by system
                try {
                    val serviceIntent = Intent(context, MayaCallGuardianService::class.java).apply {
                        action = MayaCallGuardianService.ACTION_START
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Throwable) {
                    Log.w("CallStateReceiver", "Service start deferred: ${e.message}")
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered or outgoing call
                val currentState = sessionManager.sessionState.value
                if (currentState.callState == CallState.IDLE || currentState.callState == CallState.DISCONNECTED) {
                    val displayCallerNumber = if (rawIncomingNumber.isNotBlank()) rawIncomingNumber else "Active Call"
                    val (isContact, contactName) = lookupContact(context, rawIncomingNumber)
                    sessionManager.startRealCall(
                        rawNumber = displayCallerNumber,
                        isContact = isContact,
                        contactName = contactName
                    )
                }

                sessionManager.onCallConnected()

                // Try start foreground service
                try {
                    val serviceIntent = Intent(context, MayaCallGuardianService::class.java).apply {
                        action = MayaCallGuardianService.ACTION_START
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Throwable) {
                    Log.w("CallStateReceiver", "Service start deferred: ${e.message}")
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended / hung up
                if (sessionManager.sessionState.value.operationMode == OperationMode.REAL_CALL_MONITOR) {
                    sessionManager.endCall()
                }
                notificationManager.cancelIncomingCallAlert()
                try {
                    val serviceIntent = Intent(context, MayaCallGuardianService::class.java).apply {
                        action = MayaCallGuardianService.ACTION_STOP
                    }
                    context.stopService(serviceIntent)
                } catch (e: Throwable) {
                    Log.w("CallStateReceiver", "Service stop error: ${e.message}")
                }
            }
        }
    }

    private fun lookupContact(context: Context, phoneNumber: String): Pair<Boolean, String?> {
        if (phoneNumber.isBlank() || phoneNumber.contains("Unknown") || phoneNumber.contains("Restricted")) {
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
