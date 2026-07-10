package com.example.webcontrol.logic

object URLBlockingManager {
    
    fun isBlockedURL(url: String, settings: com.example.webcontrol.data.BlockSettings): Boolean {
        val lowerUrl = url.lowercase()
        
        if (settings.youtubeShortBlocked && isYoutubeShort(lowerUrl)) {
            android.util.Log.d("URLBlockingManager", "Blocked YouTube Short: $url")
            return true
        }
        
        if (settings.instagramReelsBlocked && isInstagramReel(lowerUrl)) {
            android.util.Log.d("URLBlockingManager", "Blocked Instagram Reel: $url")
            return true
        }
        
        android.util.Log.d("URLBlockingManager", "Allowed URL: $url")
        return false
    }
    
    private fun isYoutubeShort(url: String): Boolean {
        val isShort = url.contains("youtube.com/shorts") || 
               url.contains("youtu.be/shorts") ||
               url.contains("m.youtube.com/shorts") ||
               url.contains("/shorts/")
        if (isShort) {
            android.util.Log.d("URLBlockingManager", "Detected YouTube Short pattern in: $url")
        }
        return isShort
    }
    
    private fun isInstagramReel(url: String): Boolean {
        val isReel = url.contains("instagram.com/reels") ||
               url.contains("instagram.com/reel/") ||
               url.contains("www.instagram.com/reels") ||
               url.contains("/reels/")
        if (isReel) {
            android.util.Log.d("URLBlockingManager", "Detected Instagram Reel pattern in: $url")
        }
        return isReel
    }
    
    fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
