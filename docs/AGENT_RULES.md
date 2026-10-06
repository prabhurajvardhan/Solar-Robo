# Solar Robo — Agent Coding Rules & Protocol (AGENTS.md)

This protocol governs all automated AI coding models (Antigravity, Claude, Codex, Android Studio AI, etc.) working on the Solar Robo Android codebase.

---

## 1. System Constitution

```
PROJECT:
- Native Android application using Kotlin and Jetpack Compose.
- Gradle Kotlin DSL multi-module architecture (:app, :core:*, :feature:*).

ARCHITECTURE RULES:
- The :app module composes features and provides the Hilt root and Navigation host.
- Feature modules NEVER import other feature modules (Zero cross-feature imports).
- Feature modules depend ONLY on :core:contracts and relevant :core:* infrastructure interfaces.
- Platform access (ESP32 Bluetooth Low Energy, CameraX, Gemini AI, SQLite) is abstracted behind platform adapters.

SCOPE RULES:
- Implement ONLY the assigned module and specified files.
- Do NOT add unrequested "helpful" features or utilities.
- If a missing contract or private internal of another module is needed:
  STOP immediately and report the contract gap. Do NOT invent workarounds.

UI CONVENTIONS:
- Jetpack Compose with Material 3 only.
- Implement loading, error, empty, and offline states for every screen.
- Provide @Preview composables for each component using Fake/Mock data.
- NEVER invoke device SDKs, Bluetooth APIs, or AI model SDKs directly from UI composables.

SAFETY INVARIANTS:
- All physical actuation commands (RoboCommand) MUST pass through the deterministic Safety Gate.
- Large Language Models (AI) can advise or summarize, but MUST NEVER directly issue actuator commands.
- The Safety Gate enforces mechanical limits [-90° to +90°], wind-speed thresholds, and battery reserves.
```

---

## 2. Standard AI Agent Module Assignment Prompt

When delegating a module to an AI agent, use this exact prompt format:

```text
TASK: Implement ONLY :feature:<MODULE_NAME>.

READ FIRST:
1. docs/SYSTEM_ARCHITECTURE.md
2. docs/TECH_STACK.md
3. docs/MODULE_CONTRACTS.md
4. docs/modules/<MODULE_NAME>.md

DO:
- Create only the specified files in feature/<MODULE_NAME>/
- Implement only the listed functions and public contracts
- Provide a Fake<Module>Repository in the mock/ directory
- Implement Compose @Preview functions for all UI components
- Add unit tests verifying happy path, errors, and boundary states
- Verify compilation with Gradle

DO NOT:
- Redesign the global architecture or modify :core:contracts without approval
- Import any other :feature:* module
- Direct GPIO, Bluetooth, or camera hardware calls from UI
- Bypass the safety gate for actuator movement

FINAL REPORT REQUIRED:
- Files created/modified
- Functions implemented
- Inputs consumed and Outputs produced
- Test cases verified
- Scope additions: NONE
```

---

## 3. Definition of Done Checklist

Every feature module must pass this checklist before merging into the main branch:

| Check | Pass Condition |
|:------|:---------------|
| **File Conformity** | Exactly the declared files in `docs/modules/<name>.md` exist; no extraneous files created. |
| **Strict Scope** | Zero unapproved features, extra tabs, or speculative logic implemented. |
| **Boundary Isolation** | Module imports only `:core:contracts`, `:core:ui`, `:core:common`, `:core:device/ai`. |
| **Mock-First** | `Fake<Module>Repository` is implemented and can drive the UI completely without hardware. |
| **Compose Previews** | Every composable has `@Preview` functions demonstrating default, loading, and error states. |
| **StateFlow UDF** | UI observes an immutable `UiState` data class emitted by a single `ViewModel`. |
| **Deterministic Safety** | Commands respect mechanical [-90°, +90°] constraints and safety overrides. |
| **Unit Tests** | `src/test/` contains ViewModel and use case unit tests covering success, failure, and edge cases. |
| **CI Passing** | `./gradlew assembleDebug test lint` passes with 0 errors. |
