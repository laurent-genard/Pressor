package com.example.pressor.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun updateSettingsPersistsAllValues() = withRepository { repository ->
        val expected = PressorSettings(
            holdDuration = 2_345L,
            breakDuration = 125L,
            runLimit = 12L,
            targetX = 320,
            targetY = 640
        )

        runBlocking { repository.updateSettings(expected) }

        assertEquals(expected, runBlocking { repository.settingsFlow.first() })
    }

    @Test
    fun updateSettingsRejectsInvalidValuesWithoutChangingDefaults() = withRepository { repository ->
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateSettings(PressorSettings(holdDuration = 99L)) }
        }

        assertEquals(PressorSettings(), runBlocking { repository.settingsFlow.first() })
    }

    @Test
    fun updateHoldDurationPersistsValue() = withRepository { repository ->
        runBlocking { repository.updateHoldDuration(2_000L) }
        assertEquals(2_000L, runBlocking { repository.settingsFlow.first().holdDuration })
    }

    @Test
    fun updateBreakDurationPersistsValue() = withRepository { repository ->
        runBlocking { repository.updateBreakDuration(750L) }
        assertEquals(750L, runBlocking { repository.settingsFlow.first().breakDuration })
    }

    @Test
    fun updateRunLimitPersistsValue() = withRepository { repository ->
        runBlocking { repository.updateRunLimit(5L) }
        assertEquals(5L, runBlocking { repository.settingsFlow.first().runLimit })
    }

    @Test
    fun updateTargetPositionPersistsBothCoordinates() = withRepository { repository ->
        runBlocking { repository.updateTargetPosition(123, 456) }
        val settings = runBlocking { repository.settingsFlow.first() }
        assertEquals(123, settings.targetX)
        assertEquals(456, settings.targetY)
    }

    private fun withRepository(test: (SettingsRepository) -> Unit) {
        val file = File(System.getProperty("java.io.tmpdir"), "pressor-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        try {
            test(SettingsRepository(dataStore))
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
