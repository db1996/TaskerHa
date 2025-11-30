package com.example.taskerha

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.taskerha.ui.theme.TaskerHaTheme

class MainActivity : ComponentActivity() {

    // 1. Create a state variable to track if permission is granted.
    // This will recompose the UI when the value changes.
    private var hasNotificationPermission by mutableStateOf(false)

    // 2. Register the Activity Result Launcher for permission requests.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            hasNotificationPermission = isGranted
            // The state change will automatically update the UI.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Check the initial permission status on create.
        updatePermissionStatus()

        enableEdgeToEdge()

        setContent {
            TaskerHaTheme {
                // Pass the permission status and the launcher function to the UI
                MainScreen(
                    hasNotificationPermission = hasNotificationPermission,
                    onPermissionRequest = {
                        // This lambda will be called by the button's onClick
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 4. Also check when the user returns to the app, in case they changed it in settings.
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older versions, permission is granted by default.
            true
        }
    }
}

// 5. Create a new Composable for the main screen structure
@Composable
fun MainScreen(hasNotificationPermission: Boolean, onPermissionRequest: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show the bottom bar if the permission is NOT granted
            if (!hasNotificationPermission) {
                NotificationPermissionBar(onPermissionRequest = onPermissionRequest)
            }
        }
    ) { innerPadding ->
        // The main content of your app
        HaSetupScreen()
    }
}

// 6. This is the new BottomAppBar Composable
@Composable
fun NotificationPermissionBar(onPermissionRequest: () -> Unit) {
    val context = LocalContext.current

    BottomAppBar {
        Text(
            text = "Enable notifications to see errors.",
            modifier = Modifier
                .weight(1f) // Takes up available space
                .padding(horizontal = 16.dp)
        )
        Button(
            onClick = onPermissionRequest,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Text("Enable")
        }
    }
}
