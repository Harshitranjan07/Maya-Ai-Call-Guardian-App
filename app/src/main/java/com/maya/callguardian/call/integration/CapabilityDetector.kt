package com.maya.callguardian.call.integration

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.SpeechRecognizer
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class DeviceCapabilityReport(
    val androidVersion: String,
    val apiLevel: Int,
    val hasTelephonyHardware: Boolean,
    val phoneStatePermissionGranted: Boolean,
    val callLogPermissionGranted: Boolean,
    val contactsPermissionGranted: Boolean,
    val audioRecordPermissionGranted: Boolean,
    val notificationPermissionGranted: Boolean,
    val speechRecognitionAvailable: Boolean,
    val cellularAudioCaptureSupported: Boolean,
    val cellularAudioCaptureLimitationReason: String
)

class CapabilityDetector(private val context: Context) {

    fun detectCapabilities(): DeviceCapabilityReport {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val hasTelephony = telephonyManager?.phoneType != TelephonyManager.PHONE_TYPE_NONE

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val callLogGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val speechRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)

        return DeviceCapabilityReport(
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            hasTelephonyHardware = hasTelephony,
            phoneStatePermissionGranted = phoneStateGranted,
            callLogPermissionGranted = callLogGranted,
            contactsPermissionGranted = contactsGranted,
            audioRecordPermissionGranted = audioGranted,
            notificationPermissionGranted = notificationGranted,
            speechRecognitionAvailable = speechRecognizerAvailable,
            cellularAudioCaptureSupported = false, // Honestly report platform constraint!
            cellularAudioCaptureLimitationReason = "Standard Android security architecture strictly forbids third-party apps from capturing remote cellular call audio without root or dedicated telecom system firmware."
        )
    }
}
