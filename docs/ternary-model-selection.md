# Offline ternary model decision

## Downloadable choices

The app now exposes three real GGUF ternary choices in its Offline model library:

- **Ternary Bonsai 1.7B Q2_0** — Apache-2.0, about 463 MB, recommended for 4 GB phones.
- **Microsoft BitNet b1.58 2B4T I2_S** — MIT, about 1.19 GB, recommended for 6 GB+ phones.
- **Ternary Bonsai 8B Q2_0** — Apache-2.0, about 2.18 GB, restricted to 8 GB+ devices by the recommendation logic.

The production default for the target 4 GB class of device is **Ternary Bonsai 1.7B Q2_0**. Microsoft BitNet b1.58 2B4T remains the stronger general-purpose alternative when the device has enough memory and storage.

The APK does not bundle hundreds of megabytes of weights. Users explicitly download a selected model from its publisher's Hugging Face release into app-private storage, then activate it. The current source includes the native JNI boundary and model-aware selection, but this debug build does not yet ship the ARM `solarrobo_bitnet` shared library; until that runtime is packaged, Robo Talk honestly falls back to `OnDeviceSolarAiEngine` even when a GGUF file is installed.

## Why this candidate

The official model card reports approximately 2B parameters, 4096-token maximum context, 8-bit activations, and a 0.4 GB non-embedding memory figure in its comparison. The official runtime documentation reports ARM CPU speedups and lower energy use relative to full-precision baselines. Those numbers are reference results, not a guarantee for a specific 4 GB phone; the app must still enforce a device memory budget and fall back when loading or generation is unsafe.

## Integration guardrails

The native backend is behind `AiEngine` with a JNI/NDK boundary and a safe fallback. Model files stay out of Git and are downloaded into app-private storage. Before production release, each catalog entry must add a pinned checksum and the ARM `solarrobo_bitnet` runtime must be packaged and benchmarked. The app exposes model availability, device fit, download progress, and active-model status; inference remains bounded and off the main thread.

Most importantly, AI output remains advisory. A response may explain a safe-stow recommendation or navigate the user to Control, but it must never dispatch `RoboCommand` directly. Any physical command must pass through the deterministic Safety Gate.

## Sources

- [Microsoft BitNet model card](https://huggingface.co/microsoft/bitnet-b1.58-2B-4T)
- [Microsoft BitNet / bitnet.cpp](https://github.com/microsoft/BitNet)
- [BitNet b1.58 paper](https://arxiv.org/html/2402.17764v1)
- [Ternary Bonsai 1.7B model card](https://huggingface.co/prism-ml/Ternary-Bonsai-1.7B-gguf)
- [Ternary Bonsai 8B model card](https://huggingface.co/prism-ml/Ternary-Bonsai-8B-gguf)
