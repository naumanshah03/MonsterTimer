package com.monstertimer.app.data

import android.content.Context
import com.monstertimer.app.MonsterTimerApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tracks daily usage statistics for YouTube Shorts
 */
data class UsageStats(
    val date: String,          // Format: yyyy-MM-dd
    val totalSecondsWatched: Long,
    val timesMonsterShown: Int,
    val timesStoppedEarly: Int  // For reward tracking
) {
    companion object {
        private const val KEY_PREFIX = "usage_stats_"
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        fun getToday(context: Context): UsageStats {
            val today = LocalDate.now().format(dateFormatter)
            return load(context, today)
        }
        
        fun load(context: Context, date: String): UsageStats {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            val key = KEY_PREFIX + date
            val data = prefs.getString(key, null)
            
            return if (data != null) {
                val parts = data.split("|")
                UsageStats(
                    date = date,
                    totalSecondsWatched = parts.getOrNull(0)?.toLongOrNull() ?: 0L,
                    timesMonsterShown = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                    timesStoppedEarly = parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
            } else {
                UsageStats(date, 0L, 0, 0)
            }
        }
        
        fun save(context: Context, stats: UsageStats) {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            val key = KEY_PREFIX + stats.date
            val data = "${stats.totalSecondsWatched}|${stats.timesMonsterShown}|${stats.timesStoppedEarly}"
            prefs.edit().putString(key, data).apply()
        }
        
        /**
         * Get stats for the last N days
         */
        fun getWeeklyStats(context: Context): List<UsageStats> {
            val stats = mutableListOf<UsageStats>()
            val today = LocalDate.now()
            for (i in 0..6) {
                val date = today.minusDays(i.toLong()).format(dateFormatter)
                stats.add(load(context, date))
            }
            return stats
        }
    }
    
    fun addWatchTime(seconds: Long): UsageStats = copy(totalSecondsWatched = totalSecondsWatched + seconds)
    fun incrementMonsterShown(): UsageStats = copy(timesMonsterShown = timesMonsterShown + 1)
    fun incrementStoppedEarly(): UsageStats = copy(timesStoppedEarly = timesStoppedEarly + 1)
    
    val formattedWatchTime: String
        get() {
            val hours = totalSecondsWatched / 3600
            val minutes = (totalSecondsWatched % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}

/**
 * Timer state that persists across app restarts
 */
data class PersistentTimerState(
    val remainingMillis: Long,
    val lastUpdatedTimestamp: Long,
    val isActive: Boolean
) {
    companion object {
        private const val KEY_TIMER_STATE = "persistent_timer_state"
        
        fun load(context: Context): PersistentTimerState? {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            val data = prefs.getString(KEY_TIMER_STATE, null) ?: return null
            
            val parts = data.split("|")
            return PersistentTimerState(
                remainingMillis = parts.getOrNull(0)?.toLongOrNull() ?: 0L,
                lastUpdatedTimestamp = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
                isActive = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false
            )
        }
        
        fun save(context: Context, state: PersistentTimerState) {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            val data = "${state.remainingMillis}|${state.lastUpdatedTimestamp}|${state.isActive}"
            prefs.edit().putString(KEY_TIMER_STATE, data).apply()
        }
        
        fun clear(context: Context) {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            prefs.edit().remove(KEY_TIMER_STATE).apply()
        }
    }
    
    /**
     * Calculate actual remaining time considering elapsed time since last update
     */
    fun getAdjustedRemainingMillis(): Long {
        if (!isActive) return remainingMillis
        val elapsed = System.currentTimeMillis() - lastUpdatedTimestamp
        return (remainingMillis - elapsed).coerceAtLeast(0)
    }
}
