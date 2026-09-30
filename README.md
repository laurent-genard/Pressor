# Pressor

Pressor is an Android settings companion built with Kotlin, Jetpack Compose, Material 3, and Preferences DataStore.

> **Current status:** Pressor lets you edit, save, and reset local configuration values. The app requests no user-granted or sensitive Android permissions, has no network access, starts no Pressor background service, and cannot control other apps. Automation is not implemented.

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

The app currently provides one configuration screen. A user can edit and persist valid settings or confirm a reset to the defaults. Settings remain in app-private Preferences DataStore. Android backup is disabled for the application.

The following project-plan items are still pending:

- A foreground service that creates and manages a draggable floating controller (requires a separate product and safety review before implementation).
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
| `app/src/main/AndroidManifest.xml` | App metadata and launcher activity. The app currently declares no permissions or services. |
| `app/src/main/java/com/example/pressor/MainActivity.kt` | Creates the settings repository and hosts the Compose screen. |
| `app/src/main/java/com/example/pressor/ui/PressorSettingsScreen.kt` | Settings form, local-only privacy note, save action, and confirmed reset action. |
| `app/src/main/java/com/example/pressor/ui/theme/` | Compose colors, typography, and Material theme. |
| `app/src/main/java/com/example/pressor/data/SettingsRepository.kt` | Settings data model, DataStore flow, and persistence operations. |
| `app/src/main/java/com/example/pressor/data/PressorSettingsInput.kt` | Parses and validates user-entered durations, run limit, and coordinates. |
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

The JVM tests cover duration parsing, validation boundaries, invalid input, DataStore updates, and persistence. The instrumentation tests cover the local-only privacy notice, saving form values, resetting settings, and invalid-duration behavior. Add a test with each behavior change; prefer small JVM tests for parsing/domain logic and instrumentation tests for Android/Compose behavior.

### Test requirements for future Android services

The current settings-only app contains no Pressor service classes, service declarations, or service-specific tests. Do not add a service to the packaged app as untested scaffolding. When a service is proposed and its behavior is implemented, its pull request must include both:

- **Unit tests** for service-independent logic extracted behind small interfaces, such as state transitions, scheduling, run limits, cancellation, retries, and error handling. These tests should use fake clocks, dispatchers, and collaborators so they run deterministically on the JVM.
- **Android instrumentation tests** for behavior that depends on the Android service lifecycle or platform APIs. Cover the relevant create/start/bind/stop or connect/disconnect paths, cleanup after interruption, and permission or notification behavior. Accessibility-service tests must verify event filtering and gesture-result handling without interacting with unrelated apps or reading their content.

Also test the merged manifest and service configuration: component export state, required permissions and foreground-service type, and the narrowest accessibility event/content capabilities needed. A service test suite is part of the definition of done, and the PR must pass the unit, coverage, lint, and API 35 instrumentation CI jobs before the service is merged or registered in the manifest.

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

### Download an installable test APK

After a push to `main`, the workflow publishes the debug-signed APK as the `pressor-debug-apk` artifact only after both required CI jobs pass. To download it, open **Actions → Android CI → the latest successful run on `main`**, then select `pressor-debug-apk` under **Artifacts**. The archive contains `app-debug.apk` and its `app-debug.apk.sha256` checksum. The artifact is retained for 90 days.

This debug APK is intended for personal testing and can be sideloaded on an Android phone that supports the app's minimum API level (24). It is signed with the temporary debug key from that CI run; a later run uses another key, so Android will not treat a later CI debug APK as an in-place update. Uninstall the old CI debug build before installing a newer one. The signed `v1.0.0` release APK will use the persistent release key configured through repository secrets and is the path for release updates.

### First release and Android signing

The first installable release is published from a `vMAJOR.MINOR.PATCH` tag that points to a commit on `main`. The tag workflow requires a release keystore provided as GitHub Actions secrets, builds and verifies the signed APK, waits for both CI jobs to pass, and publishes the APK as an asset on a GitHub Release. Do not create the version tag until signing secrets are configured and the reviewed release commit is on `main`.

Create a dedicated Android release keystore on a trusted machine. Keep the `.jks` file and its passwords under your control; Android updates must keep using the same signing key. Back up the keystore in at least two secure locations before publishing. Never commit it, put it in a build artifact, or share it in chat. If the key is lost, users cannot install an update signed with that key over the existing app.

For example, from a secure directory outside the repository, use `keytool` from the JDK and choose strong, unique passwords when prompted:

```powershell
keytool -genkeypair -v -keystore pressor-release.jks -alias pressor-release -keyalg RSA -keysize 4096 -validity 10000
```

Add these repository **Actions secrets** under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 encoding of the complete `.jks` file, without wrapping or line breaks. In PowerShell, copy it with `$releaseKey = [Convert]::ToBase64String([IO.File]::ReadAllBytes('.\pressor-release.jks')); Set-Clipboard -Value $releaseKey`. |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password selected during key generation. |
| `ANDROID_KEY_ALIAS` | `pressor-release` for the example command above. |
| `ANDROID_KEY_PASSWORD` | Password for the `pressor-release` key entry. |

Keep the keystore file in your secure backup location, not in this repository. The workflow writes a temporary copy under the GitHub runner's temporary directory, uses it only for the tagged build, and requires all four secrets. Untagged pull request and branch builds remain unsigned and do not need signing secrets.

For the initial `v1.0.0` release, merge the reviewed `develop` increment into `main`, ensure the release secrets are set, and create/push the tag from the resulting `main` commit:

```powershell
git switch main
git pull --ff-only origin main
git tag -a v1.0.0 -m "Pressor v1.0.0"
git push origin v1.0.0
```

CI verifies the tag is contained in `main`, runs the build, lint, unit and instrumentation tests plus coverage, verifies the APK signature, and only then creates the GitHub Release with the APK and `SHA256SUMS.txt` attached. Verify the APK against the checksum before installation. Because this is a new signing identity, an existing debug-signed Pressor installation cannot be updated in place with the release APK; uninstall the debug build first if Android reports a signature mismatch.

## Permissions and security

The app declares no user-granted or sensitive permissions: no `INTERNET`, overlay, accessibility, or foreground-service capability. AndroidX contributes an app-signature-protected permission for its internal dynamic receiver; it does not grant access to the network, screen, or other apps. The manifest merger output should be reviewed whenever dependencies or plugins change, because libraries may contribute manifest entries. The app disables Android backup so the saved settings remain on-device.

The build artifact is a debug APK signed with the standard local debug key. It is suitable for personal installation and evaluation, not trusted production distribution. A debug signature does not prove the software is bug-free or guarantee absolute harmlessness; review the source and merged manifest for the build you install. Pressor is intentionally limited to its own settings screen and local settings data.

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

### Coverage check fails

Open the JaCoCo HTML report under `app/build/reports/jacoco/jacocoDebugUnitTestReport/`, find the uncovered data-layer lines, and add tests for behavior rather than excluding classes. A legitimate source change can lower coverage; bring it back above 85% before merging.

## Known limitations and next steps

- Press automation, a target picker, and any cross-app interaction are not implemented.
- No start/stop state, run counter, timing loop, or cancellation model exists.
- Coordinates are persisted as raw nonnegative integers, with no screen-bound or display-density handling.
- The release build is not signed and has no distribution pipeline.
- The version catalog contains dependencies for planned features that are not all currently used.
- Coverage enforcement measures data-layer line coverage only; broader code coverage remains future work.

Recommended next implementation order: define the intended feature and safety boundaries; keep new behavior local to the app unless broader Android access is essential; implement and test the behavior in isolation; then review the merged manifest and release signing before distribution.
