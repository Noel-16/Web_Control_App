package com.example.webcontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BlockedOverlay(
    onUnlock: (reason: String) -> Unit,
    onGoBack: () -> Unit,
    timerSeconds: Int = 10,
    isGracePeriod: Boolean = false,
    modifier: Modifier = Modifier
) {
    var reason by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(timerSeconds) }
    var showUnlockButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isGracePeriod) {
            for (i in timerSeconds downTo 0) {
                timeRemaining = i
                if (i == 0) showUnlockButton = true
                delay(1000)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF2D2D2D), shape = RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "ACCESS BLOCKED",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                textAlign = TextAlign.Center
            )

            if (isGracePeriod) {
                Text(
                    text = "Enjoy your first 5 minutes freely",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            } else {
                // Reason Input
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Why do you need this?", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A), shape = RoundedCornerShape(8.dp)),
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    singleLine = false,
                    maxLines = 3
                )

                // Timer
                if (!showUnlockButton) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Please wait",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$timeRemaining seconds",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                    }
                } else {
                    // Unlock Button
                    Button(
                        onClick = { onUnlock(reason) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        enabled = reason.isNotEmpty()
                    ) {
                        Text(
                            text = "UNLOCK FOR 5 MINUTES",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Note: You can access for 5 minutes, then blocking will resume",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Go Back Button (always visible)
            if (isGracePeriod) {
                Button(
                    onClick = onGoBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("GO BACK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
