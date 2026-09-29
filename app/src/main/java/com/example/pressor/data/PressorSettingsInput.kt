package com.example.pressor.data

import kotlin.math.roundToLong

internal object PressorSettingsInput {
    private const val MIN_INTERVAL_MILLIS = 100L
    private const val MAX_INTERVAL_MILLIS = 600_000L

    fun parseDurationMillis(value: String): Long? {
        val seconds = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (!seconds.isFinite() ||
            seconds < MIN_INTERVAL_MILLIS / 1000.0 ||
            seconds > MAX_INTERVAL_MILLIS / 1000.0
        ) {
            return null
        }
        return (seconds * 1000).roundToLong()
    }

    fun parseSettings(
        holdSeconds: String,
        breakSeconds: String,
        runLimit: String,
        targetX: String,
        targetY: String
    ): PressorSettings? {
        val settings = PressorSettings(
            holdDuration = parseDurationMillis(holdSeconds) ?: return null,
            breakDuration = parseDurationMillis(breakSeconds) ?: return null,
            runLimit = runLimit.trim().toLongOrNull() ?: return null,
            targetX = targetX.trim().toIntOrNull() ?: return null,
            targetY = targetY.trim().toIntOrNull() ?: return null
        )
        return settings.takeIf(::isValid)
    }

    fun isValid(settings: PressorSettings): Boolean =
        settings.holdDuration in MIN_INTERVAL_MILLIS..MAX_INTERVAL_MILLIS &&
            settings.breakDuration in MIN_INTERVAL_MILLIS..MAX_INTERVAL_MILLIS &&
            settings.runLimit >= 0L &&
            settings.targetX >= 0 &&
            settings.targetY >= 0
}
