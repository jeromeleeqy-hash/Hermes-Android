# Hermes operation guide

## Getting connected

First launch, gateways and connection problems

### Connect to an Agent for the first time

1. Watch or skip the welcome clip. Choose an assistant avatar and name, then optionally enter your own name.
2. Tap “Next · Connect a gateway”. Copy the Remote URL from the desktop remote-access settings into the server address field.
3. Enter the gateway username and password configured on the computer, then tap “Verify and connect”. A successful connection opens the assistant home.

Display names only affect this app. They do not rename the Agent, Profile or gateway account.

### Change servers or sign in again

1. Open Me → Settings → Remote gateway.
2. Change the server address or username and re-enter the password.
3. Tap “Verify and update connection” and wait for confirmation. To sign out, use “Clear connection and sign out” at the bottom.

Signing out clears the gateway connection while keeping local names, avatars and appearance choices.

### Troubleshoot a failed connection

1. Check the complete Remote URL and remove any extra /api or /v1 suffix. Tap “Where do I find the address?” for help.
2. Confirm that the server and gateway are running, that your phone can reach them, and that the credentials are correct.
3. For a saved connection, expand “Diagnostics and Agent version” and tap “Diagnose current connection”. Resolve the reported failing step and retry.

## Conversations

Start chats, find history and add material

### Start or continue a conversation

1. Tap “Talk to me” on the assistant home for your daily conversation, or choose a recent conversation to continue it.
2. For a separate topic, open History and tap the compose button at the top right.
3. Type and send your request. Use ⋯ → Full conversation to read the surrounding messages.

### Find an earlier conversation

1. Open History, tap the search icon at the top right and enter a title or remembered keyword.
2. Use the project and time filters on the list to narrow the results.
3. Open a result and choose Full conversation, then scroll up to earlier messages.

### Add a file or image to a chat

1. Tap + to the left of the chat input.
2. Choose File or Image for material on your phone, or Workspace to pick a file on the server.
3. Wait for the attachment to appear in the input area, describe what you want done and send the message.

### Create reusable prompt snippets

1. Tap + in a chat, find Prompt snippets and tap Manage.
2. Add or edit a snippet, enter its name and text and save. You can also reorder or remove snippets.
3. Return to the add panel and tap a snippet to insert it into the draft. Review or edit the text before sending.

## Voice conversations

Dictation, continuous voice and recognition problems

### Dictate a message

1. Open Me → Settings → Voice settings and turn on Enable voice.
2. Return to a chat and tap the voice button beside the input. Grant microphone permission when first asked.
3. Finish recording, review the recognized text and use the available send or continue action.

### Start and interrupt continuous voice

1. Open ⋯ → Voice chat at the top right of a chat.
2. Allow recording and speak. While the assistant is answering, use the interrupt control to stop it and speak again.
3. Return to the chat to leave continuous voice. The screen stays awake while this voice page is in the foreground.

Normal screen timeout resumes in the background or after leaving the voice page. You can still lock the phone manually.

### Fix voice recognition problems

1. Check that Hermes has microphone permission in Android settings, then check your connection.
2. Open Me → Settings → Voice settings and review the voice engine and recording environment.
3. If Agent voice services are unavailable, try the automatic or device engine and test a short sentence. Chinese transcription style can be changed on the same page.

## Projects and files

Workspace paths, previews, editing and sharing

### Set a project workspace

1. Open History, switch the top view filter to Projects and tap New project.
2. Enter a project name and an absolute directory on the server, or use Browse directories.
3. For Windows, use a path such as D:\Hermes\workspace; for Linux, use /home/user/workspace. Confirm to create the project.

This is a directory on the computer running the Agent, not on your phone.

### Preview and edit a file

1. Open Files, browse to the directory and open the file.
2. For editable text such as Markdown, switch to the editor, make your changes and save.
3. Confirm that saving succeeded before leaving. Use preview or download for other file types.

### Share a file created by the assistant

1. Tap the file card in a reply, or find the file in Files.
2. Open the preview, tap Share and choose an app.
3. If the app cannot accept that file type, download it first and share it from your phone’s file manager.

## Tasks and approvals

Schedules, results and approval requests

### Create a daily task

1. Open Tasks, tap New task and enter a name.
2. Describe the work in “What should Hermes do?” and set a Cron expression. For 9:00 every day, enter 0 9 * * *.
3. Create the task, then check its schedule and prompt in the details. Review the first result and adjust as needed.

Execution time follows the server scheduler’s time zone. Check the task details.

### Review results or pause a scheduled task

1. Select a task in the scheduled list on Tasks.
2. Review the latest run status and output. Use Run now when you want to test it immediately.
3. Disable or pause a task you do not currently need, and enable it again later.

### Handle an approval request

1. Open the request from pending items in Tasks or from its approval card in the chat.
2. Read the proposed action and affected material, and check it matches your intent.
3. Allow or reject it, then return to the conversation to see the result.

## Make it yours

Names, avatars, appearance, language and memory

### Change names and avatars

1. Open Me → Edit profile.
2. Under Your assistant, change the avatar or name. Under Your profile, change your own avatar, name or bio.
3. Save your changes. The home header displays “I’m” followed by the assistant name, and other pages use the updated profile.

### Change the skin or language

1. Open Me → Settings → Appearance and choose the warm, glass or quiet skin.
2. Choose light, dark or system colors in the same panel. Enable reduced motion if you prefer fewer animations.
3. Return to Settings and open Language to choose Simplified Chinese, English or the system language.

The language setting changes the app interface. It does not translate existing chats or server files.

### Change the app icon

1. Open Me → Settings → Appearance, then scroll to App icon.
2. Choose Partner or Sprite. After the confirmation, return to your home screen.
3. Your launcher may take a few seconds to refresh. This only changes the app icon, not conversation avatars.

Your selection is kept across app restarts and upgrades.

### Play an assistant gesture

1. Tap the white-haired assistant on Home to play a gesture.
2. After it finishes, tap again for the other gesture. Repeated taps during playback do not restart it.
3. For a still character, enable Reduce Motion in Me → Settings → Appearance.

Leaving Home or backgrounding ends the gesture. Android 8 devices use still artwork.

### Edit the assistant’s long-term memory

1. Open My memory on the Me page and read the current content.
2. Enter the editor and add or revise the facts and preferences you want to retain.
3. Save, reopen and review your changes. To adjust personality and behavior, use My mind instead.

These files belong to the current Agent Profile and are separate from local names and avatars.

