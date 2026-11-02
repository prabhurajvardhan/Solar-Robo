# ASTRA UI upgrade verification — corrective pass

## Delivery

- **Branch:** `ui/design-upgrade`
- **Base branch:** `feature/astra-dashboard`
- **Investigation commit:** `61378e5`
- **Corrective pass:** visible Home, Track, shell/navigation, and sign-in compositions now use reference-led layouts and optimized hero resources.
- **Design images:** 7 reference PNGs copied under [`docs/ui-proof/reference`](ui-proof/reference).
- **Runtime proof:** no `adb`, emulator, or physical Android device exists in this sandbox. Previous-app and redesigned-running-app screenshots are therefore not claimed. See [`docs/ASTRA_VISUAL_COMPARISON.md`](ASTRA_VISUAL_COMPARISON.md).
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK size:** 78,410,975 bytes
- **APK SHA-256:** `e0a1f7afc4b0abf444eebbc1ffc43c438f54cc1358712b740954018cdccb9c6b`
- **New bundled UI assets:** 148 KB total, five compressed WebP hero crops; below the 5 MB limit.

## Verification commands

| Command | Result | Details |
|---|---|---|
| `./gradlew assembleDebug` | **PASS** | `BUILD SUCCESSFUL` after the corrective UI changes and packaged resources. |
| `./gradlew testDebugUnitTest` | **FAIL — existing failures** | Two existing `core:ai` tests fail: `OnDeviceSolarAiEngineTest.unknownQuestionsRemainBoundedAndNonActuating` and `respondsToSolarTopicsWithoutNetwork`. No tests were removed or weakened. |
| `./gradlew lintDebug` | **FAIL — existing module errors** | Lint stops in `core:camera` with 40 errors, first at `CameraXFrameSource.kt:73` for a missing opt-in annotation. |

## Investigation result

The launcher and route wiring were correct: `MainActivity` calls `SolarRoboApp`, and `AppNavHost` imports the live `app/presentation` Home, Track, and Insights screens. The prior redesign was visually insufficient because it changed theme/shell/cards but left the real screen compositions mostly as text/card stacks and did not implement the reference hero, chart, segmented-control, and navigation structures. The investigation is documented in [`docs/ASTRA_UI_INVESTIGATION.md`](ASTRA_UI_INVESTIGATION.md).

## Corrective implementation

The logged-in shell now uses a light ASTRA header with a visible red STOP pill and four-item Home/Track/Sky/History navigation. Home now uses the reference-led solar hero, Clean Energy/Smarter Motion hierarchy, real-data metric tiles, sun-position card, segmented action treatment, panel-position slider, and source tags. Track now uses the reference-led live camera hero, live/offline status, ideal-versus-actual chart card, real tracker metric tiles, and retains the existing camera, calibration, follow, jog, E-stop, and safety handlers. Sign-in now uses the supplied optimized solar hero with the existing local six-digit PIN and BiometricPrompt behavior.

## Runtime screenshot status

All seven reference images are present. No previous or redesigned running-app screenshot is presented because the sandbox has no Android runtime tooling (`adb`/emulator/device), and the previous APK was not retained. The comparison report explicitly marks each runtime screenshot as **Not captured**; no preview or mockup copy is being passed off as running-app proof.
