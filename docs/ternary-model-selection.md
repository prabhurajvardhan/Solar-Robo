# Retired: offline ternary model selection

This document described a previous model-download and native-inference design. That implementation has been removed from the current Solar Robo Android app because the ARM runtime was not packaged in the debug build and model files are not required for the present product.

Current Talk behavior is a deterministic, rule-based offline assistant in `core/ai/OnDeviceSolarAiEngine.kt`. It has no model downloads, native inference, or model-library UI. It does not claim to be a general language model, invent measurements, or issue motor commands.

For the current sensorless single-axis tracker design, see [`TRACKER_ARCHITECTURE.md`](TRACKER_ARCHITECTURE.md).
