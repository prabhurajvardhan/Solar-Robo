# Mobile ternary runtime research

- Atomic Chat guide: https://atomic.chat/blog/guides/how-to-run-bonsai-2-locally
  - Desktop Bonsai 2 uses llama.cpp/Prism paths; Atomic's mobile builds do not expose the same self-hosted provider. Mobile catalog uses earlier 1-bit Bonsai.
- Atomic source: https://github.com/atomicbot-ai/atomic-chat
  - Local API architecture is desktop-oriented.
- llmedge: https://github.com/Aatricks/llmedge
  - Android GGUF/JNI runtime, model downloads, streaming. Version 0.3.9 on Maven Central.
  - Explicit BitNet preset uses third-party IQ2_BN repack `tdh111/bitnet-b1.58-2B-4T-GGUF/bitnet1582b4t-iq2_bn.gguf`; Prism Bonsai Q2_0 is not loadable by this runtime.
- llmedge catalog: https://raw.githubusercontent.com/Aatricks/llmedge/main/llmedge/src/main/java/io/aatricks/llmedge/model/DefaultModelCatalog.kt
  - BitNet ModelSpec includes required canonical chat template.
- llmedge API: https://raw.githubusercontent.com/Aatricks/llmedge/main/llmedge/src/main/java/io/aatricks/llmedge/LLMEdge.kt
  - `LLMEdge.create(context, scope)`, `edge.text.generate`, `edge.models.prefetch`, `edge.close`.
- Official Microsoft GGUF listing: https://huggingface.co/microsoft/bitnet-b1.58-2B-4T-gguf
  - Official file is I2_S, not the IQ2_BN artifact required by llmedge.
- Prism Bonsai demo: https://github.com/PrismML-Eng/Bonsai-demo
  - Runtime support is format/backend dependent; no ready Android integration for current Solar Robo JNI boundary.
- RunAnywhere SDK: https://github.com/RunanywhereAI/runanywhere-sdks
  - Android Kotlin llama.cpp backend exists, but exact BitNet IQ2_BN support is unverified and requires a larger build migration.
