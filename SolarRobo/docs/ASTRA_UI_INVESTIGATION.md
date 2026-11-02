# ASTRA UI redesign investigation

## Finding

The previous PR did not fail because the launcher or redesigned route was disconnected. It failed visually because the implementation changed the shell, palette, and a few cards but did not replace the visible screen compositions with the supplied ASTRA layouts.

## Entry point and branch checks

- `app/src/main/AndroidManifest.xml` declares `.MainActivity` as the `MAIN`/`LAUNCHER` activity.
- `MainActivity.onCreate()` calls `setContent { SolarRoboApp() }`.
- `SolarRoboApp()` is in the application module and is the actual installed-app root.
- The checked-out branch is `ui/design-upgrade`, based on `feature/astra-dashboard`, with the latest prior implementation at `2c8a12e` and report commit `c93e0ac`.
- The APK was built from this repository and the app module; there was no evidence of a wrong module or wrong launcher artifact.

## Route and composable checks

`AppNavHost` imports and renders:

- `com.solarrobo.app.presentation.HomeScreen`
- `com.solarrobo.app.presentation.TrackScreen`
- `com.solarrobo.app.presentation.InsightsScreen`
- `feature.auth.presentation.LockScreen` through `SolarRoboApp`

Therefore the redesigned `app/presentation` Home and Insights files are live route implementations. A second `feature/home/presentation/HomeScreen.kt` exists, but it is not imported by `AppNavHost`; it is a duplicate legacy feature implementation and was not the reason the new app route was hidden.

## Exact visual gap

The seven supplied references are not merely color references:

- Sign-in is a large solar-panel image/brand composition with fingerprint and face action tiles, not only a keypad on a Canvas hero.
- Home is an image-led hero with logo/header actions, current power and energy tiles, sun-position arc, Auto/Manual/Schedule segmented control, position slider, system/energy cards, and a four-item Home/Track/Sky/History navigation bar.
- Track is a camera hero with Live/STOP controls, ideal-vs-actual chart, spot offset and wind-jitter cards, mode segmented control, East/West controls, and travel gauge.
- Sky is a live/offline segmented forecast surface with cloud-cover hero, six-hour strip, chart, atmospheric pressure, wind-risk, and solar-window cards.
- Predictions is a separate visible state with model-status hero, learning progress, stow/motor cards, prediction chart, accuracy ring, outlook, insights, and a Predictions-selected bottom navigation state.

The previous branch only provided:

- A global top bar and bottom navigation.
- A Canvas panel illustration and basic metric tiles on Home.
- A text/card-based Sky/Predictions screen with a simple line chart.
- A custom sign-in keypad and Canvas illustration.
- Theme tokens and rounded card shapes.

Track, Settings, onboarding, camera, notifications, simulator, Talk, and security remained materially close to their previous layouts. The route was connected, but the visual result was not close enough to the references to satisfy the task.

## Corrective plan

1. Keep the actual launcher, ViewModels, handlers, device calls, authentication, routes, and existing auxiliary screens.
2. Rebuild the live Home, Track, Sky, Predictions, and sign-in compositions with the reference hierarchy: hero, segmented controls, data-bound cards, charts/gauges, and four-item navigation.
3. Use Canvas/Compose primitives and existing camera/state data instead of adding large assets or fabricated readings.
4. Keep `Not measured`/source tags for unavailable values.
5. Add real APK screenshot proof only if an emulator or physical device is available. If no device is available, report that limitation plainly and do not use previews as running-app proof.
