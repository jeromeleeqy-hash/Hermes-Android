# Hermes 3.6.1-preview

This upgrade refines the three-skin interface, adds Windows workspace paths, and introduces Simplified Chinese and English UI languages.

## Interface

- The glass home budgets the actual usable viewport, including system insets and navigation. At 390 × 844 dp with normal text, both recent items and the main conversation action fit above the dock. Recent titles and previews use one line each; opening a row shows the full conversation. Small screens, large text and pending approvals retain scrolling.
- Character crops use a stable artwork anchor shared by home and conversation views. Animation frames are not independently recentered, so natural movement remains intact.
- Glass history groups have rounded first/last rows, including their swipe-action reveal. The shared navigation footer paints an opaque background through the bottom gap and system navigation region. The dock itself continues sampling the content behind it.
- Glass buttons, switches, radio controls and configuration fields use restrained highlights, subtle edges and the current theme palette. Warm and quiet home arrangements are preserved.

### Corner roles

| Role | Warm | Glass | Quiet |
|---|---:|---:|---:|
| Content panel | 22 dp | 20 dp | 2 dp |
| Button / input | 16 dp | 12 dp | 8 dp |
| Dropdown | 20 dp | 16 dp | 14 dp |
| Sheet / dialog | 24 dp | 24 dp | 24 dp |
| Floating dock | — | 28 dp | — |

Round switches, radio controls and circular icon actions retain their functional geometry. API 31+ uses the existing actual backdrop blur and shallow lens effect; API 26–30 retains the readable fallback. No additional floating bubbles or decorative particles are introduced.

## Language

Open **Me → Settings → Language**, or use **简体中文 / English** on the connection screen. Choose System default, Simplified Chinese or English.

Android 13+ uses the platform per-app locale setting, including the system language page. Older supported Android versions persist the preference and recreate a localized activity. A configuration change retains the ViewModel, conversation, drafts and attachments. App notifications and non-Compose messages use the same localized resources.

The resource pair `values/ui_strings.xml` and `values-en/ui_strings.xml` covers navigation, settings, menus, dialogs, accessibility descriptions, status and error messages, dates, default snippets and help. `localization-catalog.json` maps the stable numbered keys to source wording. Add future strings to both languages. Only application-owned literals use `uiText`; user messages, custom snippets, filenames, profile names and server-returned text remain unchanged.

Daily assistant recognition accepts both built-in titles and keeps the existing session binding and title across language changes.

## Windows workspace support

Supported examples:

- `C:\Users\Name\workspace`
- `D:/Hermes/My project`
- `\\server\share\workspace`
- `/home/user/workspace`

The project dialog accepts an absolute path directly and can browse it. The client normalizes surrounding quotes/trailing separators without changing a drive root to a drive-relative path. Directory changes preserve the exact profile and send the path through JSON-RPC. HTTP browsing preserves Windows characters through URL encoding. Project matching respects folder boundaries and handles Windows drive case and separator variations.

Windows `file:///C:/...` and `file://server/share/...` links and relative image paths are resolved using server path conventions. Relative paths such as `C:workspace` are not accepted as absolute workspaces. The server still determines whether a directory exists and whether the connected account may use it.

Path interpretation follows [Microsoft path formats](https://learn.microsoft.com/en-us/dotnet/standard/io/file-path-formats). Locale integration follows [Android per-app languages](https://developer.android.com/guide/topics/resources/app-languages).

## Rebuild

Use JDK 17 or newer, Gradle 8.13 and Android SDK / build tools 36. Run:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/the/existing-preview.keystore
```

This preview keeps application ID `com.qingyu.hermescompanion.preview` and increments versionCode to `361`. To install over an existing preview, use its original signing key. Signing keys are deliberately excluded from the source archive.
