package com.github.db1996.taskerha.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.db1996.taskerha.activities.screens.MainSettingsScreen
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            TaskerHaTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var topBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { topBarContent() }
    ) { padding ->
        MainSettingsScreen(
            modifier = Modifier.padding(padding),
            setTopBar = {  topBarContent = it }
        )
    }
}

