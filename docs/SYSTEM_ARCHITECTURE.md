# Solar Robo — Android System Architecture Specification

## 1. Executive Summary & Core Principle

**Solar Robo** is a native Android application engineered using **Kotlin** and **Jetpack Compose** for monitoring, controlling, and interacting with a dual-axis solar-tracking robotic apparatus equipped with ESP32 microcontrollers, CameraX vision feed, environmental sensor arrays, local/cloud AI reasoning, and deterministic safety mechanisms.

> **Core Principle:** Create the puzzle piece completely → verify it → place it in the correct position.
> 
> A module must never expand its scope because an adjacent feature looks useful. Business logic is strictly isolated; feature modules never import other feature modules.

---

## 2. High-Level Architectural Diagram

```
                     +---------------------------------------+
                     |              SOLAR ROBO               |
                     |            ANDROID CLIENT             |
                     +---------------------------------------+
                                         |
                                         v
                     +---------------------------------------+
                     |              :app MODULE              |
                     |   - SolarRoboApplication (Hilt Root)   |
                     |   - MainActivity                      |
                     |   - AppNavHost (Navigation Compose)   |
                     |   - Top-level composition & DI graph  |
                     +---------------------------------------+
                                    /         \
                 +-----------------+           +-----------------+
                 |                                               |
                 v                                               v
   +----------------------------+                 +----------------------------+
   |      FEATURE MODULES       |                 |        CORE MODULES        |
   |  (:feature:<name>)         |                 |  (:core:<name>)            |
   |----------------------------|                 |----------------------------|
   | - :feature:onboarding      |                 | - :core:contracts          |
   | - :feature:home            |                 | - :core:model              |
   | - :feature:energy          |                 | - :core:ui (Design System) |
   | - :feature:control         |                 | - :core:common             |
   | - :feature:talk            |====== uses =====> - :core:device (RoboDevice) |
   | - :feature:safety          |                 | - :core:ai (AiEngine)      |
   | - :feature:camera          |                 | - :core:database (Room)    |
   | - :feature:environment     |                 | - :core:storage (DataStore)|
   | - :feature:activity        |                 | - :core:network (Retrofit) |
   | - :feature:health          |                 | - :core:camera (CameraX)   |
   | - :feature:analytics       |                 | - :core:simulator          |
   | - :feature:notifications   |                 +----------------------------+
   | - :feature:settings        |                                |
   | - :feature:simulator       |                                |
   +----------------------------+                                v
                                                  +----------------------------+
                                                  |     PLATFORM ADAPTERS      |
                                                  |----------------------------|
                                                  | - ESP32 BLE / Wi-Fi Socket |
                                                  | - CameraX Native Preview   |
                                                  | - Gemini / Local AI Engine |
                                                  | - Room DB / SharedPreferences|
                                                  +----------------------------+
```

---

## 3. Architecture Rules & Invariants

1. **Native Android Standard:** Exclusively built on modern Android conventions: Kotlin 1.9+, Jetpack Compose, Material Design 3, Coroutines, StateFlow, and Jetpack Navigation.
2. **Feature Isolation (Zero Cross-Feature Imports):**
   - A feature module (`:feature:A`) must NEVER import another feature module (`:feature:B`).
   - `feature:energy` CANNOT import `feature:home`.
   - `feature:talk` CANNOT import `feature:control`.
3. **Canonical Contracts as Single Source of Truth:**
   - All shared models, events, commands, and interfaces reside strictly in `:core:contracts` and `:core:model`.
   - If feature A produces an event and feature B consumes it, communication occurs via an event boundary or shared repository contract in `:core:contracts`.
4. **Mock-First & Hardware Decoupling:**
   - Every feature must include a mock implementation (`Fake<Module>Repository`) capable of rendering all screens, states, and preview modes without requiring physical ESP32 hardware, camera access, network, or cloud AI.
5. **Deterministic Safety Gate:**
   - AI language models may suggest or explain actions, but **NEVER directly issue physical actuation commands**.
   - Every movement command (`MOVE_TO_ANGLE`, `STOP_MOTION`, `SAFE_POSITION`) MUST pass through the deterministic `SafetyPolicy` gate before reaching the hardware device adapter.
6. **Clean Architecture & Unidirectional Data Flow (UDF):**
   - **UI Layer:** Compose Screen → Stateless Components + Composable Previews (`@Preview`). Observes `StateFlow<UiState>`.
   - **Presentation Layer:** `ViewModel` with Hilt DI (`@HiltViewModel`), exposing immutable `StateFlow<UiState>` and accepting user intents.
   - **Domain Layer:** Pure Kotlin interfaces, use cases, and business logic.
   - **Data Layer:** Repository implementations mediating between local database, DataStore, and platform adapters.
7. **Stop Condition for AI Coding Agents:**
   - If an agent discovers that a capability or private internal of another module is needed, it must NOT write an ad-hoc workaround or import the module. It must report a contract gap and stop.

---

## 4. End-to-End Runtime Execution Path

```
 [ESP32 / BLE / Camera / AI Engine]
                 │
                 ▼
     Platform Adapters (BLE / CameraX / Retrofit)
                 │
                 ▼
       Canonical Contracts (:core:contracts)
                 │
                 ▼
      Repository & Kotlin Flows (:feature:*:data)
                 │
                 ▼
      ViewModel & UiState (:feature:*:presentation)
                 │
                 ▼
     Jetpack Compose UI Screen (:feature:*:presentation)
                 │
                 ▼ (User Interaction, e.g., Angle Slider)
      Command Request
                 │
                 ▼
   +-------------------------------------------------+
   |              DETERMINISTIC SAFETY GATE          |
   | - Evaluate Wind Speed & Rain Sensors            |
   | - Check Hardware Faults & Battery Reserves      |
   | - Enforce Mechanical Boundaries [-90° to +90°]  |
   +-------------------------------------------------+
                 │
       [ALLOW / BLOCK / MODIFY]
                 │
                 ▼
      DeviceAdapter / RoboDevice
                 │
                 ▼
   [ESP32 Microcontroller Actuation]
```

---

## 5. Build Order & Milestones

| Stage | Milestone | Primary Modules | Exit Condition |
|:-----:|:----------|:----------------|:---------------|
| **1** | Android Project & Foundation | `:app`, `:core:contracts`, `:core:ui`, `:core:common` | App builds cleanly, empty navigation shell runs on emulator. |
| **2** | Core Contracts | `:core:contracts`, `:core:model` | Canonical types, sealed interfaces, and commands compile. |
| **3** | App Shell & Design System | `:app:navigation`, `:core:ui` | Navigation bar, theme, and scaffold switch between destination routes. |
| **4** | Simulator & Mocks | `:core:simulator`, `:feature:simulator` | Fake Robo world generates deterministic telemetry, faults, and ticks. |
| **5** | Device Onboarding | `:feature:onboarding` | BLE/Wi-Fi scan, connection verification, and device pairing flow work. |
| **6** | Home & Energy | `:feature:home`, `:feature:energy` | Dashboard displays live generation, battery stats, and energy flow chart. |
| **7** | Control & Safety Gate | `:feature:control`, `:feature:safety` | Manual angle sliders, safe stow position, and E-Stop pass safety checks. |
| **8** | Robo Talk (AI Assistant) | `:feature:talk`, `:core:ai` | Natural language chat with AiEngine and bounded context; no direct actuation. |
| **9** | Camera & Environment | `:feature:camera`, `:feature:environment` | CameraX video preview and weather sensor readings render without hardware. |
| **10**| Activity, Health & Alerts | `:feature:activity`, `:feature:health`, `:feature:notifications` | Event timeline, diagnostics checklist, and deduplicated notification inbox. |
| **11**| Analytics & Settings | `:feature:analytics`, `:feature:settings` | Energy generation charts, efficiency metrics, and user preferences persist. |
| **12**| Production Adapters | `:core:device`, `:core:camera`, `:core:network` | Swap mock adapters for real ESP32 BLE/Socket and CameraX hardware. |
| **13**| Hardware Validation | Physical Rig & Hardware Integration | End-to-end testing with solar tracker hardware and emergency stops. |
