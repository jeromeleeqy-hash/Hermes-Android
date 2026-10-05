# Hermes Android 3.7.3

This release builds on the reconstructed 3.7.2 source (927ebfd). It does not recover or relabel the missing original 3.7.1 source. No remote repository push or user-server installation is implied.

## Changes

- Persist validated overview JSON in a private no-backup directory, AES-GCM encrypted with an Android Keystore key. Server/account/Profile are authenticated cache namespaces; the payload records the workspace root. The server remains authoritative.
- Show a saved copy before checking the saved connection. Verify the active workspace on network reads, retain same-workspace content after transient failures, and invalidate old-root data when the server reports a different workspace. A confirmed missing file removes its cache.
- Check for updates on foreground resume, about every 45 seconds while resumed, and after a completed active-Profile turn. This uses the existing gateway, with no model invocation, new server, database, push service or background schedule. Identical JSON avoids a cache rewrite. Automatic checks retain known folder counts; a manual refresh recounts.
- Reject late writes after a newer sync, profile/connection invalidation or logout. Logout clears the overview cache. Invalid or tampered encrypted content is never rendered.
- Split detail UI into question, facts, options, steps and metrics; long background and sources are collapsed. Keep conversation actions outside the scrolling body. Existing long v1 prose remains available verbatim.
- Keep schema_version=1; add optional presentation_version=1, question/facts/background/intent and coverage_note. Invalid optional presentation data is reported and retained in chat context, rather than silently mistaken for a successful structured result.
- Send separate compact_existing_only and incremental refresh requests, each carrying the actual current contract and Python writer. Short limits are enforced in the writer for presentation_version=1. Source records remain read-only during overview updates.

## Limits

Foreground polling observes the latest server file; it is not an instant notification of every intermediate generation. Force-stopped/offline phones catch up on the next active connection. The cache includes JSON and folder metadata, not full Markdown bodies or attachments. It is disposable and not a backup.

The Agent instructions constrain scope but cannot prove that a model always follows it. The writer enforces format, path boundaries, expected-file hash, snapshots and atomic replacement. Timing improvements in Agent generation require live measurement; no runtime speedup is claimed without those logs.

The release package retains the 3.7.2 preview application ID and signing certificate. Native UI tests use synthetic/deidentified fixtures. This workspace has no access to the user's live server or Android device, so live server timing, Android Keystore hardware integration and OEM background behavior require device verification.

## Reproduce

Use JDK 21, Android SDK 36/build-tools 36.0.0 and the Gradle wrapper. Supply your own signing keystore through hermesSigningFile. Do not commit or distribute signing keys, credentials, private server data or local.properties.

Run Python unittest discovery for tools/test_today_writer.py and the Android testDebugUnitTest/assembleRelease Gradle tasks with hermesPreview=true when upgrading the provided preview package. Signing with a different key cannot replace an installed package signed by the release key.

Validation results and artifact hashes are recorded with the release deliverables.
