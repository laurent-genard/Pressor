# Project Plan

Build a production-ready Android automation app 'Pressor' (API 34+) using Kotlin, Jetpack Compose, and Material 3. 
Features:
1. Floating Overlay Controller: Compact overlay with draggable crosshair for target coordinates and a mini control bar (Start/Stop, Settings, Drag handle).
2. Accessibility Service Engine: Simulates continuous press-and-hold gestures using dispatchGesture() at the target coordinates. Includes randomized variance (+/- 5ms, +/- 3px).
3. Configuration UI: Input fields for Hold Duration, Break Duration, and Run Limit with validation and DataStore persistence. Status indicators for Accessibility and Overlay permissions.
4. Robust State Management: StateFlow or Intent actions for communication between services. Cancellation logic and edge case handling (rotation, drag bounds).
Permissions: SYSTEM_ALERT_WINDOW, BIND_ACCESSIBILITY_SERVICE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE.

## Project Brief

# Project Brief: Pressor

## Features
- **Floating Overlay Controller**: A persistent, draggable crosshair and mini control bar that allows users to set target coordinates and manage execution (Start/Stop/Settings) directly over other applications.
- **Randomized Gesture Engine**: An Accessibility-based engine that simulates human-like long-press gestures with built-in temporal (+/- 5ms) and spatial (+/- 3px) variance to ensure robust automation.
- **Configuration Dashboard**: A dedicated interface for fine-tuning Hold Duration, Break Duration, and Run Limits, with input validation and persistent storage.
- **Permission & Service Monitoring**: Integrated status tracking for required Accessibility and Overlay permissions, ensuring the app handles service lifecycle and edge cases (like rotation) gracefully.

## High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Jetpack Navigation 3 (State-driven)
- **Adaptive Strategy**: Compose Material Adaptive library for all layouts
- **Concurrency**: Kotlin Coroutines & StateFlow for gesture timing and inter-service communication
- **Persistence**: Jetpack DataStore (for configuration settings)
- **Core APIs**: Android Accessibility Service API, System Alert Window (Overlay), and Foreground Service (Special Use)

## Implementation Steps

### Task_1_Foundation_And_Settings: Configure project manifest, permissions, and DataStore persistence.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - AndroidManifest.xml includes SYSTEM_ALERT_WINDOW, BIND_ACCESSIBILITY_SERVICE, FOREGROUND_SERVICE, and FOREGROUND_SERVICE_SPECIAL_USE.
  - DataStore is implemented to persist Hold Duration, Break Duration, and Run Limit.
  - Base project structure with required dependencies (Navigation 3, Adaptive, DataStore) is set up.
- **StartTime:** 2026-09-07 16:08:36 CEST

### Task_2_Accessibility_Engine: Implement the Accessibility Service and Gesture Simulation Engine.
- **Status:** PENDING
- **Acceptance Criteria:**
  - PressorAccessibilityService is implemented and correctly registered.
  - Gesture simulation uses dispatchGesture() with randomized spatial (+/- 3px) and temporal (+/- 5ms) variance.
  - StateFlow or Intent-based communication is established to start/stop the engine.

### Task_3_Overlay_Controller: Implement the Foreground Service and Draggable Floating Overlay UI.
- **Status:** PENDING
- **Acceptance Criteria:**
  - ForegroundService (SPECIAL_USE) manages the overlay lifecycle.
  - Draggable crosshair and mini control bar UI are built using Jetpack Compose.
  - Overlay correctly updates target coordinates and triggers engine start/stop.

### Task_4_Dashboard_And_UI: Build the Configuration Dashboard and Permission Management UI.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Main UI uses Material 3 and Navigation 3 with adaptive layouts.
  - Settings input fields include validation and persist to DataStore.
  - Permission status indicators for Accessibility and Overlay are functional.

### Task_5_Run_And_Verify: Final integration, stability check, and requirement verification.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Application builds successfully and does not crash during gesture execution or screen rotation.
  - Gestures are accurately dispatched at the crosshair coordinates.
  - Critic_agent verifies application stability, alignment with user requirements, and UI fidelity.
  - Make sure all existing tests pass.

