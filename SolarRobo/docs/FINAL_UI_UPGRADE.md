
## Follow-up acceptance details

The start branch carried `versionCode = 2` and `versionName = "2.0.0-astra"`; this branch carries `versionCode = 3` and `versionName = "2.1.0-astra-ui"`. The new version is visible on sign-in/create-profile and Settings About.

The four-tab decision is final: Home, Track, Sky, and History. Predictions stays within the Sky/analytics surface. Settings and the existing auxiliary surfaces remain reachable from the new top-bar settings icon and existing routes; no feature was removed or hidden. The Home mockup’s Schedule control is not treated as implemented because the current app has no scheduler. Sky image 5 is the primary layout; image 4 contributes only compatible Sky/current-condition elements.

The full feature-parity table is in [`docs/ui-mapping.md`](ui-mapping.md). The seven-design status is: Sign-in **Partial** (implemented and built; runtime screenshot/biometric hardware not tested); Home **Partial** (implemented and built; live hardware values/runtime screenshot not tested); Track **Partial** (implemented and built; camera/service runtime not tested); Sky live **Partial** (existing state-backed surface; online/offline runtime not tested); Sky detail **Partial** (mockup-only values intentionally omitted); Predictions **Partial** (real state retained; mockup-only accuracy/learning values intentionally omitted); Predictions alternate navigation **Partial** (four-tab decision applied; Predictions remains an in-screen state). No design is marked Done solely from source code without runtime screenshot proof.
