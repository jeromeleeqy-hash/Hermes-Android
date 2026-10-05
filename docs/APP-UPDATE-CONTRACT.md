# Android update feed v1

Source: `https://download.leaier.com/releases/android-latest.json`

```json
{
  "schemaVersion": 1,
  "packageName": "com.qingyu.hermescompanion.preview",
  "versionCode": 382,
  "versionName": "3.8.2",
  "apkUrl": "https://download.leaier.com/releases/Hermes-Android-3.8.2-release.apk",
  "sha256": "REPLACE_WITH_ACTUAL_SHA256",
  "sizeBytes": 1,
  "minSdk": 26,
  "notes": ["中文发布说明"],
  "notesEn": ["English release notes"]
}
```

This is a schema example, not a publishable feed. Generate hash and length from the signed release APK. The delivered `android-latest.json` contains actual values.

Integers must be JSON integers. Metadata ≤64 KiB, SHA-256 exactly 64 hex characters, size 1–512 MiB, version code 1–2,100,000,000, minSdk ≥26. Each notes array has ≤12 entries of ≤500 characters. The optional `publishedAt` field may be supplied by the release pipeline; the current client ignores it.

Only HTTPS on download.leaier.com:443, a single APK filename inside `/releases/`, and no credentials, query, fragment or encoded path are accepted. The metadata client rejects redirects, never reuses gateway cookies or authorization, and bypasses stale caches. Android DownloadManager controls transport/retries of APK bytes; the client does not claim to pin its redirect chain. All bytes and APK identity must pass verification before requesting installation.

Publication order: build and sign → verify signature/package/version/minSdk → compute bytes/hash → upload versioned APK → check public HTTPS download → upload/replace metadata last. Keep previous metadata for rollback and never overwrite an already published versioned APK. Rollback hides a bad release from new downloads, not an already installed release; publish a higher version to repair installations.

Use the same current signing certificate as the installed app. The v1 client intentionally rejects signing-key changes, including rotation; plan a separate compatible key-rotation migration if needed. It supports one release channel/package per feed. Re-distributors must configure their own feed and preserve their own signing keys.

Downloads are app-specific external files under `Download/app-updates/`. FileProvider exposes only that directory in addition to the existing shared-artifact cache. Persistent preferences store download ID and the exact metadata used to start that download, so a later feed cannot change an in-progress update. Downloaded state is not an installation receipt. Only the installed package's version after restart confirms an upgrade.

Test with MockWebServer for metadata errors, Robolectric for durable download state and UI consent, and a real device + OSS for live download, process restart and system installation. Never test by falsifying the version in metadata; create a genuinely higher signed QA build if an upgrade test is needed.
