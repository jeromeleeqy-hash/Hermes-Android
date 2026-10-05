# 3.7.2 verification

Validated 2026-10-03. Application source commit: 821270d89af6665aa8a6f567a712a1949b8b35a5.

- JDK 21, Gradle 8.13, SDK 36, build tools 36.0.0.
- Full :app:testDebugUnitTest: 338 tests across 61 suites; 0 failures, 0 errors, 0 skipped.
- :app:assembleRelease and release lint completed successfully.
- Today UI was rendered and inspected in CLEAN, GLASS and PAPER; tests cover expanding the list, language changes, option-to-draft callbacks, folder paths and unknown counts. Screenshots contain controlled demonstration data.
- Schema/repository tests cover old v1 JSON, blocked source references, duplicate ids, optional presentation fallback, explicit recorded progress, Profile-scoped reads, response-path validation and unknown folder counts.
- Binary attachment tests cover selected-conversation Profile, immutable upload, read-back validation, retry/conflict handling and cancellation before upload.
- Recovered writer is byte-identical to the original APK resource. Verified first write/read-back, snapshots on update, stale-SHA rejection and preservation on invalid JSON.
- APK package: com.qingyu.hermescompanion.preview; versionCode 372; versionName 3.7.2; min SDK 26; target SDK 36.
- APK Signature Scheme v2 verified; certificate SHA-256 equals the original 3.7.1 certificate: 741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6.
- zipalign 16 KiB validation passed; exactly one classes.dex; APK assets match source; no keystores or Python bytecode included.
- APK SHA-256: fb34eb0fed4b702ce73d08b7ab09716b0c17f2f5e0128704fa5032f43c0f431d.

## Remaining acceptance
No live connection to the user's Hermes server or physical Android device was made. Server compatibility and WeChat's actual share payload must still be checked on the user's device. Binary transport support is not a guarantee that proprietary chat formats can be interpreted.

The lost original 3.7.1 source was not recovered. This release is a transparent reconstruction and upgrade. GitHub publishing was blocked by missing authentication; no remote branch was created.
