# ASTRA UI upgrade verification

## Delivery

- **Branch:** `ui/design-upgrade`
- **Start branch:** `feature/astra-dashboard`
- **Latest commit before this report:** `2c8a12e` (`feat: deliver ASTRA UI design upgrade`)
- **Design images:** 7
- **Mapped screens:** sign-in, Home, Track, Sky, Predictions, History, and Settings/More visual language
- **New assets:** no bundled image or model assets; hero visuals are Compose Canvas drawings
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK size:** 78,061,420 bytes
- **APK SHA-256:** `db343fc7eebc7f6d01d8fab2bf510de0e142c89629c829f8ad5f7124ea7f9a99`
- **Previous APK size delta:** not measurable from the repository because the previous APK was not retained as a tracked artifact.

## Verification commands

| Command | Result | Details |
|---|---|---|
| `./gradlew assembleDebug` | **PASS** | `BUILD SUCCESSFUL` on the UI branch. |
| `./gradlew testDebugUnitTest` | **FAIL — existing failures** | Two existing `core:ai` tests fail: `OnDeviceSolarAiEngineTest.unknownQuestionsRemainBoundedAndNonActuating` and `respondsToSolarTopicsWithoutNetwork`. No tests were removed or weakened. |
| `./gradlew lintDebug` | **FAIL — existing module errors** | Lint stops in `core:camera` with 40 errors, first at `CameraXFrameSource.kt:73` for a missing opt-in annotation. No lint error was reported from the changed UI files before the build stopped. |

## Design-image status

| Design image | Mapped surface | Status | Reason |
|---|---|---|---|
| `file_000000003b848207a06c7826f48f783e.png` | `LockScreen` | **Partial** | Six-dot six-digit PIN display, custom keypad, biometric action, version proof, and Canvas hero are implemented. Real-device biometric variants were not tested. |
| `file_00000000be50820ab43c32cb6af7cf71.png` | `HomeScreen` | **Partial** | ASTRA hero, metric tiles, source tags, white/dark theme tokens, and existing live state are wired. No emulator screenshot or hardware-connected validation was available. |
| `file_000000002b3c820aaf3ab7b7defcc85d.png` | `TrackScreen` | **Partial** | Existing camera, controls, calibration, follow, manual jog, safety latch, and E-stop remain reachable and pick up the new theme/top bar. Full mockup-specific chart/hero polish is not complete. |
| `file_0000000058f0820a96d6b535542cd5fd.png` | `InsightsScreen` Sky mode | **Partial** | Sky mode, forecast cards, real forecast values, camera/model status, advisory banner, and refresh are implemented. Offline/online behavior remains in the existing ViewModel/runtime. |
| `file_000000000184820a8d8b803d190d997b.png` | `InsightsScreen` Sky/Predictions detail | **Partial** | Detail cards and forecast chart use real state and show `Not measured` when absent. Exact mockup-only weather values were not fabricated. |
| `file_00000000d5a0820aa755137e557f131a.png` | `InsightsScreen` Predictions mode | **Partial** | Model status, learning progress, forecast chart, advisory, and history remain data-bound. Ring-gauge and animated accuracy treatments are not complete. |
| `file_00000000f140820ab94b377f2f5390f0.png` | Predictions navigation state | **Partial** | Predictions can be selected within the existing Insights route; no new business route was invented. No screenshot capture was performed. |

## Feature-parity checklist

| Capability | Present after UI work | Current location / note |
|---|---|---|
| PIN sign-in, lockout and auto-lock | Yes | `feature/auth`, existing ViewModel and repository unchanged; six-digit UI retained. |
| Fingerprint/face via `BiometricPrompt` | Yes | `LockScreen`; platform chooses the enrolled biometric. Real hardware not tested. |
| Host/member roles and member join | Not found in current repository | No corresponding route or implementation was present in the inspected current branch; not invented. |
| Setup wizard steps and skips | Yes | Existing `OnboardingScreen` and route retained; shared theme applies. |
| ESP32 status and `/status`, `/move`, `/stop`, `/home`, `/sync`, `/setlimit` calls | Yes | Existing device/tracker layers retained; UI changes do not alter calls. Hardware not connected for verification. |
| Manual East/West, pulse length, Follow, Auto, Go flat, Calibrate | Yes | Existing `TrackScreen` and `TrackViewModel` retained. |
| Foreground tracking service and notification | Yes | Existing service retained. Android runtime notification behavior not tested on a device. |
| Sun position, ideal/actual tilt, pointing error, wind stow, jam fault | Yes where implemented by current state | Existing Home/Track state retained; missing readings continue to show unavailable/Not measured. |
| Sky stats, Predictions, model status, Offline/Online forecast | Yes | Redesigned `InsightsScreen` plus existing `WeatherRuntime`. |
| History charts, event data and CSV export | Yes | Redesigned `InsightsScreen`; CSV export retained. PDF export was not found in the current route/code inventory. |
| Safety, alerts, notifications, Device, Settings, About | Yes where present | Existing routes remain reachable; global STOP is in the logged-in shell. No new Device/Users screens were invented. |
| Backup/restore, delete logs, delete Example data | Not found in current repository | No corresponding current route/control was found during inventory. |
| STOP in logged-in top bar | Yes | `AstraTopBar` is supplied by the main logged-in shell. It invokes the existing `TrackViewModel.emergencyStop` after confirmation. |
| Version text on sign-in and About | Yes | `2.1.0-astra-ui` in Gradle, sign-in, Home, Settings/About. |

## Deliberately omitted or replaced

- Mockup-only electrical power, battery, wind, humidity, UV, visibility, accuracy, learning-day counts, and example forecast numbers are not fabricated. The UI renders real values or `Not measured`.
- No large hero images, GLB, LLM, or model assets were added. Canvas illustrations keep the APK asset increase at zero.
- No new Users, Device, PDF, backup, restore, or member-sharing screens were invented because they were not present in the inspected repository routes.
- Real-device biometric, ESP32 motor movement, two-phone member sharing, low-end 4 GB animation performance, dark-theme screenshot comparison, large-font 1.3x layout, and Android remove-animations behavior were not tested in this sandbox.
- Before/after emulator screenshots were not captured because no Android emulator/device session was available.

See [`docs/ui-mapping.md`](./ui-mapping.md) for the image-by-image U0 mapping and visual observations.
