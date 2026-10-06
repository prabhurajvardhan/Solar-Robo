# Solar Robo — Native Android Application

Welcome to the **Solar Robo** native Android codebase and architecture specification.

Solar Robo is a dual-axis solar tracking robotic system controlled by a clean, reactive Android application written exclusively in **Kotlin** and **Jetpack Compose**.

---

## 📚 Complete Architectural Documentation

Detailed architectural and engineering documentation is maintained in the [`docs/`](./docs) directory:

1. **[System Architecture (`docs/SYSTEM_ARCHITECTURE.md`)](./docs/SYSTEM_ARCHITECTURE.md)**  
   System flow, 3-tier clean architecture, module boundaries, deterministic safety gate, and build milestones.
2. **[Technology Stack (`docs/TECH_STACK.md`)](./docs/TECH_STACK.md)**  
   Locked Android technology stack: Kotlin 1.9+, Jetpack Compose, Hilt, Coroutines & Flow, Navigation Compose, Room, DataStore, CameraX, and OkHttp.
3. **[Canonical Cross-Module Contracts (`docs/MODULE_CONTRACTS.md`)](./docs/MODULE_CONTRACTS.md)**  
   Exhaustive Kotlin data models, sealed hierarchies, and interfaces (`RoboSnapshot`, `EnergySnapshot`, `RoboCommand`, `SafetyEvent`, `RoboDevice`, `AiEngine`).
4. **[Agent Coding Protocol (`docs/AGENT_RULES.md`)](./docs/AGENT_RULES.md) & [AGENTS.md](./AGENTS.md)**  
   Strict instruction sets for autonomous AI coding agents (Antigravity, Claude, Codex, Android Studio AI).
5. **Detailed Module Specifications with Complete Skeleton Code (`docs/modules/`)**:
   - [`onboarding.md`](./docs/modules/onboarding.md) — Bluetooth LE discovery and provisioning.
   - [`home.md`](./docs/modules/home.md) — Command center dashboard and first-glance cards.
   - [`energy.md`](./docs/modules/energy.md) — Real-time power flow and historical energy yield.
   - [`control.md`](./docs/modules/control.md) — Manual/auto angle sliders and safe stow controls.
   - [`talk.md`](./docs/modules/talk.md) — Conversational AI assistant with bounded context.
   - [`safety.md`](./docs/modules/safety.md) — Deterministic safety gate, wind stow, and E-Stop button.
   - [`camera.md`](./docs/modules/camera.md) — CameraX live optical stream and still snapshots.
   - [`environment.md`](./docs/modules/environment.md) — Irradiance lux, temperature, wind, and rain indicators.
   - [`activity.md`](./docs/modules/activity.md) — Chronological robotic event memory and logs.
   - [`health.md`](./docs/modules/health.md) — Subsystem diagnostic checklists (motors, sensors, battery).
   - [`analytics.md`](./docs/modules/analytics.md) — Historical efficiency metrics and comparison scores.
   - [`notifications.md`](./docs/modules/notifications.md) — Deduplicated alert inbox and unread badges.
   - [`settings.md`](./docs/modules/settings.md) — DataStore user preferences and display toggles.
   - [`simulator.md`](./docs/modules/simulator.md) — Deterministic hardware simulator with 7 scenario presets.

---

## 🛠️ Repository Structure

```text
SolarRobo/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/solarrobo/app/
│       ├── SolarRoboApplication.kt
│       ├── MainActivity.kt
│       ├── App.kt
│       └── navigation/AppNavHost.kt
├── core/
│   ├── contracts/        # Cross-module shared contracts
│   ├── model/            # Core business models
│   ├── ui/               # Reusable Compose design system
│   ├── common/           # Coroutine dispatchers & EventBus
│   ├── device/           # RoboDevice abstraction & ESP32 BLE adapter
│   ├── ai/               # AiEngine model abstraction
│   ├── camera/           # CameraX manager
│   ├── database/         # Room DB & DAOs
│   ├── storage/          # DataStore preferences
│   ├── network/          # Retrofit & OkHttp
│   └── simulator/        # In-memory hardware simulation engine
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── energy/
│   ├── control/
│   ├── talk/
│   ├── safety/
│   ├── camera/
│   ├── environment/
│   ├── activity/
│   ├── health/
│   ├── analytics/
│   ├── notifications/
│   ├── settings/
│   └── simulator/
└── docs/
```

---

## 🚀 Rapid Development & Preview Loop

1. **Clone once into local environment:** Open this folder directly in Android Studio.
2. **Sub-second UI previews:** Every feature composable has `@Preview` functions backed by `Fake<Module>Repository`.
3. **No hardware needed:** The Developer Simulator (`:feature:simulator`) models all tracker movements, sunlight angles, rain, wind gusts, and battery states.
4. **Safety Invariant:** Large Language Models (AI) NEVER directly issue physical commands. All commands pass through the deterministic `SafetyPolicy` gate.
