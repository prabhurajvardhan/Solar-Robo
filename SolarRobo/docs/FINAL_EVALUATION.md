# Solar Robo — Final Evaluation Report

**Evaluation date:** 25 October 2026  
**Android package:** `com.solarrobo.app`, version 1.0, minimum API 30, target API 35  
**Model:** one MobileNetV3Small transfer-learning run, exported to TensorFlow Lite  
**Dataset:** WEBCAM v1.0

## CNN held-out evaluation

The model was evaluated on **2,280 held-out images** grouped into 390 capture-source/day groups, with train, validation, and test groups kept disjoint. Test accuracy was **82.5%** and macro-F1 was **70.0%**. These metrics describe image-label classification on the held-out dataset; they do not establish live-site cloud or storm detection and are not a tracker safety metric.

Per-class held-out results were: clear — 98.6% precision, 91.7% recall, 95.0% F1; fair-weather cumulus — 74.8% precision, 88.0% recall, 80.9% F1; overcast/stratus — 89.7% precision, 74.8% recall, 81.6% F1; towering cumulus — 47.0% precision, 51.8% recall, 49.3% F1; cumulonimbus/storm-like — 27.3% precision, 5.6% recall, 9.3% F1; fog/obscured sky — 96.2% precision, 95.2% recall, 95.7% F1; and rain/precipitation — 70.6% precision, 88.0% recall, 78.3% F1.

The storm-like class is not dependable as a detector: it missed most held-out storm-like examples, with only 5.6% recall. In-app class and confidence are advisory-only and must not be treated as storm verification, a weather-sensor replacement, or a safety clearance. No weather output actuates a motor. The selected classification cutoff is **0.30**, chosen on validation data under the recorded policy of targeting at least 80% selective accuracy and at least 20% coverage. At that cutoff the validation split had 99.4% coverage and 86.5% selective accuracy. Softmax scores are not calibrated storm probabilities.

## Dataset and training

The single training run used **11,306** WEBCAM images across seven classes: 7,882 training images in 1,363 groups, 1,144 validation images in 195 groups, and 2,280 test images in 390 groups. Capture filenames were used to group camera/source tokens with capture dates. Altocumulus and cirrus source folders were excluded because they had no dedicated target class; they were not relabeled as another class.

MobileNetV3Small was initialized from ImageNet weights and fine-tuned once, with the final 35 non-BatchNorm layers trainable. Training used 160×160 input, Adam at 0.0002, batch size 24, class weighting, horizontal-flip and contrast augmentation, and early stopping on validation loss. The best checkpoint was restored after **3 epochs** (8 maximum; patience 2). The TFLite export is **1,117,432 bytes (about 1.07 MiB)** and uses TensorFlow Lite post-training optimization. Sandbox CPU inference latency was 1.44 ms median and 1.65 ms p95 after warm-up; this is not an Android-phone benchmark.

Dataset archive SHA-256: `44aadeb0b8b68cdbb8a222f561d69e398a03e96b0e37465559a98ab3eccd3201`. The dataset source describes public availability for research, but no dataset-specific license was present in the inspected source. The training record therefore restricts the derived model to non-commercial evaluation pending license confirmation. [WEBCAM dataset release](https://github.com/MarcusCoteFIT/webcam-ground-based-cloud-image-dataset/releases/tag/v1.0)

## Application behavior and architecture

The existing app was upgraded rather than replaced. Its Today, Track, Insights, and Settings screens use Compose and Material 3. Tracking logic runs in Kotlin; the controller computes NOAA sun position and single-axis geometry, and performs camera-referenced position correction plus dead reckoning. Sun-visible motor moves are checked against subsequent camera displacement for jam detection. Jitter-burst centroids are averaged, and high-jitter observations do not rehome the panel while wind-hold stow is active. Both the original Python simulation and the newly supplied `sim_test-1.py` passed all eight checks against the supplied Python tracker, including clear-day tracking, 90-minute cloud dead reckoning, wind stow/release, jam flagging, and travel-limit clamping.

The foreground service hosts the control loop and demand-driven CameraX frames. The ESP32 adapter uses Android Wi-Fi network selection and socket-based HTTP transport. Its SSID, password, host, routes, and JSON status fields match the newly supplied `.ino`. The adapter now waits for a partially travel-clamped firmware move to finish and returns the actual millisecond travel; a clamp with zero travel still raises the limit fault. UI state is lifecycle-collected; UI components do not access hardware APIs. Physical commands remain behind the controller's safety authorization path, with a persistent emergency-stop latch. Weather inference has no direct device or motor reference.

The five advisory layers are represented by camera classification and image-motion observations; a clearly provisional climatology fallback with no claimed local coverage; verified-outcome learning that accepts only explicitly verified labels; Open-Meteo forecast snapshots backed by a local cache; and a conservative safe default when evidence is missing, old, offline, or insufficient. Forecast age is re-evaluated locally without network polling: combined risk prefers data no older than 12 hours, while a snapshot at or beyond 24 hours is shown as stale. The forecast response mapping has no reliable issue-time field, so the displayed age uses retrieval time as a proxy. Optical-flow output is apparent pixel displacement only; it is not converted to wind speed. Weather risk never automatically stows or moves the panel. The verified-outcome accumulator is currently process-local and has no verified-label entry workflow wired into the UI, so it stays insufficient by default and does not survive process death.

The Today screen surfaces classifier label and confidence, image-statistic/obstruction heuristics, forecast age/location, combined advisory, provisional climatology, and verified-outcome count. Unmeasured physical weather quantities remain unmeasured; image-derived values are labeled as estimates or heuristics.

## Build and verification

The final Gradle command `:core:contracts:testDebugUnitTest :core:device:testDebugUnitTest :core:weather:testDebugUnitTest :core:tracker:testDebugUnitTest :app:assembleDebug` completed successfully. All **45 Android unit tests** passed: 24 contracts/SafetyGate, 5 device (including two new clamp regressions), 6 weather, and 10 tracker tests. The newly supplied `sim_test-1.py` passed all eight scenarios against the supplied Python tracker.

The supplied `follow_test.py` is not runnable with the supplied Python tracker: it expects `Tracker.follow()` (and helper APIs such as `find_light`/`pixel_to_angle`), while the tracker file available here exposes `Tracker.cycle()` and the ZIP does not include a replacement tracker module. This is a Python API mismatch, not an Android test failure; the follow-specific scenarios remain unvalidated until the matching Python tracker is supplied or the scenarios are ported to Kotlin.

The installable debug APK contains both `assets/sky_conditions.tflite` and `assets/sky_conditions_labels.json`. Its signature was verified with Android `apksigner` using the v2 scheme and the Android debug certificate. It is a **debug build**, not a production/release-signed package; release distribution requires an app signing key and release configuration. APK size is **78,155,662 bytes (about 74.5 MiB)**. SHA-256: `a80e0d5e3abdb502c806269c8a3459438d3b56474cf2953394efe674724102d7`.

## Limitations and deployment caution

The APK has not been installed or exercised on a physical Android phone, CameraX device, or actual ESP32/motor installation. This report does not certify Wi-Fi provisioning behavior across handset vendors, firmware endpoint compatibility, physical limit switches, stow direction, motor calibration, mount alignment, electrical safety, or weatherproofing. Test those on the real hardware under supervision before unattended operation. The very weak held-out recall for the storm-like class also means the CNN must not be used as a protective or life-safety detector.
