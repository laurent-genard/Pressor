package com.example.pressor.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class PressorAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events
    }

    override fun onInterrupt() {
        // Handle interruption
    }
}