package com.example.pressor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PressorSettingsInputTest {
    @Test
    fun parsesSecondsToMillisecondPrecisionWithDotOrCommaDecimal() {
        assertEquals(2_345L, PressorSettingsInput.parseDurationMillis("2.345"))
        assertEquals(125L, PressorSettingsInput.parseDurationMillis(" 0,125 "))
        assertEquals(100L, PressorSettingsInput.parseDurationMillis("0.1"))
        assertEquals(600_000L, PressorSettingsInput.parseDurationMillis("600"))
    }

    @Test
    fun rejectsInvalidOrOutOfRangeDurations() {
        listOf("", "abc", "NaN", "Infinity", "0.099", "600.001", "-1")
            .forEach { value ->
                assertNull("Expected '$value' to be rejected", PressorSettingsInput.parseDurationMillis(value))
            }
    }

    @Test
    fun parsesAllSettingsTogether() {
        val settings = PressorSettingsInput.parseSettings(
            holdSeconds = "2.345",
            breakSeconds = "0,125",
            runLimit = "12",
            targetX = "320",
            targetY = "640"
        )

        assertEquals(
            PressorSettings(
                holdDuration = 2_345L,
                breakDuration = 125L,
                runLimit = 12L,
                targetX = 320,
                targetY = 640
            ),
            settings
        )
    }

    @Test
    fun rejectsNegativeLimitsCoordinatesAndMalformedNumbers() {
        assertNull(parse(runLimit = "-1"))
        assertNull(parse(targetX = "-1"))
        assertNull(parse(targetY = "-1"))
        assertNull(parse(runLimit = "9223372036854775808"))
        assertNull(parse(targetX = "2147483648"))
        assertNull(parse(breakSeconds = "0"))
    }

    @Test
    fun validatesRepositorySettingsBoundaries() {
        assertTrue(PressorSettingsInput.isValid(PressorSettings()))
        assertTrue(PressorSettingsInput.isValid(PressorSettings(holdDuration = 100L)))
        assertTrue(PressorSettingsInput.isValid(PressorSettings(breakDuration = 600_000L)))
        assertFalse(PressorSettingsInput.isValid(PressorSettings(holdDuration = 99L)))
        assertFalse(PressorSettingsInput.isValid(PressorSettings(breakDuration = 600_001L)))
        assertFalse(PressorSettingsInput.isValid(PressorSettings(runLimit = -1L)))
        assertFalse(PressorSettingsInput.isValid(PressorSettings(targetX = -1)))
        assertFalse(PressorSettingsInput.isValid(PressorSettings(targetY = -1)))
    }

    private fun parse(
        holdSeconds: String = "1",
        breakSeconds: String = "0.5",
        runLimit: String = "0",
        targetX: String = "0",
        targetY: String = "0"
    ) = PressorSettingsInput.parseSettings(holdSeconds, breakSeconds, runLimit, targetX, targetY)
}
