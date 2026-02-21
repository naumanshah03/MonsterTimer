package com.monstertimer.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for UsageStats data class
 */
class UsageStatsTest {

    @Test
    fun `addWatchTime increases total seconds`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 100L,
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        val updated = stats.addWatchTime(50L)
        
        assertEquals(150L, updated.totalSecondsWatched)
    }

    @Test
    fun `addWatchTime with zero seconds preserves original`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 100L,
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        val updated = stats.addWatchTime(0L)
        
        assertEquals(100L, updated.totalSecondsWatched)
    }

    @Test
    fun `incrementMonsterShown increases counter`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 0L,
            timesMonsterShown = 5,
            timesStoppedEarly = 0
        )
        
        val updated = stats.incrementMonsterShown()
        
        assertEquals(6, updated.timesMonsterShown)
    }

    @Test
    fun `incrementStoppedEarly increases counter`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 0L,
            timesMonsterShown = 0,
            timesStoppedEarly = 3
        )
        
        val updated = stats.incrementStoppedEarly()
        
        assertEquals(4, updated.timesStoppedEarly)
    }

    @Test
    fun `formattedWatchTime displays minutes only when under an hour`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 1800L, // 30 minutes
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        assertEquals("30m", stats.formattedWatchTime)
    }

    @Test
    fun `formattedWatchTime displays hours and minutes when over an hour`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 5400L, // 1 hour 30 minutes
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        assertEquals("1h 30m", stats.formattedWatchTime)
    }

    @Test
    fun `formattedWatchTime displays zero minutes for no watch time`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 0L,
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        assertEquals("0m", stats.formattedWatchTime)
    }

    @Test
    fun `formattedWatchTime handles large values correctly`() {
        val stats = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 10800L, // 3 hours
            timesMonsterShown = 0,
            timesStoppedEarly = 0
        )
        
        assertEquals("3h 0m", stats.formattedWatchTime)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = UsageStats(
            date = "2026-02-08",
            totalSecondsWatched = 100L,
            timesMonsterShown = 5,
            timesStoppedEarly = 2
        )
        
        val updated = original.addWatchTime(50L)
        
        assertEquals("2026-02-08", updated.date)
        assertEquals(5, updated.timesMonsterShown)
        assertEquals(2, updated.timesStoppedEarly)
    }
}
