package com.example

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.StageBridgeScreen
import com.example.ui.StageBridgeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: StageBridgeViewModel

    // Permission launcher for Android 13+ Push Notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "StageBridge notification prompts enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Notifications disabled. StageBridge will still trigger sound, vibration, and full-screen overlay cues inside the app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the ViewModel nicely
        viewModel = ViewModelProvider(this)[StageBridgeViewModel::class.java]

        // Trigger request for Push Notifications if appropriate (Android 13 / Tiramisu / SDK 33 and up)
        checkAndRequestPushNotifications()

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    StageBridgeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAndRequestPushNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure our persistent quick reporting menu inside the notification drawer survives background states
        viewModel.refreshPersistentNotification()
    }
}
