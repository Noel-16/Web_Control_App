package com.example.webcontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.webcontrol.data.BlockSettings

@Composable
fun SettingsScreen(
    blockSettings: BlockSettings,
    onSettingsChanged: (BlockSettings) -> Unit
) {
    var youtubeShortBlocked by remember { mutableStateOf(blockSettings.youtubeShortBlocked) }
    var instagramReelsBlocked by remember { mutableStateOf(blockSettings.instagramReelsBlocked) }
    var gracePeriodMinutes by remember { mutableStateOf(blockSettings.gracePeriodMinutes.toString()) }
    var unlockDurationMinutes by remember { mutableStateOf(blockSettings.unlockDurationMinutes.toString()) }
    var timerSeconds by remember { mutableStateOf(blockSettings.timerSeconds.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Blocking Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // YouTube Shorts Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Block YouTube Shorts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Prevent access to youtube.com/shorts",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = youtubeShortBlocked,
                    onCheckedChange = {
                        youtubeShortBlocked = it
                        onSettingsChanged(
                            blockSettings.copy(youtubeShortBlocked = it)
                        )
                    }
                )
            }
        }

        // Instagram Reels Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Block Instagram Reels",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Prevent access to instagram.com/reels",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = instagramReelsBlocked,
                    onCheckedChange = {
                        instagramReelsBlocked = it
                        onSettingsChanged(
                            blockSettings.copy(instagramReelsBlocked = it)
                        )
                    }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Duration Settings
        Text(
            text = "Timer Settings",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Grace Period
        OutlinedTextField(
            value = gracePeriodMinutes,
            onValueChange = {
                gracePeriodMinutes = it
                it.toIntOrNull()?.let { minutes ->
                    onSettingsChanged(
                        blockSettings.copy(gracePeriodMinutes = minutes)
                    )
                }
            },
            label = { Text("Grace Period (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Unlock Duration
        OutlinedTextField(
            value = unlockDurationMinutes,
            onValueChange = {
                unlockDurationMinutes = it
                it.toIntOrNull()?.let { minutes ->
                    onSettingsChanged(
                        blockSettings.copy(unlockDurationMinutes = minutes)
                    )
                }
            },
            label = { Text("Unlock Duration (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Timer Seconds
        OutlinedTextField(
            value = timerSeconds,
            onValueChange = {
                timerSeconds = it
                it.toIntOrNull()?.let { seconds ->
                    onSettingsChanged(
                        blockSettings.copy(timerSeconds = seconds)
                    )
                }
            },
            label = { Text("Wait Timer (seconds)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // Info Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD))
        ) {
            Text(
                text = "Grace Period: You can access freely for the specified minutes. After that, blocking activates with the wait timer.",
                fontSize = 12.sp,
                color = Color(0xFF1565C0),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
