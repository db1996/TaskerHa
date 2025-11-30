package com.github.db1996.taskerha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.db1996.taskerha.activities.screens.MainSettingsScreen
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme

class MainActivity : ComponentActivity() {
    private var hasNotificationPermission by mutableStateOf(false)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            hasNotificationPermission = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updatePermissionStatus()

        enableEdgeToEdge()

        setContent {
            TaskerHaTheme {
                MainScreen(
                    hasNotificationPermission = hasNotificationPermission,
                    onPermissionRequest = {
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
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

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
    ) { _ ->
        MainSettingsScreen()
    }
}

@Composable
fun NotificationPermissionBar(onPermissionRequest: () -> Unit) {
    BottomAppBar {
        Text(
            text = "Enable notifications to see errors.",
            modifier = Modifier
                .weight(1f)
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
