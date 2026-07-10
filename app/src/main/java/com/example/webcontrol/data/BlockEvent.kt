package com.example.webcontrol.data

data class BlockEvent(
    val id: String = System.currentTimeMillis().toString(),
    val sessionId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val url: String = "",
    val reason: String = "",
    val unlockedFor: Long = 0, // duration in milliseconds
    val isExternal: Boolean = false
)
