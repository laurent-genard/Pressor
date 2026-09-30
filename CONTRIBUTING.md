# Development workflow

## Branches and pull requests

- `main` contains reviewed, releasable versions. Do not push directly to it.
- `develop` is the integration branch for the active sprint.
- Create short-lived branches from `develop`, named `feature/<issue>-<short-name>` or `fix/<issue>-<short-name>`.
- Open pull requests from those branches into `develop`. For a production hotfix, branch from `main` and open a pull request into `main`; merge the fix back into `develop` as well.
- For dependent work, a short-lived feature branch may target another feature branch; CI runs for these stacked pull requests too. Retarget it to `develop` after its base feature is merged.
- Promote a sprint increment by opening a pull request from `develop` into `main`. Tag the resulting commit as `vMAJOR.MINOR.PATCH` after review. The Android CI workflow validates version tags and retains the release APK as a build artifact.

Keep pull requests small enough to review within a sprint. Link the issue or sprint item, describe user-visible behavior and risk, and include the tests added or updated.

## Required repository settings

Workflow files cannot enforce GitHub branch protection by themselves. In repository settings, add rulesets for `develop` and `main`:

- Require pull requests and at least one approval; dismiss stale approvals after new commits.
- Require the `Build, lint, unit tests, and coverage` and `Instrumentation tests (API 35)` checks.
- Require the branch to be up to date before merging; block force pushes and deletion.
- Restrict direct pushes to `main` to repository administrators only, and use pull requests for normal changes.
- Allow squash merges for feature/fix PRs. Promote `develop` to `main` through a reviewed PR.

## Quality gate

Every pull request into `develop` or `main` runs Android lint, release compilation, JVM unit tests, the coverage threshold, and Compose instrumentation tests on an API 35 emulator. The current 85% line-coverage gate is scoped to the implemented `com.example.pressor.data` logic (input parsing and settings persistence). Android framework entry points, Compose UI, and placeholder service classes are not counted in that percentage; expand coverage and the gate as those behaviors gain meaningful implementations and tests. Do not exclude implemented business logic merely to satisfy the threshold.

Run the same checks locally with:

```powershell
./gradlew :app:lintDebug :app:check :app:connectedDebugAndroidTest :app:assembleRelease
```

The unit coverage report is written under `app/build/reports/jacoco/jacocoDebugUnitTestReport/`.
