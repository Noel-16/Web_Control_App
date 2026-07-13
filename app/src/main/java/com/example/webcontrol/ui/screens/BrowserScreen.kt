package com.example.webcontrol.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.URLUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.webkit.JavascriptInterface

// JavaScript interface for detecting blocked content
class ContentDetectionInterface(val onBlockedContentDetected: (String) -> Unit, val context: android.content.Context) {
    @JavascriptInterface
    fun onShortsDetected(url: String) {
        android.util.Log.d("BrowserScreen", "JavaScript detected Shorts: $url")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onBlockedContentDetected(url)
        }
    }
    
    @JavascriptInterface
    fun onReelsDetected(url: String) {
        android.util.Log.d("BrowserScreen", "JavaScript detected Reels: $url")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onBlockedContentDetected(url)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    sessionData: SessionData,
    blockSettings: BlockSettings,
    onBlockDetected: (url: String) -> Unit,
    onSessionExpired: () -> Unit,
    isVisible: Boolean = true  // Track if screen is currently visible
) {
    // rememberSaveable preserves currentUrl across Settings tab navigation (NavHost removes composable when switching tabs)
    var currentUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isBlocked by remember { mutableStateOf(false) }
    var blockedUrl by remember { mutableStateOf("") }
    var gracePeriodActive by remember { mutableStateOf(false) }
    var gracePeriodStarted by remember { mutableStateOf(false) }
    var gracePeriodAvailable by remember { mutableStateOf(true) } // Can grace period be used? (once per 30min idle)
    var timeRemainingSeconds by remember { mutableStateOf(blockSettings.gracePeriodMinutes * 60) }
    var extensionUnlockEndTime by remember { mutableStateOf<Long?>(null) } // Track unlock expiration
    var isInExtensionPeriod by remember { mutableStateOf(false) } // Track if we're in an extension period
    var extensionTimeRemainingSeconds by remember { mutableStateOf(0) } // Extension countdown display
    var lastActivityTime by remember { mutableStateOf(System.currentTimeMillis()) } // Track last grace/extension activation
    // Tracks whether JS detected Shorts/Reels on the current page (needed for blocking after grace on dynamic content)
    var jsDetectedBlockedContent by remember { mutableStateOf(false) }

    // Callback to start grace period from WebViewClient
    // Grace period ONLY activates if: gracePeriodAvailable=true AND not yet started
    val startGracePeriod = {
        if (gracePeriodAvailable && !gracePeriodStarted && !isInExtensionPeriod) {
            gracePeriodStarted = true
            gracePeriodActive = true
            gracePeriodAvailable = false  // Grace is now consumed, won't be available again until 30 min idle
            timeRemainingSeconds = blockSettings.gracePeriodMinutes * 60
            lastActivityTime = System.currentTimeMillis()
            android.util.Log.d("BrowserScreen", "Grace period started on blocked site access (available was true)")
        } else if (!gracePeriodAvailable || isInExtensionPeriod) {
            android.util.Log.d("BrowserScreen", "Grace period NOT started - available=$gracePeriodAvailable, inExtension=$isInExtensionPeriod")
            // Don't start grace, just block immediately
            isBlocked = true
            blockedUrl = webView?.url ?: ""
        }
    }

    // Callback to start extension unlock period
    val startExtensionPeriod = {
        android.util.Log.d("BrowserScreen", "EXTENSION PERIOD STARTED: ${blockSettings.unlockDurationMinutes} minutes")
        isInExtensionPeriod = true
        gracePeriodActive = false
        isBlocked = false
        extensionUnlockEndTime = System.currentTimeMillis() + (blockSettings.unlockDurationMinutes * 60 * 1000)
        extensionTimeRemainingSeconds = blockSettings.unlockDurationMinutes * 60
        lastActivityTime = System.currentTimeMillis()
    }

    // Track extension/unlock period expiration
    LaunchedEffect(extensionUnlockEndTime) {
        if (extensionUnlockEndTime != null) {
            while (isInExtensionPeriod && extensionUnlockEndTime != null) {
                val timeRemaining = extensionUnlockEndTime!! - System.currentTimeMillis()
                if (timeRemaining <= 0) {
                    android.util.Log.d("BrowserScreen", "Extension unlock period EXPIRED!")
                    isInExtensionPeriod = false
                    extensionUnlockEndTime = null
                    extensionTimeRemainingSeconds = 0
                    // Keep gracePeriodStarted=true so blocking check loop continues and can block again
                    break
                }
                extensionTimeRemainingSeconds = (timeRemaining / 1000).toInt()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Track 30-minute inactivity - after 30min idle, grace period becomes available again
    LaunchedEffect(lastActivityTime, gracePeriodAvailable) {
        if (!gracePeriodAvailable) {  // Only monitor if grace was just consumed
            while (!gracePeriodAvailable) {
                val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
                val thirtyMinutesMs = 30 * 60 * 1000
                
                android.util.Log.d("BrowserScreen", "Inactivity monitor: ${timeSinceLastActivity / 1000}s since last activity (need ${thirtyMinutesMs / 1000}s)")
                
                if (timeSinceLastActivity >= thirtyMinutesMs) {
                    android.util.Log.d("BrowserScreen", "30-minute idle detected - grace period now available again")
                    gracePeriodAvailable = true
                    gracePeriodStarted = false  // Reset so grace can be used again
                    break
                }
                kotlinx.coroutines.delay(10000)  // Check every 10 seconds
            }
        }
    }

    // Track grace period countdown - only runs when grace period is active
    LaunchedEffect(gracePeriodActive) {
        android.util.Log.d("BrowserScreen", "LaunchedEffect triggered: gracePeriodActive=$gracePeriodActive, timeRemaining=$timeRemainingSeconds")
        if (gracePeriodActive) {
            android.util.Log.d("BrowserScreen", "Starting countdown loop")
            while (gracePeriodActive && timeRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                timeRemainingSeconds--
                android.util.Log.d("BrowserScreen", "Countdown: $timeRemainingSeconds seconds left")
                if (timeRemainingSeconds <= 0) {
                    gracePeriodActive = false
                    android.util.Log.d("BrowserScreen", "Grace period EXPIRED! Setting gracePeriodActive=false")
                }
            }
        }
    }

    fun resolveInput(input: String): String? {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return null

        return if (trimmedInput.startsWith("http://") || trimmedInput.startsWith("https://")) {
            // Input has protocol, treat as URL
            trimmedInput
        } else {
            // No protocol, treat as search query
            "https://www.google.com/search?q=${android.net.Uri.encode(trimmedInput)}"
        }
    }

    fun loadInput(input: String) {
        val resolvedUrl = resolveInput(input) ?: return
        currentUrl = resolvedUrl
        webView?.loadUrl(resolvedUrl)
    }

    // Continuous check for Shorts/Reels after grace period expires
    LaunchedEffect(gracePeriodActive, webView) {
        android.util.Log.d("BrowserScreen", "Blocking check LaunchedEffect: gracePeriodStarted=$gracePeriodStarted, gracePeriodActive=$gracePeriodActive, webView=$webView")
        if (gracePeriodStarted && webView != null && !isBlocked) {
            while (gracePeriodStarted && !isBlocked) {
                // Check the actual WebView URL (more reliable than state)
                val webViewUrl = webView?.url ?: ""
                
                // Block if URL matches pattern OR if JS detected blocked content on current page
                val urlShouldBlock = URLBlockingManager.isBlockedURL(webViewUrl, blockSettings) || jsDetectedBlockedContent
                if (webViewUrl.isNotEmpty() && urlShouldBlock) {
                    // Only block if grace period has expired AND not in extension period
                    if (!gracePeriodActive && !isInExtensionPeriod) {
                        android.util.Log.d("BrowserScreen", "BLOCKING: Grace period expired for $webViewUrl (jsDetected=$jsDetectedBlockedContent)")
                        isBlocked = true
                        blockedUrl = webViewUrl
                        onBlockDetected(webViewUrl)
                        break
                    } else {
                        android.util.Log.d("BrowserScreen", "ALLOWING during grace or extension: $webViewUrl")
                    }
                } else {
                    android.util.Log.d("BrowserScreen", "URL check: $webViewUrl - gracePeriodActive=$gracePeriodActive - inExtension=$isInExtensionPeriod")
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
            // Back button
            IconButton(
                onClick = { webView?.goBack() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(16.dp))
            }

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
                    .height(56.dp)
                    .background(Color.White, shape = RoundedCornerShape(4.dp)),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = Color.Black
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { loadInput(currentUrl) }
                ),
                placeholder = { Text("URL", fontSize = MaterialTheme.typography.labelMedium.fontSize) }
            )

            // Refresh button
            IconButton(
                onClick = {
                    loadInput(currentUrl)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Search, "Search/Load", tint = Color.White, modifier = Modifier.size(16.dp))
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

        // Extension period indicator
        if (isInExtensionPeriod) {
            Text(
                text = "Extension: ${extensionTimeRemainingSeconds / 60}:${String.format("%02d", extensionTimeRemainingSeconds % 60)} remaining",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(8.dp),
                color = Color.White,
                fontSize = MaterialTheme.typography.labelSmall.fontSize
            )
        }

        // Navigation controls - compact (REMOVED - URL bar is the main control now)
        // This row is no longer needed as the URL bar at the top handles all navigation

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
                        
                        // Add JavaScript interface for content detection
                        val contentDetection = ContentDetectionInterface({ url ->
                            jsDetectedBlockedContent = true  // Track that current page has blocked content
                            if (!gracePeriodStarted) {
                                startGracePeriod()
                                android.util.Log.d("BrowserScreen", "Grace period started via JS detection: $url")
                            }
                        }, context)
                        addJavascriptInterface(contentDetection, "ContentDetection")
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null) {
                                    currentUrl = url
                                    android.util.Log.d("BrowserScreen", "shouldOverrideUrlLoading: $url | inExtension=$isInExtensionPeriod | gracePeriodActive=$gracePeriodActive")
                                    
                                    // Check if URL should be blocked
                                    val urlIsBlocked = URLBlockingManager.isBlockedURL(url, blockSettings)
                                    android.util.Log.d("BrowserScreen", "isBlockedURL returned: $urlIsBlocked for $url")
                                    
                                    if (urlIsBlocked) {
                                        // If we're in an extension/unlock period, allow access
                                        if (isInExtensionPeriod) {
                                            android.util.Log.d("BrowserScreen", "ALLOWING during unlock extension: $url")
                                            return false
                                        }
                                        
                                        // First time accessing blocked site - start grace period
                                        if (!gracePeriodStarted && !isInExtensionPeriod) {
                                            startGracePeriod()
                                            android.util.Log.d("BrowserScreen", "GRACE PERIOD STARTED: $url")
                                            return false
                                        }
                                        
                                        // Block if grace period expired (and not in extension)
                                        if (!gracePeriodActive && !isInExtensionPeriod) {
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
                                    // Reset JS-detected flag when navigating away from YouTube/Instagram
                                    if (!url.contains("youtube.com") && !url.contains("instagram.com")) {
                                        jsDetectedBlockedContent = false
                                        android.util.Log.d("BrowserScreen", "Navigated away from YT/IG - cleared jsDetectedBlockedContent")
                                    }
                                    android.util.Log.d("BrowserScreen", "Page finished: $url | Grace Period: $gracePeriodActive | InExtension: $isInExtensionPeriod")
                                    
                                    // Inject JS on ANY YouTube or Instagram page to detect Shorts/Reels dynamically
                                    if ((url.contains("youtube") || url.contains("instagram")) && !gracePeriodStarted && !isInExtensionPeriod) {
                                        val detectionScript = """
                                            (function() {
                                                // Check repeatedly for Shorts/Reels elements
                                                var checkCount = 0;
                                                var checkInterval = setInterval(function() {
                                                    checkCount++;
                                                    
                                                    // YouTube Shorts - check for player
                                                    if (document.querySelector('ytm-shorts-player') || 
                                                        document.querySelector('[data-component-type="SHORTS"]') ||
                                                        document.body.innerHTML.indexOf('shorts-player') > -1) {
                                                        ContentDetection.onShortsDetected(window.location.href);
                                                        clearInterval(checkInterval);
                                                        return;
                                                    }
                                                    
                                                    // Instagram Reels
                                                    if (document.querySelector('[role="region"][aria-label*="Reel"]') ||
                                                        document.querySelector('._a9-s') ||
                                                        document.body.innerHTML.indexOf('reels') > -1) {
                                                        ContentDetection.onReelsDetected(window.location.href);
                                                        clearInterval(checkInterval);
                                                        return;
                                                    }
                                                    
                                                    // Stop checking after 5 seconds if nothing found
                                                    if (checkCount > 50) {
                                                        clearInterval(checkInterval);
                                                    }
                                                }, 100);
                                            })();
                                        """.trimIndent()
                                        
                                        view?.evaluateJavascript(detectionScript) { _ -> }
                                        android.util.Log.d("BrowserScreen", "Injected detection script on: $url")
                                    }
                                    
                                    // Check URL patterns too
                                    if (URLBlockingManager.isBlockedURL(url, blockSettings)) {
                                        if (!gracePeriodStarted && !isInExtensionPeriod) {
                                            startGracePeriod()
                                            android.util.Log.d("BrowserScreen", "GRACE PERIOD STARTED on finish: $url")
                                            return
                                        }
                                        
                                        if (!gracePeriodActive && !isInExtensionPeriod) {
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
                    android.util.Log.d("BrowserScreen", "Unlock clicked - starting extension period")
                    startExtensionPeriod()
                },
                onGoBack = {
                    android.util.Log.d("BrowserScreen", "Go back clicked")
                    isBlocked = false
                    webView?.goBack()
                },
                modifier = Modifier.weight(0.85f).fillMaxWidth()
            )
        }
    }
}
