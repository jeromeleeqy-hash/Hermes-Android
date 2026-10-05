# 3.7.3 validation

Code commit: b97d6f0. Package: com.qingyu.hermescompanion.preview, versionCode 373, versionName 3.7.3.

- The full Android unit/native-rendering gate passed after release metadata was updated: 347 tests. The final folder-count polling optimization added one regression case; the affected Today and concurrent-session suites then passed all 53 selected tests (zero failures/errors).
- Four Python writer tests passed: new and legacy formats, actual atomic writes and snapshots, expected-hash conflicts, invalid short/presentation fields, source boundaries, and preservation of source Markdown and the prior valid JSON on rejection.
- Cache tests use the production AES-GCM implementation with an injected test key: process restart, authenticated scope isolation, tampering, invalid-response retention, workspace changes, logout late-write rejection and out-of-order sync rejection. Android Keystore key provisioning itself needs device verification.
- Native-rendered detail tests passed for legacy paragraphs of roughly screenshot length, explicit questions/facts/options, exact background retention, collapsed sources, no status change on option selection, and a 360×640 layout at 1.4 font scale. Existing Today tests cover the three skins and language changes.
- Release build and release lint succeeded. APK ZIP integrity passed; it contains one classes.dex. Bundled contract, writer, guide and history exactly match the source files.
- APK v2 signature and 16 KiB ZIP alignment checks passed. The signer certificate matches 3.7.2: SHA-256 741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6.
- APK: 64,809,561 bytes; SHA-256 58cfd8c64db09e02cbcab20d245bd0598468c024bb40cd7e53fdbb66605ebf93.

No access to the user's phone or live Hermes server was used. This does not establish live model latency, hardware-backed key behavior, end-to-end Cron timing or OEM background delivery. Foreground polling is the supported automatic sync mechanism; a stopped/offline app catches up later.

The first local build attempt could not create Gradle's process communication socket under the restricted sandbox; the authorized build succeeded with the required process permissions. An initial test gate caught stale 3.7.2 release-history metadata, which was corrected before the successful full gate.
