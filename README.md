# Pressor

Pressor is an Android application prototype for configuring repeated press-and-hold automation. The project uses Kotlin, Jetpack Compose, Material 3, Android Accessibility Services, a system overlay, and Preferences DataStore.

> **Current status:** The configuration screen, permission-status checks, settings validation, persistence, and their tests are implemented. The floating controller and gesture automation engine are not implemented yet. The two service classes are registration scaffolding; they do not currently display an overlay or dispatch gestures. The app is not ready to automate input in other apps.

## Contents

- [Project status and scope](#project-status-and-scope)
- [Requirements](#requirements)
- [Get the source and open the project](#get-the-source-and-open-the-project)
- [Build and run](#build-and-run)
- [Architecture and source map](#architecture-and-source-map)
- [Settings behavior](#settings-behavior)
- [Tests and coverage](#tests-and-coverage)
- [Development and integration workflow](#development-and-integration-workflow)
- [Continuous integration and releases](#continuous-integration-and-releases)
- [Permissions and security](#permissions-and-security)
- [Troubleshooting](#troubleshooting)
- [Known limitations and next steps](#known-limitations-and-next-steps)

## Project status and scope

The app currently provides one configuration screen. A user can inspect whether overlay and accessibility access are enabled, edit settings, and persist valid settings. The accessibility-status check updates when the activity resumes after returning from Android Settings.

The following project-plan items are still pending:

- A foreground service that creates and manages a draggable floating controller.
- A crosshair/target picker that records coordinates from the live display.
- Start/stop controls and communication between the overlay and accessibility service.
- Gesture timing, repeated dispatch, cancellation, and run-limit handling.
- Lifecycle handling for orientation, screen-size changes, process death, and permission revocation.
- Device-level verification of gesture behavior and accessibility policy review.

The project plan in `.agent/plan.md` records the original product brief and pending implementation tasks. Treat the source code and tests as authoritative when the plan and current implementation differ.

## Requirements

- Android Studio with Android SDK Platform 37 installed.
- JDK 17 for Gradle and Android Gradle Plugin 9.3.2.
- The Gradle Wrapper distribution specified in `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.5.0). Use the wrapper rather than installing another Gradle version.
- For emulator instrumentation tests: an Android emulator image for API 35, x86_64, Google APIs. CI uses a Pixel 6 emulator profile.
- For a physical-device run: an Android device with API 24 or later and USB debugging enabled.

The app's `minSdk` is 24, `targetSdk` is 37, and `compileSdk` is 37. The instrumentation runner is `androidx.test.runner.AndroidJUnitRunner`.

## Get the source and open the project

Clone the repository and select the branch you are working on:

```sh
git clone https://github.com/laurent-genard/Pressor.git
cd Pressor
git switch develop
```

Open the repository root in Android Studio and allow Gradle sync to finish. Install the Android SDK Platform 37 and accept the SDK licenses when prompted. Android Studio creates `local.properties` for the local SDK location; this file is machine-specific and must not be committed.

To create a local feature branch, start from the current integration branch:

```sh
git switch develop
git pull --ff-only origin develop
git switch -c feature/<issue>-<short-name>
```

Replace the placeholder with an issue number and a brief kebab-case description, for example `feature/42-overlay-controls`.

## Build and run

The commands below assume a shell opened at the repository root.

### Windows PowerShell

```powershell
.\gradlew.bat :app:assembleDebug
```

Install on the first connected device or running emulator:

```powershell
.\gradlew.bat :app:installDebug
```

### macOS or Linux

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

You can also select the `app` run configuration in Android Studio and run it on an emulator or connected device. The launcher activity is `com.example.pressor.MainActivity`.

The debug APK is written under `app/build/outputs/apk/debug/`. A release variant can be compiled with `:app:assembleRelease`; release signing and publication are not configured in this project.

## Architecture and source map

This is a single-module Android application (`:app`) with a single launcher activity. UI is built with Compose; settings persistence is backed by Preferences DataStore.

| Path | Responsibility |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | App metadata, permissions, launcher activity, accessibility-service registration, and special-use service declaration. |
| `app/src/main/java/com/example/pressor/MainActivity.kt` | Creates the settings repository and hosts the Compose screen. |
| `app/src/main/java/com/example/pressor/ui/PressorSettingsScreen.kt` | Settings form, input state, save action, permission status, and links to Android Settings. |
| `app/src/main/java/com/example/pressor/ui/theme/` | Compose colors, typography, and Material theme. |
| `app/src/main/java/com/example/pressor/data/SettingsRepository.kt` | Settings data model, DataStore flow, and persistence operations. |
| `app/src/main/java/com/example/pressor/data/PressorSettingsInput.kt` | Parses and validates user-entered durations, run limit, and coordinates. |
| `app/src/main/java/com/example/pressor/service/PressorAccessibilityService.kt` | Accessibility service registration scaffold. Event and interruption callbacks are currently empty. |
| `app/src/main/java/com/example/pressor/service/FloatingControlService.kt` | Service scaffold. It currently returns `START_STICKY` and does not create a notification or overlay. |
| `app/src/main/res/xml/accessibility_service_config.xml` | Accessibility capabilities and event/window-access declarations. |
| `app/src/test/` | JVM unit tests for input validation and settings persistence. |
| `app/src/androidTest/` | Compose UI tests running on Android. |
| `.github/workflows/android-ci.yml` | Pull-request checks, push checks, version-tag validation, and release APK artifact upload. |
| `CONTRIBUTING.md` | Short branch, pull-request, and repository-ruleset guide. |

### Settings data flow

1. `MainActivity` constructs `SettingsRepository` using the application context and passes it to `PressorSettingsScreen`.
2. The screen collects `settingsFlow` with lifecycle awareness and initializes editable fields from the stored settings.
3. `PressorSettingsInput` parses the fields together. Invalid or unchanged values disable the save action.
4. A save writes the complete settings object to DataStore and displays a success or failure snackbar.
5. The repository emits the persisted settings back to collectors.

Keep parsing and business rules outside composables where practical; this allows most behavior to be tested without launching Android UI. When adding state shared by the activity, overlay, or accessibility service, define ownership and lifecycle/cancellation behavior before wiring those components together.

### Dependencies

Versions are declared in `gradle/libs.versions.toml`. Gradle plugin and dependency repositories are declared in `settings.gradle.kts`; application configuration and dependencies are in `app/build.gradle.kts`.

The version catalog currently includes libraries for planned capabilities that are not all used by the source code yet. Before adding or upgrading dependencies, check actual usages, Android Gradle Plugin/Kotlin compatibility, minimum SDK, and CI behavior. Keep version changes deliberate and validate them through the full CI workflow.

## Settings behavior

Defaults are defined in `PressorSettings`:

| Setting | Default | Validation |
| --- | ---: | --- |
| Hold duration | 1,000 ms (1 second) | 0.1 through 600 seconds, rounded to milliseconds. |
| Break duration | 500 ms (0.5 seconds) | 0.1 through 600 seconds, rounded to milliseconds. |
| Run limit | 0 | Zero is described as unlimited; otherwise the value must be zero or greater. |
| Target X | 0 | Integer zero or greater. |
| Target Y | 0 | Integer zero or greater. |

Duration input accepts either a dot or comma as the decimal separator. Run limit and coordinates must parse as integers within their respective Kotlin numeric ranges. Settings are only saved as a complete validated object from the current UI. The repository's individual field-update methods predate the form validation and do not enforce the same constraints; review that API before reusing those methods in new features.

Target coordinates are currently stored as nonnegative integer values. They are labelled as screen pixels from the top-left, but there is not yet a target picker, display-bound validation, density conversion, or gesture consumer. Do not assume the current values are safe to dispatch to another app.

## Tests and coverage

Run unit tests and the enforced coverage check:

```sh
./gradlew :app:check
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:check
```

Run the Android instrumentation tests with an API 35 emulator already booted:

```sh
./gradlew :app:connectedDebugAndroidTest
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

The JVM tests cover duration parsing, validation boundaries, invalid input, DataStore updates, and persistence. The instrumentation tests cover setup requirements, saving form values, and invalid-duration behavior. Add a test with each behavior change; prefer small JVM tests for parsing/domain logic and instrumentation tests for Android/Compose behavior.

JaCoCo enforces at least **85% line coverage** over `com.example.pressor.data` when Gradle's `check` task runs. This is a data-layer threshold, not an 85% whole-app threshold: Compose UI, activity/service entry points, resources, and instrumentation execution are not included in the JaCoCo percentage. The CI instrumentation job verifies UI tests separately. Expand the coverage scope only when the added classes can be tested meaningfully, and do not exclude implemented business logic simply to satisfy the threshold.

Reports are generated under:

- Unit test results: `app/build/reports/tests/testDebugUnitTest/`
- JaCoCo XML/HTML: `app/build/reports/jacoco/jacocoDebugUnitTestReport/`
- Android lint: `app/build/reports/lint-results-debug.html`

The CI quality job uploads unit-test and JaCoCo reports as a 14-day artifact.

## Development and integration workflow

The repository uses a lightweight trunk-based integration flow with a `develop` integration branch and a `main` release branch:

```text
feature/<issue>-<name> -> pull request -> develop -> release pull request -> main -> vMAJOR.MINOR.PATCH tag
```

Use this sequence for normal work:

1. Create or select an issue with acceptance criteria and a test approach. Keep the work small enough to review as one increment.
2. Branch from the latest `develop` using `feature/<issue>-<short-name>` or `fix/<issue>-<short-name>`.
3. Implement the smallest complete change. Add or update tests and documentation in the same branch.
4. Use semantic commit subjects, for example `feat: add draggable target picker`, `fix: reject coordinates outside display`, `test: cover invalid duration input`, or `docs: explain local emulator setup`.
5. Push the branch and open a pull request into `develop`. Describe the behavior, link the issue, note risks, and state what you tested.
6. Address review feedback. Merge only after required CI checks pass and the required review is recorded.
7. Promote a reviewed sprint increment through a pull request from `develop` to `main`.
8. After the release commit is on `main`, create a `vMAJOR.MINOR.PATCH` tag on that commit. CI validates the tag and stores the release APK artifact.

For a production hotfix, branch from `main`, open a reviewed pull request into `main`, then merge or cherry-pick the fix back into `develop`. For dependent work that cannot yet target `develop`, use a stacked pull request against its feature-branch dependency; this workflow runs for PRs targeting `feat/**` and `feature/**`. Retarget it to `develop` after its base branch has merged.

Use Conventional Commit-style semantic prefixes (`feat`, `fix`, `docs`, `test`, `ci`, `build`, `refactor`, `chore`). Prefer squash merges for feature/fix pull requests so the integration history remains easy to scan. Do not push directly to `main`; repository rulesets should enforce that policy.

### Pull-request definition of done

- Acceptance criteria are met, and the implementation matches the actual scope of the issue.
- Tests cover the new behavior and relevant error/boundary cases.
- Documentation, user-facing strings, manifest declarations, and dependency changes are updated where needed.
- Android lint, unit tests, coverage verification, release compilation, and emulator instrumentation checks pass.
- The PR has a review, has no unresolved review threads, and communicates any remaining limitation or follow-up work.

## Continuous integration and releases

The workflow is `.github/workflows/android-ci.yml`.

### Triggers

- Pull requests targeting `develop`, `main`, `feat/**`, or `feature/**`.
- Pushes to `develop` or `main`.
- Version tags matching `v*`.
- Manual dispatch from the GitHub Actions page.

### Required CI jobs

| GitHub check name | What it runs |
| --- | --- |
| `Build, lint, unit tests, and coverage` | Gradle clean, Android lint, unit tests, the JaCoCo threshold, and release-variant compilation. On version tags it also verifies that the tagged commit is contained in `main` and uploads the APK artifact. |
| `Instrumentation tests (API 35)` | Boots an x86_64 API 35 Google APIs emulator and runs `connectedDebugAndroidTest`. |

In GitHub repository settings, configure branch rulesets for `develop` and `main`. Require pull requests, at least one approval, and both check names above; require branches to be up to date and block force pushes and deletion. Limit direct `main` updates to administrators. Rulesets are repository-host configuration and cannot be enabled by committing a workflow file. If the repository is operated by one developer, adjust the approval rule to match available reviewers without making CI optional.

To run the same quality job locally, including a clean build, lint, unit tests, coverage enforcement, and release compilation:

```sh
./gradlew --no-daemon clean :app:lintDebug :app:check :app:assembleRelease
```

```powershell
.\gradlew.bat --no-daemon clean :app:lintDebug :app:check :app:assembleRelease
```

The tag workflow creates an unsigned release APK artifact retained for 90 days. It does not create a GitHub Release, sign the APK, publish to an app store, or deploy the application. Configure release signing and publication separately before distributing production builds.

## Permissions and security

The manifest declares internet access, overlay access, accessibility-service binding, and foreground-service/special-use foreground-service permissions. The accessibility-service configuration allows gesture dispatch and window-content retrieval and subscribes to broad event types. This is sensitive access. Users should enable the service only when needed, and developers should narrow its event/content access to the minimum required before implementing automation.

Do not add credentials, signing keys, keystores, local SDK paths, or machine-specific Gradle/Android state to Git. Keep CI permissions minimal. Any future release-signing configuration must use protected GitHub secrets or a managed signing service and must never put the key in the repository.

## Troubleshooting

### Gradle cannot find Java or uses the wrong Java version

Check the runtime used by the wrapper:

```sh
java -version
```

Use JDK 17. In Android Studio, configure the Gradle JDK under **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle**. Restart the IDE or terminal after changing `JAVA_HOME`.

### Android SDK platform or licenses are missing

Install **Android SDK Platform 37** and accept the Android SDK licenses using Android Studio's SDK Manager. Check that `local.properties` points to the installed SDK. Do not commit `local.properties`.

### Gradle reports an SSL certificate/PKIX error on a managed Windows device

Some organizations inspect TLS connections and add their trusted root certificate to the Windows certificate store, but not to Java's default `cacerts`. If the root is already trusted by Windows, a PowerShell session can make Java use the Windows root store for that session:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=Windows-ROOT'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle'
.\gradlew.bat :app:check
```

This setting is Windows/JDK-specific and should not be added to CI or committed as a project-wide Gradle property. If it does not apply to your managed device, ask your administrator for the approved Java trust-store configuration; do not disable TLS verification or import an unverified certificate.

### Instrumentation tests cannot find a device

Start an API 35 emulator in Android Studio's Device Manager, confirm `adb devices` lists it as `device`, and rerun `connectedDebugAndroidTest`. The CI emulator is provisioned by the workflow and requires no local device.

### Accessibility or overlay status shows as disabled

These are special Android settings, not runtime permissions granted by a normal dialog. Open the corresponding system settings from the app and return to Pressor; the status is refreshed on activity resume. Remember that enabling the current accessibility service grants capabilities that the unfinished automation feature does not yet use.

### Coverage check fails

Open the JaCoCo HTML report under `app/build/reports/jacoco/jacocoDebugUnitTestReport/`, find the uncovered data-layer lines, and add tests for behavior rather than excluding classes. A legitimate source change can lower coverage; bring it back above 85% before merging.

## Known limitations and next steps

- No floating overlay is created, despite the service declaration.
- No accessibility gesture is dispatched; accessibility callbacks are empty.
- No start/stop state, run counter, timing loop, or cancellation model exists.
- Coordinates are persisted as raw nonnegative integers, with no screen-bound or display-density handling.
- The accessibility service requests broad event and window-content access; revisit this before release.
- The release build is not signed and has no distribution pipeline.
- The version catalog contains dependencies for planned features that are not all currently used.
- Coverage enforcement measures data-layer line coverage only; broader code coverage remains future work.

Recommended next implementation order: define coordinate semantics and gesture behavior; implement and test the gesture engine in isolation; implement the overlay lifecycle and permission handling; connect start/stop state with cancellation; then expand device tests, accessibility/privacy review, and release signing.
