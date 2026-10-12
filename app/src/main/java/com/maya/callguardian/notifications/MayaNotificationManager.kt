package com.maya.callguardian.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.maya.callguardian.MainActivity
import com.maya.callguardian.R
import com.maya.callguardian.domain.models.RiskLevel

class MayaNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_SCAM_ALERTS = "channel_maya_scam_alerts"
        const val CHANNEL_SERVICE = "channel_maya_service"
        const val NOTIFICATION_ID_ALERT = 1001
        const val NOTIFICATION_ID_SERVICE = 1002
        const val NOTIFICATION_ID_INCOMING = 1003
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_SCAM_ALERTS,
                "Scam & Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications when evolving scam risk is detected on an active call."
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Call Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification displayed while MAYA is actively guarding calls."
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Shows a heads-up incoming call alert banner when a real call is ringing.
     * Allows user to immediately open MAYA Live Guardian over the dialer.
     */
    fun showIncomingCallGuardianAlert(
        callerNumber: String,
        displayName: String,
        riskLevel: RiskLevel,
        riskScore: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "LIVE_CALL")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🛡️ MAYA Call Guardian: $displayName"
        val subtitle = if (riskScore > 0) {
            "Risk Score: $riskScore/100 (${riskLevel.label}) • Tap to open Live Guardian"
        } else {
            "$callerNumber • Live Scam Protection Active (Tap to view)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SCAM_ALERTS)
            .setSmallIcon(R.drawable.ic_maya_logo)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_maya_logo, "Open Guardian", pendingIntent)

        notificationManager.notify(NOTIFICATION_ID_INCOMING, builder.build())
    }

    fun cancelIncomingCallAlert() {
        notificationManager.cancel(NOTIFICATION_ID_INCOMING)
    }

    fun showRiskAlert(
        riskLevel: RiskLevel,
        threatReason: String,
        score: Int
    ) {
        if (riskLevel == RiskLevel.LOW) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "LIVE_CALL")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (riskLevel) {
            RiskLevel.CRITICAL -> "⚠️ Critical Call Risk (Score: $score/100)"
            RiskLevel.HIGH -> "⚠️ High Risk Call Alert (Score: $score/100)"
            RiskLevel.MEDIUM -> "Caution: Suspicious Call Activity (Score: $score/100)"
            else -> "Call Guardian Update"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SCAM_ALERTS)
            .setSmallIcon(R.drawable.ic_maya_logo)
            .setContentTitle(title)
            .setContentText(threatReason)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_maya_logo, "View Warning", pendingIntent)

        notificationManager.notify(NOTIFICATION_ID_ALERT, builder.build())
    }

    fun buildServiceNotification(contentText: String) =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_maya_logo)
            .setContentTitle("MAYA Call Guardian")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
