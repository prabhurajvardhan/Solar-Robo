# Solar Robo Sensorless Tracker Architecture

## Product boundary

The Android phone is the tracking controller. It owns location, time, camera observations, the NOAA sun-position calculation, estimated panel angle, persistent tracker state, and local logs. The ESP32 is a motor-only slave on its own `SolarTracker` Wi-Fi access point at `192.168.4.1`; it provides no camera, encoder, electrical, or environmental sensor measurements. The Android app must function without internet after joining that local access point.

The mechanical axis is horizontal north-south. `theta` is panel tilt from flat toward EAST, with limits `-70°..+70°`. The ideal target is `atan2(cos(elevation) * sin(azimuth), sin(elevation))`, clamped to travel limits. Panel angles are estimates from dead reckoning and camera re-homing, never encoder readings.

## Runtime modules

- `:core:contracts` owns the platform-neutral `TrackerEngine`, `TrackerSnapshot`, `SkyObservation`, `EnergyEstimate`, link states, and faults.
- `:core:tracker` contains pure-Kotlin NOAA geometry, sun-blob detection, and controller logic. Android camera, storage, and network access must remain behind injected interfaces.
- `:core:camera` supplies bounded CameraX frame capture. The analyzer is idle except for one frame per control cycle and a four-frame jitter burst.
- `:core:device` owns the ESP32 Wi-Fi transport. Only this adapter performs HTTP calls; UI and tracker algorithms do not call Android Wi-Fi APIs.
- `:core:storage` and `:core:database` own local state and tracker event history.
- `:app` owns navigation, service lifecycle, dependency composition, permissions, and notification.

Feature modules must not import one another. Shared types cross module boundaries only through `:core:contracts`.

## Device protocol safety

`GET /move?dir=1|-1&ms=N` starts movement and returns immediately. Its `ran_ms` is the accepted/planned run duration, not completion evidence. The app must poll `/status`, which includes `running`, and must never blindly retry a move after timeout. `/status` is safe to retry. `/stop`, `/home`, and `/sync` finalize active motion before responding. The phone caps each move at six seconds; firmware caps at ten seconds and can clamp travel. A stale link, running motor, active fault, or unconfirmed setup blocks further movement.

## Observations and safety

Camera detection thresholds are minimum RGB channel `>=235`, at least four pixels, and at most 15% of the frame. Camera brightness, cloud fraction, sun visibility, and frame jitter are image-derived heuristics only. Jitter-based wind hold is not a physical wind measurement and cannot guarantee storm protection. When observations are missing, angle source is `DEAD_RECKONED`; pointing error is `null` if the sun is not visible. Unknown generation, battery, grid, temperature, rain, and wind values are shown as `Not measured`, never zero.

Every physical command passes through a deterministic `SafetyGate`. E-stop is single-tap and sends stop immediately when a live adapter is available; the UI must state clearly when offline and cannot deliver the command. Resume is explicit after E-stop.

The production CameraX source binds to the foreground service but installs its analyzer only for each requested sample; it clears the analyzer after one 320×240-bounded, aspect-preserving frame. Four samples are spaced by 500 ms for jitter estimation. AE lock and the camera's most negative supported exposure compensation are requested best-effort through Camera2 interop; the Track screen reports the camera's advertised support and clearly labels the request as best-effort. Valid cloudy frames are not treated as camera failure: the controller continues dead reckoning while the camera itself is available.

The E-stop latch is synchronously persisted in application preferences and is restored latched after process recreation. Tracker angle, calibration, wind hold, jam, and fault codes are persisted in encrypted DataStore-backed state. The sticky camera foreground service verifies the ESP32 status before motion, holds a partial wake lock only after successful startup, and releases it on stop or startup failure. The Settings screen reports battery-optimization exemption state and only opens Android's exemption flow after an explicit user tap; it never assumes exemption was granted.

Room `tracker_log` stores only the tracker.py-compatible scalar columns (`t`, `az`, `el`, `th_sun`, `lum`, `blue_frac`, `white_frac`, `sun_found`, `cx`, `area`, `jitter`, `est`, `tps`, `wind_hold`, `jam`, `action`), never camera frames. After 90 days, complete UTC days are aggregated into `tracker_daily_summary` before raw-row deletion. Insights displays local daily counts and camera-derived heuristics and can share the latest 2,000 detail rows as CSV.

## Legacy documentation

The previous dual-axis, BLE, sensor-array, battery/grid telemetry, and downloaded ternary-model descriptions are legacy until each corresponding implementation is removed or explicitly marked obsolete. Inspect actual Gradle/source files before assuming a documented module or API exists.
