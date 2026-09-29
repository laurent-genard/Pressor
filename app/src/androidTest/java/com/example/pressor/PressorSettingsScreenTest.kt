package com.example.pressor

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pressor.data.PressorSettings
import com.example.pressor.data.SettingsRepository
import com.example.pressor.ui.PressorSettingsScreen
import com.example.pressor.ui.theme.PressorTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PressorSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenShowsTheSetupRequirements() = withSettingsScreen {
        composeRule.onNodeWithText("Setup").assertIsDisplayed()
        composeRule.onNodeWithText("Display over other apps").assertIsDisplayed()
        composeRule.onNodeWithText("Accessibility service")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("press controls are not active yet", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun savePersistsAllSettingsToDataStore() = withSettingsScreen { repository ->
        composeRule.onNodeWithTag("hold-duration")
            .performScrollTo()
            .performTextReplacement("2.345")
        composeRule.onNodeWithTag("break-duration")
            .performScrollTo()
            .performTextReplacement("0.125")
        composeRule.onNodeWithTag("run-limit")
            .performScrollTo()
            .performTextReplacement("12")
        composeRule.onNodeWithTag("target-x")
            .performScrollTo()
            .performTextReplacement("320")
        composeRule.onNodeWithTag("target-y")
            .performScrollTo()
            .performTextReplacement("640")

        composeRule.onNodeWithTag("save-settings")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Settings saved").fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(
            PressorSettings(
                holdDuration = 2_345L,
                breakDuration = 125L,
                runLimit = 12L,
                targetX = 320,
                targetY = 640
            ),
            runBlocking { repository.settingsFlow.first() }
        )
    }

    @Test
    fun invalidDurationKeepsSaveDisabledAndDoesNotChangeStoredSettings() =
        withSettingsScreen { repository ->
            composeRule.onNodeWithTag("hold-duration")
                .performScrollTo()
                .performTextReplacement("0.05")

            composeRule.onNodeWithTag("save-settings")
                .performScrollTo()
                .assertIsNotEnabled()
            composeRule.onNodeWithText("Durations must be 0.1–600 seconds", substring = true)
                .assertIsDisplayed()

            assertEquals(PressorSettings(), runBlocking { repository.settingsFlow.first() })
        }

    private fun withSettingsScreen(test: (SettingsRepository) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = File(context.cacheDir, "settings-${UUID.randomUUID()}.preferences_pb")
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { testFile }
        val repository = SettingsRepository(dataStore)

        try {
            composeRule.setContent {
                PressorTheme {
                    PressorSettingsScreen(repository)
                }
            }
            test(repository)
            composeRule.waitForIdle()
        } finally {
            composeRule.setContent {}
            composeRule.waitForIdle()
            dataStoreScope.cancel()
            testFile.delete()
        }
    }
}
