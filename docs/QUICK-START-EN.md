# Hermes Android: quick start

Current version: **3.8.8 (388)**. Requires Android 8.0 or later and your own running Hermes Agent with a remote gateway. Models, tools, Skills and memory run on your server.

## Connect and start chatting

1. Install `Hermes-Android-3.8.8-release.apk`. It can update an existing `com.qingyu.hermescompanion.preview` installation signed with the same key, retaining local settings.
2. Watch or skip the welcome animation and set your local display names and avatars. Choose English in the language selector, or later under **Me → Settings → Language**.
3. Enter the full remote gateway URL and the same Hermes username and password used on your computer. Do not append `/api` or `/v1`; retain an existing reverse-proxy path prefix.
4. New users start with **Simple Home**. **Talk to me** continues the daily conversation. Use **History** for other conversations and projects, **Tasks** for running work and schedules, and **Files** for the server workspace.

The app does not store your password. It encrypts the login cookie with Android Keystore. See [gateway setup](SERVER_SETUP.md) for connection troubleshooting.

## Choose your Home

Open the Home gear and select **Simple Home** or **Personal assistant**. The app remembers your choice independently of the visual theme.

| Mode | What you get |
| --- | --- |
| Simple Home | The original larger character, daily chat and recent conversations. No briefing setup is required. |
| Personal assistant | Cards for current matters, unfinished work and reminders. Configure a first briefing and optional daily schedules from the same Home settings window. |

The first-briefing button disappears when an overview already exists. Daily schedules only need configuring once unless you want to change them. Legacy overview files can be organized into `.hermes-app/today/` once; new users do not need this migration. Existing configuration remains valid in 3.8.8.

After a turn in a card-linked conversation finishes, the app checks that matter's latest progress and refreshes Home automatically. Discussion alone is not treated as completion. Failed checks keep the previous state available for review.

Switching to Simple Home stops passive overview polling but keeps your files, conversations and running work. Existing server schedules continue; pause them in Tasks if desired. Automated App tasks are hidden from the mobile conversation list and remain available in Tasks. Server and PC session records are retained.

## Everyday controls

- Use **+** in a conversation to add files, images, commands, workspace references or prompt snippets. Android sharing also sends material into a draft for review before sending.
- In History, **Rename** lets you type a title, up to 120 characters. Pinning, archiving, search and project filters help organize conversations.
- Speech input, voice chat and reply playback are configured in voice settings. Availability depends on the phone or server speech services.
- **Me → Settings → Appearance** offers Warm, Glass and Quiet skins, light/dark modes and reduced motion.
- In Personal assistant Home, the new compact portrait cycles through six local animations and responds to a tap with a wink. It stops offscreen or in the background; reduced motion and Android 8 use a static portrait. Simple Home keeps its original larger character.
- Check **Me → Settings → App updates** for the APK published by the maintainer. Publishing GitHub source does not automatically update the separate APK feed.

UI language changes do not translate existing messages, filenames, custom snippets or server content. For step-by-step help, open **Me → User guide**.

[Project overview](../README.md) · [3.8.8 notes](RELEASE-3.8.8.md) · [English release history](../CHANGELOG.en.md) · [Documentation](README.md)
