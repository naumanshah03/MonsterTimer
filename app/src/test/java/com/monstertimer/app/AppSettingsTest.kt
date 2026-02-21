package com.monstertimer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppSettings data class
 */
class AppSettingsTest {

    @Test
    fun `default constructor sets reasonable defaults`() {
        val settings = AppSettings()
        
        assertEquals(10, settings.timerMinutes)
        assertEquals("1234", settings.parentPin)
        assertTrue(settings.monsterPaths.isEmpty())
    }

    @Test
    fun `custom values are preserved`() {
        val paths = listOf("/path/to/monster1.png", "/path/to/monster2.jpg")
        val settings = AppSettings(
            timerMinutes = 15,
            parentPin = "5678",
            monsterPaths = paths
        )
        
        assertEquals(15, settings.timerMinutes)
        assertEquals("5678", settings.parentPin)
        assertEquals(2, settings.monsterPaths.size)
        assertEquals("/path/to/monster1.png", settings.monsterPaths[0])
    }

    @Test
    fun `copy creates proper copy with modified fields`() {
        val original = AppSettings(
            timerMinutes = 10,
            parentPin = "1234",
            monsterPaths = listOf("/path/to/monster.png")
        )
        
        val copy = original.copy(timerMinutes = 20)
        
        assertEquals(20, copy.timerMinutes)
        assertEquals(original.parentPin, copy.parentPin)
        assertEquals(original.monsterPaths, copy.monsterPaths)
    }

    @Test
    fun `monsterPaths can be modified via copy`() {
        val original = AppSettings(monsterPaths = listOf("/path/1.png"))
        
        val newPaths = original.monsterPaths.toMutableList()
        newPaths.add("/path/2.png")
        val copy = original.copy(monsterPaths = newPaths)
        
        assertEquals(1, original.monsterPaths.size)
        assertEquals(2, copy.monsterPaths.size)
    }

    @Test
    fun `empty monster paths list is valid`() {
        val settings = AppSettings(monsterPaths = emptyList())
        
        assertTrue(settings.monsterPaths.isEmpty())
    }

    @Test
    fun `timer minutes can be set to valid presets`() {
        val presets = listOf(5, 10, 15, 20, 30)
        
        presets.forEach { minutes ->
            val settings = AppSettings(timerMinutes = minutes)
            assertEquals(minutes, settings.timerMinutes)
        }
    }

    @Test
    fun `pin can be any 4 digit combination`() {
        val pins = listOf("0000", "1234", "9999", "0001")
        
        pins.forEach { pin ->
            val settings = AppSettings(parentPin = pin)
            assertEquals(pin, settings.parentPin)
            assertEquals(4, settings.parentPin.length)
        }
    }

    @Test
    fun `equality is based on all fields`() {
        val settings1 = AppSettings(
            timerMinutes = 10,
            parentPin = "1234",
            monsterPaths = listOf("/path/1.png")
        )
        
        val settings2 = AppSettings(
            timerMinutes = 10,
            parentPin = "1234",
            monsterPaths = listOf("/path/1.png")
        )
        
        val settings3 = AppSettings(
            timerMinutes = 15,  // Different
            parentPin = "1234",
            monsterPaths = listOf("/path/1.png")
        )
        
        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val settings1 = AppSettings(timerMinutes = 10, parentPin = "1234")
        val settings2 = AppSettings(timerMinutes = 10, parentPin = "1234")
        
        assertEquals(settings1.hashCode(), settings2.hashCode())
    }
}
