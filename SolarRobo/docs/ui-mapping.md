# ASTRA UI design mapping

**Source branch:** `feature/astra-dashboard`  
**Design archive:** `Astra-UI.zip`  
**Mockups found:** 7 PNG images, each approximately 862–863 × 1,822–1,825 px.  
**Mapping status:** U0 inventory; no product UI code changed yet.

## Design images mapped to the current app

| Design image | App screen / route / composable | Existing features on the screen | Visual elements to add or restyle | Design elements with no real data in the current app |
|---|---|---|---|---|
| `file_000000003b848207a06c7826f48f783e.png` | Sign-in / locked session: `SolarRoboApp` → `LockScreen` / `LockScreenContent`; first run remains `CreateProfileScreen` | Local PIN verification, lockout messaging, biometric prompt, viewer mode, version proof text | Full-screen ASTRA hero, six-dot PIN display, custom numeric keypad, fingerprint/face entry buttons, soft white/green surfaces | Fingerprint and face are not independently distinguishable by the app; both must remain the platform `BiometricPrompt` path. Hero image/branding is decorative only. |
| `file_00000000be50820ab43c32cb6af7cf71.png` | Home: `Screen.Home` (`today`) → `HomeScreen` | ESP32 snapshot, tracker state, sun position, safety state, history summary, local camera/weather advisory, Talk and simulator entry points | Hero panel treatment, status chip, metric cards, sun-position card, panel-position control, system/energy cards, new bottom navigation styling | Current power, kWh energy, motor/sensor health, schedule, and exact system-status bullets are not all available as measured values; show existing values or `Not measured`, never mock them. |
| `file_000000002b3c820aaf3ab7b7defcc85d.png` | Track: `Screen.Control` (`track`) → `TrackScreen` | Camera preview/light-follow, auto/follow/manual controls, east/west jog, calibration, go-home, emergency stop, safety latch, travel/safety state | Camera hero with overlay labels, ideal-vs-actual chart card, spot offset/wind jitter tiles, segmented mode control, travel meter, persistent red STOP affordance | FPS, camera-derived sun azimuth/elevation/brightness, exact chart history, and wind speed are only shown if available from current state; otherwise use `Not measured`. |
| `file_0000000058f0820a96d6b535542cd5fd.png` | Sky live state: current weather/camera portion of `Screen.Analytics` / `InsightsScreen` plus existing `WeatherViewModel` state | Offline/online forecast switch, camera classifier, forecast cache, local sky observations, pressure/optical-flow advisory where available | Sky hero, live/offline segmented switch, cloud-cover metric row, next-hours strip, forecast chart, pressure/wind cards, risk banner | Temperature, humidity, wind speed/direction, UV, visibility and weather icons are not measured offline; show `Not measured` or only render online-tagged values when Online mode supplied them. |
| `file_000000000184820a8d8b803d190d997b.png` | Sky/predictions detail state: existing `Screen.Analytics` route; likely a visual mode within `InsightsScreen` rather than a new route | Local sky observations, forecast state, prediction/advisory text, history export | Large Sky heading, condition hero, cloud-cover prediction chart with confidence band, pressure trend and wind-risk cards, current conditions container | Example values such as 28°C, 62% humidity, 10 km visibility and UV 6 must not be copied unless real online data exists; otherwise label `Not measured`. |
| `file_00000000d5a0820aa755137e557f131a.png` | Predictions view: existing forecast/prediction data in `WeatherViewModel` and `InsightsScreen`; keep current route graph | Forecast cache, camera classification, model status, local advisory and history | Model-status hero, progress/ring treatment, stow/motor cards, cloud prediction chart, accuracy card only when computed, hourly outlook, insights list | “Day 9/30”, “81% last 7 days”, motor-normal prediction and example hourly percentages cannot be invented; render baseline/status values only when the existing state computes them. |
| `file_00000000f140820ab94b377f2f5390f0.png` | Predictions alternate navigation state: same existing analytics/prediction surface, not a new business route | Same prediction/model/history data as above | Predictions-selected bottom navigation, model status card, prediction accuracy ring, outlook and insights cards | Same as the previous prediction mockup; accuracy and learning-day counts are absent unless computed by the current app. |

## Current screens/features without a matching design image

The current code contains these surfaces without a dedicated mockup. They will keep their routes, handlers, permissions, and behavior and will be restyled with the same white/dark-green tokens and shared components rather than removed or hidden:

| Existing surface | Route / entry | Restyling plan |
|---|---|---|
| First-run setup wizard | `Screen.Onboarding` / `OnboardingScreen` | Use the same rounded cards, typography, spacing, progress treatment, and top-bar language; preserve all setup steps and skips. |
| Security settings | `Screen.Security` / `SecuritySettingsScreen` | Restyle as a settings/detail page with the same cards, green controls, source/status labels, and destructive-action treatment. |
| Camera screen | `Screen.Camera` / `CameraScreen` | Use the Track visual language for preview and status; preserve camera behavior. |
| Notifications | `Screen.Notifications` / `NotificationsScreen` | Use alert cards, severity colors, and the shared top bar; preserve notification state. |
| Developer simulator | `Screen.Simulator` / `SimulatorScreen` | Keep visibly marked as simulated/developer data; reuse metric cards and controls. |
| Talk | `Screen.Talk` / `TalkScreen` | Use the same ASTRA shell and card language; preserve the existing conversation flow. |
| Existing settings | `Screen.Settings` / `SettingsScreen` | Restyle settings sections and About while retaining all current controls and security navigation. |
| Existing insights/history | `Screen.Analytics` / `InsightsScreen` | Use the Sky/Predictions visual language and keep CSV export and local-history behavior. |

## Current feature-parity checklist captured from code

- Local profile creation, salted PIN hashing, lockout, biometric prompt, viewer mode and auto-lock.
- Onboarding/setup flow and navigation to Home.
- ESP32/device adapter and safety-gated commands, including stop, movement, home, calibration-related controls and foreground tracking service.
- Home dashboard, tracker state, camera/weather runtime, offline/online forecast cache and local history.
- Track camera/light-follow preview, Auto/Follow/Manual controls, jog controls, calibration, go-home, emergency stop, fault reset and safety latch.
- Sky/weather advisory and prediction state, model status, forecast refresh/cache and camera-derived observations.
- Insights/history display and CSV share/export behavior.
- Settings, security, location/travel-limit controls, notification/voice settings, reset/defaults and About text.
- Camera, Notifications, Simulator and Talk routes.

The design archive does **not** provide dedicated images for member sharing/users, device management, PDF export, backup/restore, or the existing auxiliary routes. These must remain reachable and will receive consistent styling only; no new data or screens will be invented from the mockups.

## Visual observations for U1

The images consistently use a near-white background, dark green primary controls, pale mint cards, large black headings, rounded 20–28 px containers, soft/no heavy shadows, and bottom navigation with a green selected pill. The exact visual treatment will be represented as theme tokens and shared Compose components. Decorative solar-panel/cloud imagery will be kept under the asset budget and will not replace measured/calculated data.
