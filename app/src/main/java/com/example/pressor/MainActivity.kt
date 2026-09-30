package com.example.pressor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pressor.data.SettingsRepository
import com.example.pressor.ui.PressorSettingsScreen
import com.example.pressor.ui.theme.PressorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            PressorTheme {
                PressorSettingsScreen(settingsRepository)
            }
        }
    }
}
