package com.example.webcontrol.data

data class SessionData(
    val sessionId: String = System.currentTimeMillis().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),
    val gracePeriodEndTime: Long = System.currentTimeMillis() + (5 * 60 * 1000), // 5 minutes default
    val currentUnlockEndTime: Long? = null,
    val blockEvents: MutableList<BlockEvent> = mutableListOf(),
    val isActive: Boolean = true
) {
    fun isGracePeriodActive(): Boolean {
        return System.currentTimeMillis() < gracePeriodEndTime
    }

    fun isUnlocked(): Boolean {
        return currentUnlockEndTime?.let { System.currentTimeMillis() < it } ?: false
    }

    fun updateLastActivity() {
        // This will be a mutable reference to update time
    }
}
