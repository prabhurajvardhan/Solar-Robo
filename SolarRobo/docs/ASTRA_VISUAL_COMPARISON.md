# ASTRA visual comparison

## Important proof limitation

The seven reference images are included in [`docs/ui-proof/reference`](ui-proof/reference). This sandbox has no `adb`, emulator, or physical Android device, and the previous APK was not retained as a screenshot source. Therefore the requested previous-app and redesigned-running-app screenshots cannot be honestly produced here. The files under [`docs/ui-proof/previous-app`](ui-proof/previous-app) and [`docs/ui-proof/redesigned-app`](ui-proof/redesigned-app) state this limitation explicitly. No preview, hand-composed image, or mockup copy is presented as running-app proof.

## Per-design status

| Reference | Real route | Corrective implementation | Running screenshot | Remaining difference / limitation |
|---|---|---|---|---|
| [Sign-in](ui-proof/reference/file_000000003b848207a06c7826f48f783e.png) | `SolarRoboApp` → `LockScreen` | Reference-led solar hero, six-dot PIN keypad, biometric action, version proof | Not captured | Biometric variants and visual match require a real device. |
| [Home](ui-proof/reference/file_00000000be50820ab43c32cb6af7cf71.png) | `Screen.Home` → `app/presentation/HomeScreen` | Hero crop, Clean Energy/Smarter Motion hierarchy, metric tiles, sun-position card, segmented actions, position slider, source tags | Not captured | ESP32-connected values cannot be exercised in this sandbox. |
| [Track](ui-proof/reference/file_000000002b3c820aaf3ab7b7defcc85d.png) | `Screen.Control` → `TrackScreen` | Camera hero, Live/Offline status, ideal-vs-actual chart card, metric tiles, existing controls retained | Not captured | Camera preview, tracking service, STOP behavior, and chart rendering require runtime verification. |
| [Sky live](ui-proof/reference/file_0000000058f0820a96d6b535542cd5fd.png) | `Screen.Analytics` → `InsightsScreen` Sky mode | Forecast cards/chart, real weather state, advisory banner, refresh, source labels | Not captured | Exact online/offline visual states require runtime network/cache conditions. |
| [Sky detail](ui-proof/reference/file_000000000184820a8d8b803d190d997b.png) | `Screen.Analytics` → Sky/Predictions surface | Existing state-backed forecast/prediction cards; unavailable readings remain unavailable | Not captured | Pressure, humidity, UV and other mockup-only values are not fabricated. |
| [Predictions](ui-proof/reference/file_00000000d5a0820aa755137e557f131a.png) | `Screen.Analytics` → Predictions mode | Model status, learning progress, forecast chart, advisory and history remain bound to state | Not captured | Accuracy ring and exact mockup-only learning values are not claimed as complete. |
| [Predictions navigation](ui-proof/reference/file_00000000f140820ab94b377f2f5390f0.png) | Existing analytics route with ASTRA shell navigation | Four-item Home/Track/Sky/History navigation; Predictions remains an in-screen state | Not captured | No new business route was invented; runtime navigation needs device validation. |

## Asset and build notes

The corrective pass adds five compressed WebP hero crops totaling approximately 148 KB, well below the 5 MB asset limit. The images are visual anchors only; live readings and actions continue to come from existing ViewModels and handlers. The release remains subject to the repository's existing `core:ai` unit-test failures and `core:camera` lint failures documented in the verification report.
