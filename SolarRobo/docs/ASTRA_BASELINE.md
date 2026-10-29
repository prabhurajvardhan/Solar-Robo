# ASTRA v2 baseline record

- **Source working copy:** `/tmp/solar-robo-repo/SolarRobo`, the preserved Sensorless single-axis implementation identified by the prompt's screen strings `Sensorless single-axis solar tracker` and `Set up tracker`.
- **Upstream base:** `main` at `09ce3a3b5fcd4f923909495c7351175a0b292101` (shallow local checkout; last source commit dated 2026-10-21).
- **Imported baseline branch:** `astra-ui-v2`; baseline import commit `34478e7cec2351292ad8f1402524a461e048529f`.
- **Baseline package/version:** applicationId `com.solarrobo.app`, versionName `1.0`, versionCode `1`, minSdk 30, targetSdk 35.
- **Baseline build:** `bash ./gradlew assembleDebug --no-daemon` — **BUILD SUCCESSFUL**, 27 seconds, 710 tasks (43 executed, 667 up-to-date).
- **On-device screen mismatch:** the imported app worktree contains the Sensorless single-axis setup screen; upstream `main` contains the distinct BLE onboarding flow described in the prompt. The preserved working copy was imported so further work is reviewable against what was reported as installed.
- **Baseline screenshot:** not captured. The sandbox has neither `adb` nor an Android emulator/device attached, so an actual first-screen screenshot cannot be truthfully produced here. This remains unverified; no mock screenshot is substituted.
