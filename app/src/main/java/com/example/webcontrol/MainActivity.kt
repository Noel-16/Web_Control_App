package com.example.webcontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.webcontrol.data.BlockSettings
import com.example.webcontrol.data.SessionData
import com.example.webcontrol.ui.screens.BrowserScreen
import com.example.webcontrol.ui.screens.SettingsScreen
import com.example.webcontrol.ui.theme.WebControlTheme
import kotlinx.coroutines.flow.collect


val Context.dataStore by preferencesDataStore(name = "app_settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebControlTheme {
                AppContent(this@MainActivity)
            }
        }
    }
}

@Composable
fun AppContent(context: Context) {
    val navController = rememberNavController()
    var currentScreen by remember { mutableStateOf("browser") }

    // Load settings from DataStore
    var blockSettings by remember {
        mutableStateOf(BlockSettings())
    }

    // Session management
    var currentSession by remember {
        mutableStateOf(SessionData())
    }

    LaunchedEffect(Unit) {
        context.dataStore.data.collect { preferences ->
            blockSettings = BlockSettings(
                youtubeShortBlocked = preferences[booleanPreferencesKey("youtube_short_blocked")] ?: true,
                instagramReelsBlocked = preferences[booleanPreferencesKey("instagram_reels_blocked")] ?: true,
                gracePeriodMinutes = preferences[intPreferencesKey("grace_period_minutes")] ?: 5,
                unlockDurationMinutes = preferences[intPreferencesKey("unlock_duration_minutes")] ?: 5,
                timerSeconds = preferences[intPreferencesKey("timer_seconds")] ?: 10
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                containerColor = Color(0xFF6200EE)
            ) {
                NavigationBarItem(
                    selected = currentScreen == "browser",
                    onClick = {
                        currentScreen = "browser"
                        navController.navigate("browser") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Browser",
                            tint = if (currentScreen == "browser") Color.White else Color.LightGray,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Text(
                            "Browser",
                            color = if (currentScreen == "browser") Color.White else Color.LightGray,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentScreen == "settings",
                    onClick = {
                        currentScreen = "settings"
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentScreen == "settings") Color.White else Color.LightGray,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            color = if (currentScreen == "settings") Color.White else Color.LightGray,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "browser"
            ) {
                composable("browser") {
                    BrowserScreen(
                        sessionData = currentSession,
                        blockSettings = blockSettings,
                        onBlockDetected = { url ->
                            // Handle block detection
                        },
                        onSessionExpired = {
                            // Reset session
                            currentSession = SessionData()
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        blockSettings = blockSettings,
                        onSettingsChanged = { newSettings ->
                            blockSettings = newSettings
                            // Save to DataStore
                            // TODO: Implement DataStore write
                        }
                    )
                }
            }
        }
    }
}