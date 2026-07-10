package com.example.webcontrol.data

data class BlockSettings(
    val youtubeShortBlocked: Boolean = true,
    val instagramReelsBlocked: Boolean = true,
    val gracePeriodMinutes: Int = 5,
    val unlockDurationMinutes: Int = 5,
    val timerSeconds: Int = 10,
    val sessionIdleTimeoutMinutes: Int = 30,
    val reasonRequired: Boolean = true
)
