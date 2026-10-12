package com.maya.callguardian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.maya.callguardian.ui.navigation.MayaNavGraph
import com.maya.callguardian.ui.navigation.Screen
import com.maya.callguardian.ui.theme.MayaTheme
import com.maya.callguardian.ui.theme.NavyDark
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val targetRoute = MutableStateFlow<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions updated
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MayaApplication

        // Request permissions safely with platform awareness
        requestPlatformPermissions()

        // Start ongoing call guardian service to prevent OS process killing
        try {
            val serviceIntent = Intent(this, com.maya.callguardian.call.service.MayaCallGuardianService::class.java).apply {
                action = com.maya.callguardian.call.service.MayaCallGuardianService.ACTION_START
            }
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Throwable) {
            // Service start deferred
        }

        val openScreen = intent?.getStringExtra("OPEN_SCREEN")
        val initialRoute = if (openScreen == "LIVE_CALL") Screen.LiveCall.route else Screen.Dashboard.route
        if (openScreen == "LIVE_CALL") {
            targetRoute.value = Screen.LiveCall.route
        }

        setContent {
            MayaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyDark
                ) {
                    MayaNavGraph(
                        app = app,
                        initialRoute = initialRoute,
                        targetRoute = targetRoute
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openScreen = intent.getStringExtra("OPEN_SCREEN")
        if (openScreen == "LIVE_CALL") {
            targetRoute.value = Screen.LiveCall.route
        }
    }

    private fun requestPlatformPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}
