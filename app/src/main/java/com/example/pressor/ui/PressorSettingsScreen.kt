package com.example.pressor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pressor.data.PressorSettings
import com.example.pressor.data.PressorSettingsInput
import com.example.pressor.data.SettingsRepository
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

@Composable
fun PressorSettingsScreen(settingsRepository: SettingsRepository) {
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = PressorSettings()
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var holdSeconds by remember(settings.holdDuration) {
        mutableStateOf(formatSeconds(settings.holdDuration))
    }
    var breakSeconds by remember(settings.breakDuration) {
        mutableStateOf(formatSeconds(settings.breakDuration))
    }
    var runLimit by remember(settings.runLimit) {
        mutableStateOf(settings.runLimit.toString())
    }
    var targetX by remember(settings.targetX) { mutableStateOf(settings.targetX.toString()) }
    var targetY by remember(settings.targetY) { mutableStateOf(settings.targetY.toString()) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val editedSettings = PressorSettingsInput.parseSettings(
        holdSeconds = holdSeconds,
        breakSeconds = breakSeconds,
        runLimit = runLimit,
        targetX = targetX,
        targetY = targetY
    )
    val settingsAreValid = editedSettings != null
    val settingsChanged = editedSettings != null && editedSettings != settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Pressor",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Choose and save your press settings.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsCard {
                    Text("Private by design", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pressor stores these settings on this device. It does not use the network, show over other apps, run in the background, or control other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsCard {
                    Text("Press settings", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Times are in seconds. Set a run limit of 0 for no limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = holdSeconds,
                        onValueChange = { holdSeconds = it },
                        modifier = Modifier.fillMaxWidth().testTag("hold-duration"),
                        label = { Text("Hold duration (seconds)") },
                        supportingText = { Text("0.1 to 600 seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = breakSeconds,
                        onValueChange = { breakSeconds = it },
                        modifier = Modifier.fillMaxWidth().testTag("break-duration"),
                        label = { Text("Break duration (seconds)") },
                        supportingText = { Text("0.1 to 600 seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = runLimit,
                        onValueChange = { runLimit = it },
                        modifier = Modifier.fillMaxWidth().testTag("run-limit"),
                        label = { Text("Run limit") },
                        supportingText = { Text("0 means unlimited") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text(
                        "Target position",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Screen pixels from the top-left corner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = targetX,
                            onValueChange = { targetX = it },
                            modifier = Modifier.weight(1f).testTag("target-x"),
                            label = { Text("X") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = targetY,
                            onValueChange = { targetY = it },
                            modifier = Modifier.weight(1f).testTag("target-y"),
                            label = { Text("Y") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Button(
                        onClick = {
                            val newSettings = checkNotNull(editedSettings)
                            coroutineScope.launch {
                                val message = try {
                                    settingsRepository.updateSettings(newSettings)
                                    "Settings saved"
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    "Couldn't save settings. Please try again."
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        enabled = settingsAreValid && settingsChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("save-settings")
                    ) {
                        Text("Save settings")
                    }
                    TextButton(
                        onClick = { showResetConfirmation = true },
                        modifier = Modifier.fillMaxWidth().testTag("reset-settings")
                    ) {
                        Text("Reset to defaults")
                    }
                    if (!settingsAreValid) {
                        Text(
                            "Enter valid values. Durations must be 0.1 to 600 seconds; other values must be zero or greater.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset settings?") },
            text = { Text("Your saved values will be replaced with the defaults.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    coroutineScope.launch {
                        val message = try {
                            settingsRepository.updateSettings(PressorSettings())
                            "Settings reset to defaults"
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            "Couldn't reset settings. Please try again."
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private fun formatSeconds(durationMillis: Long): String =
    String.format(java.util.Locale.getDefault(), "%.3f", durationMillis / 1000.0)
