# Weather intelligence core (advisory only)

`core:weather` is an isolated, additive module. It does not modify or replace camera acquisition, tracking, ESP32 control, `TrackerEngine`, or `SafetyGate`. The app is not wired to this module; callers may consume recommendations without any automatic behavior.

## Layers and APIs

- `CameraNowcast` / `ImageTrendNowcast`: accepts time-stamped `CameraObservation` descriptors and derives cloud-cover trend and centroid pixel motion over a short window. Results are explicitly `isImageEstimate`; pixel displacement is not wind speed, cloud height, precipitation, or a calibrated forecast. It requires at least two compatible samples and rejects nonpositive/overlong time intervals.
- `CameraInference`: a future inference seam only. It does not imply that a CNN/model exists or is trained. The current tracker image heuristics remain unchanged.
- `LocalClimatology` / `ProvisionalClimatology`: configurable baseline defaults to 50% cloud estimate and unknown wind baseline. `ClimateProfile` captures provider/name, reference, retrieval date, geographic coverage, site, and provisional state. The default explicitly records no verified source and no coverage.
- `WeatherLearning` / `VerifiedOutcomeLearning`: only `VerifiedOutcome` instances increment the count; each requires nonblank verifier provenance. It remains insufficient until `minimumVerifiedOutcomes` (default 100) is reached. Existing tracker logs are not ingested or treated as labels.
- `ForecastProvider` and `OpenMeteoTransport`: free, no-key Open-Meteo hourly cloud cover, precipitation and wind speed (m/s), cached behind `ForecastCache`. `ForecastSnapshot` carries retrieval/fetched, issued, and expiry timestamps plus `FRESH`, `STALE`, `OFFLINE`, or `UNAVAILABLE` status. Open-Meteo's hourly forecast response does not expose the model issue time, so this adapter explicitly uses retrieval time as a proxy for `issuedAt`; consumers must not interpret it as provider issue metadata. `CachedForecastProvider` catches network/transport errors and returns marked cached data or `UNAVAILABLE`, never allowing weather network failure to block tracking. `MemoryForecastCache` is a starter implementation; production persistence can implement `ForecastCache` independently.
- `WeatherRiskCombiner` / `ConservativeRiskCombiner`: yields `WeatherRecommendation` only. Even a high advisory has `actionable=false`; the module has no motor/command/SafetyGate reference and cannot actuate. Any future protective stow must be separately requested through the existing `TrackerEngine` path, whose physical operations remain subject to its established safety authorization and `SafetyGate` interlocks. A recommendation is not approval to stow.

## Use

Add `implementation(project(":core:weather"))` only to a caller that intentionally adopts the module. No application DI or UI wiring is performed by this change. Create a `CachedForecastProvider(OpenMeteoTransport(), cache)` and handle `UNAVAILABLE`/`OFFLINE` as advisory states; never await network before a tracker control cycle.

Run tests with `./gradlew :core:weather:testDebugUnitTest` (or `./gradlew :core:weather:test` depending on installed Android Gradle Plugin task configuration). The tests use deterministic fakes and do not require network access.
