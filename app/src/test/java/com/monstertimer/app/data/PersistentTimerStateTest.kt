package com.monstertimer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PersistentTimerState data class
 */
class PersistentTimerStateTest {

    @Test
    fun `getAdjustedRemainingMillis returns original when not active`() {
        val state = PersistentTimerState(
            remainingMillis = 60000L,  // 1 minute
            lastUpdatedTimestamp = System.currentTimeMillis() - 30000L,  // 30 seconds ago
            isActive = false
        )
        
        // When not active, should return original remaining time
        assertEquals(60000L, state.getAdjustedRemainingMillis())
    }

    @Test
    fun `getAdjustedRemainingMillis subtracts elapsed time when active`() {
        val now = System.currentTimeMillis()
        val elapsedMs = 10000L  // 10 seconds elapsed
        
        val state = PersistentTimerState(
            remainingMillis = 60000L,  // 1 minute
            lastUpdatedTimestamp = now - elapsedMs,
            isActive = true
        )
        
        val adjusted = state.getAdjustedRemainingMillis()
        
        // Should be approximately 50 seconds (60 - 10)
        // Allow small tolerance for test execution time
        assertTrue(adjusted in 49900L..50100L)
    }

    @Test
    fun `getAdjustedRemainingMillis does not go negative`() {
        val state = PersistentTimerState(
            remainingMillis = 10000L,  // 10 seconds
            lastUpdatedTimestamp = System.currentTimeMillis() - 60000L,  // 1 minute ago
            isActive = true
        )
        
        // Should not go below 0
        assertEquals(0L, state.getAdjustedRemainingMillis())
    }

    @Test
    fun `getAdjustedRemainingMillis handles zero remaining time`() {
        val state = PersistentTimerState(
            remainingMillis = 0L,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            isActive = true
        )
        
        assertEquals(0L, state.getAdjustedRemainingMillis())
    }

    @Test
    fun `getAdjustedRemainingMillis handles very recent update`() {
        val state = PersistentTimerState(
            remainingMillis = 60000L,
            lastUpdatedTimestamp = System.currentTimeMillis(),  // Just now
            isActive = true
        )
        
        val adjusted = state.getAdjustedRemainingMillis()
        
        // Should be approximately 60 seconds
        assertTrue(adjusted in 59900L..60000L)
    }

    @Test
    fun `copy preserves all fields correctly`() {
        val original = PersistentTimerState(
            remainingMillis = 120000L,
            lastUpdatedTimestamp = 1707410000000L,
            isActive = true
        )
        
        val copy = original.copy(isActive = false)
        
        assertEquals(original.remainingMillis, copy.remainingMillis)
        assertEquals(original.lastUpdatedTimestamp, copy.lastUpdatedTimestamp)
        assertFalse(copy.isActive)
    }

    @Test
    fun `default values work correctly`() {
        // Verify that reading uninitialized state behaves predictably
        val state = PersistentTimerState(
            remainingMillis = 0L,
            lastUpdatedTimestamp = 0L,
            isActive = false
        )
        
        assertEquals(0L, state.remainingMillis)
        assertEquals(0L, state.lastUpdatedTimestamp)
        assertFalse(state.isActive)
    }

    @Test
    fun `inactive timer with elapsed time is handled correctly`() {
        val state = PersistentTimerState(
            remainingMillis = 300000L,  // 5 minutes
            lastUpdatedTimestamp = System.currentTimeMillis() - 600000L,  // 10 minutes ago
            isActive = false  // But timer was not active
        )
        
        // Since inactive, should still return original remaining time
        assertEquals(300000L, state.getAdjustedRemainingMillis())
    }
}
