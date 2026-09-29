package com.example.pressor.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pressor.data.PressorSettings
import com.example.pressor.data.SettingsRepository
import com.example.pressor.service.PressorAccessibilityService
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import kotlin.math.roundToLong

private const val MIN_INTERVAL_MILLIS = 100L
private const val MAX_INTERVAL_MILLIS = 600_000L

@Composable
fun PressorSettingsScreen(settingsRepository: SettingsRepository) {
    val context = LocalContext.current
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

    val lifecycleOwner = LocalLifecycleOwner.current
    var setupRefreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) setupRefreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val overlayPermissionGranted = remember(setupRefreshKey) {
        Settings.canDrawOverlays(context)
    }
    val accessibilityServiceEnabled = remember(setupRefreshKey) {
        val expectedService = ComponentName(context, PressorAccessibilityService::class.java)
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        accessibilityManager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { service ->
                val serviceInfo = service.resolveInfo.serviceInfo
                ComponentName(serviceInfo.packageName, serviceInfo.name) == expectedService
            } == true
    }

    val holdMillis = parseSeconds(holdSeconds)
    val breakMillis = parseSeconds(breakSeconds)
    val parsedRunLimit = runLimit.trim().toLongOrNull()
    val parsedTargetX = targetX.trim().toIntOrNull()
    val parsedTargetY = targetY.trim().toIntOrNull()
    val settingsAreValid = holdMillis != null &&
        breakMillis != null &&
        parsedRunLimit != null && parsedRunLimit >= 0L &&
        parsedTargetX != null && parsedTargetX >= 0 &&
        parsedTargetY != null && parsedTargetY >= 0
    val settingsChanged = holdMillis != settings.holdDuration ||
        breakMillis != settings.breakDuration ||
        parsedRunLimit != settings.runLimit ||
        parsedTargetX != settings.targetX ||
        parsedTargetY != settings.targetY

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
                        text = "Set up permissions and choose your press settings.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SetupCard(
                    overlayPermissionGranted = overlayPermissionGranted,
                    accessibilityServiceEnabled = accessibilityServiceEnabled,
                    onRequestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onOpenAccessibilitySettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
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
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hold duration (seconds)") },
                        supportingText = { Text("0.1 to 600 seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = breakSeconds,
                        onValueChange = { breakSeconds = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Break duration (seconds)") },
                        supportingText = { Text("0.1 to 600 seconds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = runLimit,
                        onValueChange = { runLimit = it },
                        modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.weight(1f),
                            label = { Text("X") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = targetY,
                            onValueChange = { targetY = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Y") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Button(
                        onClick = {
                            val newSettings = PressorSettings(
                                holdDuration = holdMillis!!,
                                breakDuration = breakMillis!!,
                                runLimit = parsedRunLimit!!,
                                targetX = parsedTargetX!!,
                                targetY = parsedTargetY!!
                            )
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
                    ) {
                        Text("Save settings")
                    }
                    if (!settingsAreValid) {
                        Text(
                            "Enter valid values. Durations must be 0.1–600 seconds; other values must be zero or greater.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(
    overlayPermissionGranted: Boolean,
    accessibilityServiceEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    SettingsCard {
        Text("Setup", style = MaterialTheme.typography.titleLarge)
        Text(
            "Pressor needs these system permissions for floating controls and screen interaction.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        SetupRequirement(
            title = "Display over other apps",
            enabled = overlayPermissionGranted,
            actionLabel = if (overlayPermissionGranted) "Granted" else "Allow",
            onAction = onRequestOverlayPermission
        )
        SetupRequirement(
            title = "Accessibility service",
            enabled = accessibilityServiceEnabled,
            actionLabel = if (accessibilityServiceEnabled) "Enabled" else "Open settings",
            onAction = onOpenAccessibilitySettings
        )

        Text(
            "The press controls are not active yet. Accessibility access can expose content from other apps; enable it only if you choose to use Pressor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SetupRequirement(
    title: String,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                if (enabled) "Ready" else "Required",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onAction, enabled = !enabled) {
            Text(actionLabel)
        }
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

private fun parseSeconds(value: String): Long? {
    val seconds = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (seconds < MIN_INTERVAL_MILLIS / 1000.0 || seconds > MAX_INTERVAL_MILLIS / 1000.0) {
        return null
    }
    return (seconds * 1000).roundToLong()
}

private fun formatSeconds(durationMillis: Long): String =
    String.format(java.util.Locale.getDefault(), "%.3f", durationMillis / 1000.0)
