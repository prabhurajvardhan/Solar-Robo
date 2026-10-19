# Solar Robo Offline Talk Handoff

## Purpose

This document records the offline Talk foundation implemented in the current change and the work still required before claiming a production-ready ternary neural model for 4 GB Android devices.

## Implemented in this change

- Added `:feature:talk` to the application dependencies.
- Added a `Talk` route to the app navigation graph and primary navigation bar.
- Replaced the Talk module's mock AI binding with `OnDeviceSolarAiEngine`.
- Added a zero-network, deterministic solar assistant behind the existing `AiEngine` abstraction.
- Added playful but technically bounded responses for greetings, battery, panel position, weather, safety, capabilities, and unknown questions.
- Preserved the safety boundary: the AI layer returns explanations and intent metadata only; it does not actuate hardware.
- Improved Talk UI with lifecycle-aware state collection, clearer offline status, accessible input labeling, multiline composer support, and error-container presentation.
- Added `javax.inject` to `:core:ai` for the injectable local engine.

## Important limitation

`OnDeviceSolarAiEngine` is a small deterministic fallback, not a ternary neural model. It does not load model weights, perform neural inference, or provide open-domain language generation. It is intentionally fast and offline, and it gives the app a safe working Talk experience while a real model backend is selected and integrated.

Do not describe the current APK as containing a production ternary model.

## Recommended production path

1. Select a permissively licensed compact instruct model that can be converted to ternary or compatible low-bit weights.
2. Benchmark candidates on the target Vivo V2205 and representative 4 GB devices using cold start, first-token latency, sustained generation, peak RSS, and battery drain.
3. Add a native on-device runtime behind `AiEngine`, preferably with JNI/NDK isolation and a cancellation-aware streaming API.
4. Keep model files out of the Git repository. Add a reproducible download, checksum, conversion, and packaging step.
5. Store the model in app-private storage and expose model availability, version, checksum, and memory budget in diagnostics.
6. Use a small context window and bounded output tokens. Avoid loading multiple models or duplicating weight buffers.
7. Add a no-model and model-load-failure fallback to `OnDeviceSolarAiEngine`.
8. Add instrumentation tests and a device benchmark matrix before enabling the neural backend by default.
9. Keep intent extraction advisory. Any physical command must go through the deterministic Safety Gate and require explicit product-approved behavior.

## Suggested backend contract

Extend `AiEngine` only when the runtime needs streaming or lifecycle controls. A compatible direction is:

- `load(modelSource): ModelLoadResult`
- `generate(request, generationConfig): Flow<AiToken>`
- `cancelGeneration()`
- `unload()`
- `capabilities(): AiCapabilities`

The Talk UI should remain independent of the runtime. `TalkRepositoryImpl` should collect the bounded stream, append partial assistant text, and expose load/generation errors without crashing the process.

## 4 GB device guardrails

- Prefer one small quantized model rather than a general-purpose large model.
- Set a hard memory budget and fail closed to the deterministic fallback when the budget is exceeded.
- Keep context to recent messages only; the existing repository already caps context to six messages and 500 characters per message.
- Run inference off the main thread.
- Support cancellation when the screen leaves composition or the user sends a new request.
- Avoid automatic model download on first launch unless the user explicitly opts in and storage/network conditions are clear.
- Provide a visible `Offline local assistant` status so users understand the capability boundary.

## Validation completed

- `bash ./gradlew :feature:talk:test :app:compileDebugKotlin --no-daemon` — passed.
- Existing Talk ViewModel tests — passed through the feature test task.
- The app navigation now exposes Talk from the main navigation bar.

## Files changed for Talk

- `SolarRobo/app/build.gradle.kts`
- `SolarRobo/app/src/main/java/com/solarrobo/app/App.kt`
- `SolarRobo/app/src/main/java/com/solarrobo/app/navigation/AppNavHost.kt`
- `SolarRobo/core/ai/build.gradle.kts`
- `SolarRobo/core/ai/src/main/java/com/solarrobo/core/ai/OnDeviceSolarAiEngine.kt`
- `SolarRobo/feature/talk/build.gradle.kts`
- `SolarRobo/feature/talk/src/main/java/com/solarrobo/feature/talk/TalkModule.kt`
- `SolarRobo/feature/talk/src/main/java/com/solarrobo/feature/talk/components/Composer.kt`
- `SolarRobo/feature/talk/src/main/java/com/solarrobo/feature/talk/presentation/TalkScreen.kt`

## Handoff acceptance criteria for the next agent

- Provide model license and model card.
- Provide reproducible model conversion and checksum verification.
- Demonstrate offline inference with network disabled.
- Measure peak memory and latency on a 4 GB device.
- Demonstrate fallback behavior when the model is unavailable or exceeds the memory budget.
- Verify no AI output can bypass the Safety Gate.
