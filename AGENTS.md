# Repository Guidelines

## Project Structure & Module Organization
This repository is currently a planning workspace for `ScheduleJS`, not a scaffolded Android app yet. The main documents are:

- `README.md`: project summary and intended stack
- `BLUEPRINT.md`: architecture, screens, data model, and roadmap
- `schedule.md`: day-template scheduling rules
- `weekly workout plan.md`: workout rotation content

When implementation begins, keep Android source under `app/src/main/`, tests under `app/src/test/` and `app/src/androidTest/`, and static assets in `app/src/main/res/`.

## Build, Test, and Development Commands
There is no runnable build yet. After Android project initialization, prefer standard Gradle commands from the repo root:

- `./gradlew assembleDebug`: build the debug app
- `./gradlew test`: run JVM unit tests
- `./gradlew connectedAndroidTest`: run device or emulator instrumentation tests
- `./gradlew lint`: run Android lint checks

Do not document ad hoc local commands in commits; keep repeatable workflows in Gradle tasks.

## Coding Style & Naming Conventions
Use Kotlin with Jetpack Compose, MVVM, Room, and AndroidX conventions described in `README.md` and `BLUEPRINT.md`. Use 4-space indentation, one top-level class or file concern per file, and clear names such as `DashboardScreen`, `ScheduleViewModel`, and `TaskEntity`.

Naming guidance:

- Compose UI: `PascalCase` screen/component names
- Kotlin functions/variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Markdown planning docs: short lowercase filenames with spaces only when already established

If formatting is added later, standardize on Gradle-integrated linting before expanding the codebase.

## Testing Guidelines
No test suite exists yet. When code is added, place fast unit tests in `app/src/test/` and UI/database/integration coverage in `app/src/androidTest/`. Name test files after the target, for example `ScheduleRepositoryTest.kt`, and write test names that describe behavior.

Focus first on schedule resolution, alarm timing, and Room mappings.

## Commit & Pull Request Guidelines
This repository has no commit history yet, so adopt a simple imperative style now: `Add dashboard data model`, `Scaffold Room entities`. Keep commits scoped to one change.

Pull requests should include:

- a short summary of intent
- linked issue or task, if available
- screenshots for Compose UI work
- notes on testing performed or gaps

## Configuration & Security
Do not commit `local.properties`, signing keys, or keystore files. The existing `.gitignore` already excludes Android build outputs, IDE metadata, logs, and signing material; keep it aligned with future Android Studio setup.
