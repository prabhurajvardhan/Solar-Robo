# AGENTS.md — Exact Rules for Coding Models

## PROJECT
- Native Android application.
- Language: Kotlin 1.9+.
- UI Framework: Jetpack Compose with Material Design 3.
- Build System: Gradle Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts`).

## ARCHITECTURE
- The `:app` module composes features and provides the Hilt root and Navigation host.
- Feature modules NEVER import other feature modules (Zero cross-feature imports).
- Feature modules depend on canonical contracts in `:core:contracts`.
- Device, AI, database, and camera access is strictly behind platform adapters in `:core:*`.

## SCOPE
- Implement ONLY the assigned task and files.
- Do not add useful-looking extras or side features.
- If blocked or if private internals of another module are required: STOP and report it.

## UI CONVENTIONS
- Compose only; add `@Preview` to all components.
- Add loading, error, offline, and empty states.
- No hardware, Bluetooth, or AI model SDK calls in the UI layer.

## DEVICE / AI / SAFETY
- Use `RoboDevice` and `RoboCommand`.
- Use `AiEngine` abstraction.
- AI output CANNOT directly actuate physical hardware.
- All physical commands must pass through the deterministic `SafetyGate`.

## VERIFICATION
- Run unit tests and Gradle build.
- Check feature dependency boundaries (no forbidden imports).
- Verify required files exist.
- Report: Files created, Functions implemented, Inputs consumed, Outputs produced, Tests passed.

## STOP CONDITION
If another module's private internals or uncontracted models are required, STOP and report the contract mismatch.
