package com.example.webcontrol.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webcontrol.data.BlockSettings
import com.example.webcontrol.data.SessionData
import com.example.webcontrol.logic.URLBlockingManager
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    sessionData: SessionData,
    blockSettings: BlockSettings,
    onBlockDetected: (url: String) -> Unit,
    onSessionExpired: () -> Unit
) {
    var currentUrl by remember { mutableStateOf("https://www.youtube.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isBlocked by remember { mutableStateOf(false) }
    var blockedUrl by remember { mutableStateOf("") }
    var gracePeriodActive by remember { mutableStateOf(true) }
    var timeRemainingSeconds by remember { mutableStateOf(blockSettings.gracePeriodMinutes * 60) }

    // Track grace period countdown
    LaunchedEffect(Unit) {
        while (gracePeriodActive && timeRemainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemainingSeconds--
            if (timeRemainingSeconds <= 0) {
                gracePeriodActive = false
                android.util.Log.d("BrowserScreen", "Grace period expired!")
            }
        }
    }

    // Continuous check for Shorts/Reels after grace period expires
    LaunchedEffect(gracePeriodActive, webView) {
        if (!gracePeriodActive && webView != null && !isBlocked) {
            while (!gracePeriodActive && !isBlocked) {
                // Check the actual WebView URL (more reliable than state)
                val webViewUrl = webView?.url ?: ""
                android.util.Log.d("BrowserScreen", "CHECK (grace expired): $webViewUrl")
                
                if (webViewUrl.isNotEmpty() && URLBlockingManager.isBlockedURL(webViewUrl, blockSettings)) {
                    android.util.Log.d("BrowserScreen", "BLOCKING active content: $webViewUrl")
                    isBlocked = true
                    blockedUrl = webViewUrl
                    onBlockDetected(webViewUrl)
                    break
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Compact URL Bar - optimized for small screens
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6200EE))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear button
            IconButton(
                onClick = { currentUrl = "" },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Clear, "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // URL TextField
            OutlinedTextField(
                value = currentUrl,
                onValueChange = { currentUrl = it },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(Color.White, shape = RoundedCornerShape(4.dp)),
                textStyle = LocalTextStyle.current.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                singleLine = true,
                placeholder = { Text("URL", fontSize = MaterialTheme.typography.labelSmall.fontSize) }
            )

            // Refresh button
            IconButton(
                onClick = {
                    val urlToLoad = if (currentUrl.startsWith("http")) {
                        currentUrl
                    } else {
                        "https://$currentUrl"
                    }
                    webView?.loadUrl(urlToLoad)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, "Load", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Grace period indicator
        if (gracePeriodActive) {
            Text(
                text = "Grace Period: ${timeRemainingSeconds / 60}:${String.format("%02d", timeRemainingSeconds % 60)} remaining",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50))
                    .padding(8.dp),
                color = Color.White,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
        }

        // Navigation controls - compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webView?.goBack() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back", modifier = Modifier.size(16.dp))
            }

            Text(
                text = currentUrl.take(20) + if (currentUrl.length > 20) "..." else "",
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
        }

        // WebView - takes most of the space, but leaves room for bottom nav
        if (!isBlocked) {
            AndroidView(
                modifier = Modifier.weight(0.85f).fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null) {
                                    currentUrl = url
                                    android.util.Log.d("BrowserScreen", "Loading URL: $url | Grace Period: $gracePeriodActive")
                                    
                                    // Check if URL should be blocked
                                    if (URLBlockingManager.isBlockedURL(url, blockSettings)) {
                                        // Block if grace period expired
                                        if (!gracePeriodActive) {
                                            android.util.Log.d("BrowserScreen", "BLOCKING (grace period expired): $url")
                                            isBlocked = true
                                            blockedUrl = url
                                            onBlockDetected(url)
                                            return true
                                        } else {
                                            android.util.Log.d("BrowserScreen", "ALLOWING during grace period: $url")
                                        }
                                    }
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) {
                                    currentUrl = url
                                    android.util.Log.d("BrowserScreen", "Page finished: $url | Grace Period: $gracePeriodActive")
                                    
                                    // Check again after page finishes loading
                                    if (URLBlockingManager.isBlockedURL(url, blockSettings)) {
                                        if (!gracePeriodActive) {
                                            android.util.Log.d("BrowserScreen", "BLOCKING on finish (grace period expired): $url")
                                            isBlocked = true
                                            blockedUrl = url
                                            onBlockDetected(url)
                                        }
                                    }
                                }
                            }
                        }
                        loadUrl(currentUrl)
                    }
                }
            )
        } else {
            BlockedOverlay(
                onUnlock = { reason ->
                    isBlocked = false
                },
                onGoBack = {
                    isBlocked = false
                    webView?.goBack()
                },
                modifier = Modifier.weight(0.85f).fillMaxWidth()
            )
        }
    }
}
