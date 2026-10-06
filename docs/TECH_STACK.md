# Solar Robo — Locked Android Technology Stack

This document specifies the frozen technology stack, libraries, versions, and architectural roles for the Solar Robo native Android project. No additional third-party dependencies may be introduced without formal specification amendment.

---

## 1. Technology Matrix

| Area | Technology / Framework | Role & Architectural Scope |
|:-----|:-----------------------|:---------------------------|
| **Target Platform** | Android (API 26 Oreo to API 35+) | Native mobile operating system target. |
| **Primary Language** | Kotlin 1.9+ | Application source code, coroutines, and type-safe DSLs. |
| **UI Framework** | Jetpack Compose (BOM 2024.04+) | Declarative UI, Material Design 3, `@Preview` composables, Live Edit. |
| **Architecture** | Clean Architecture + MVVM / UDF | Layered separation (Domain, Data, Presentation), unidirectional StateFlow. |
| **Dependency Injection** | Hilt (Dagger) 2.50+ | Compile-time dependency injection across `:app`, `:core`, and `:feature`. |
| **Async & Concurrency** | Kotlin Coroutines + Flow / StateFlow | Reactive streams, cancellation, thread management, structured concurrency. |
| **Navigation** | Navigation Compose | Type-safe declarative routing, deep links, animated screen transitions. |
| **Persistence (Relational)** | Room Database 2.6+ | Local SQLite database for historical events, telemetry history, notifications. |
| **Persistence (Key-Value)** | Jetpack DataStore (Preferences) | Typed asynchronous key-value storage for settings and device pairing states. |
| **Networking & HTTP** | OkHttp 4.12+ & Retrofit 2.9+ | REST API client and WebSocket connections for ESP32 and remote telemetry. |
| **Device Hardware I/O** | Android Bluetooth Low Energy (BLE) | Peripheral scanning, GATT connection, characteristic read/write/notify. |
| **Camera & Computer Vision** | Android CameraX 1.3+ | Hardware-accelerated camera preview, image capture, and image analysis pipeline. |
| **Background Scheduling** | Android Jetpack WorkManager | Periodic background synchronization, offline data upload, health checks. |
| **AI Inference Abstraction** | Custom `AiEngine` Interface | Model-agnostic abstraction wrapping Gemini Flash / Local On-Device LLM. |
| **Unit & Integration Testing** | JUnit 4/5, MockK, Turbine, AndroidX Test | Domain/data unit tests, Flow testing, and coroutine test dispatchers. |
| **UI Testing** | Compose UI Test (`createComposeRule`) | Component-level and screen-level assertions and interaction verification. |
| **Build Tooling** | Gradle Kotlin DSL (`build.gradle.kts`) | Multi-module build system with version catalog (`libs.versions.toml`). |
| **Version Control** | Git & GitHub | Distributed version control and pull request reviews. |
| **Continuous Integration** | GitHub Actions (`android-ci.yml`) | Automated build verification, lint check, unit tests, and architecture checks. |
| **Rapid Preview Loop** | Compose `@Preview` & Live Edit | Sub-second visual iteration directly within Android Studio. |
| **Agentic Tooling** | Claude, Codex, Android Studio AI | Automated module implementation adhering strictly to `AGENTS.md`. |

---

## 2. Architectural Guidelines & Conventions

### 2.1 Presentation Layer (Jetpack Compose)
- Screens are constructed using standard Material 3 components (`Scaffold`, `TopAppBar`, `Card`, `Button`, `Slider`, `Text`).
- No clickable `Box` or `Column` divs without accessibility semantics; use standard interactive composables or `clickable(role = Role.Button)`.
- Every screen must declare an immutable UI state class (e.g. `HomeUiState`):
  ```kotlin
  data class HomeUiState(
      val isLoading: Boolean = false,
      val robo: RoboSnapshot? = null,
      val energy: EnergySnapshot? = null,
      val alerts: List<SafetyEvent> = emptyList(),
      val errorMessage: String? = null
  )
  ```
- Every composable file must contain at least one `@Preview` composable utilizing mock or fake state.

### 2.2 Domain Layer
- Domain layers reside in `feature/<name>/domain/` and depend only on `:core:contracts` and Kotlin standard libraries.
- Business rules are encapsulated in single-purpose use case classes with `operator fun invoke(...)`.

### 2.3 Data Layer
- Repositories implement domain interfaces and inject platform adapters (`RoboDevice`, `AiEngine`, `RoomDao`, `DataStore`).
- Every feature module provides a `Fake<Module>Repository` inside `feature/<name>/mock/` for immediate previews and unit tests.

### 2.4 Hilt Dependency Injection Scope
- `@Singleton`: Core platform adapters (`RoboDevice`, `AiEngine`, `AppDatabase`).
- `@ViewModelScoped`: Repository bindings and use cases consumed by specific ViewModels.
