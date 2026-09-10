# Hermes release history

All releases documented by the available release notes, newest first. Unknown early dates and undocumented version numbers are not invented. Historical entries describe behavior at that time; see the latest entry for current behavior.

## 3.6.5 · 2026-09-10

- Enlarge and raise the Warm home character, aligning its head with the greeting and feet with the main action. The layout follows measured text and button positions.
- Replace the home identity heading with a smaller date and weekday in all three skins, localized for Chinese and English.
- Add 48 bilingual short home quotes. Refresh on a new home visit or day, while keeping the current line stable during reading, theme changes and language changes.
- Reserve two caption lines in the Warm layout so changing quotes does not move the action or character. Running and pending task messages remain prioritized.
- Same-version refinement: compact the chat overflow menu in all three skins, sizing its width to the labels with smaller corners while retaining centered text and comfortable touch targets.

## 3.6.4 · 2026-09-10

- Appearance now offers Partner and Sprite launcher icons. The selected icon is retained across restarts and upgrades.
- Tap the assistant on Home to alternate between a confident folded-arms gesture and a stretch. Each gesture finishes before returning to idle; leaving the screen or backgrounding stops it.
- The two supplied clips are shipped as silent transparent animations, with support for all three home layouts and Reduce Motion.
- The glass dock clips list content to its measured lower corner contour, preserving translucency inside the dock while revealing the original scene around the corners and gesture area.
- Home greetings refresh on minute ticks, clock or time-zone changes, and foreground resume.

## 3.6.3 · 2026-09-09

- Remove the mismatched footer strip: clip route content at the gesture area to reveal the original page gradient while keeping the dock translucent.
- Center conversation menu labels and soften Glass popup shadows, sampling the background without blurring underlying text into the menu.
- Keep the screen awake during foreground continuous voice and restore normal behavior when leaving or backgrounding it.
- Tighten Warm and Quiet home spacing and budget the portrait against the viewport so common phone sizes show the main action and two recent items.
- Add inner spacing to rounded Glass history groups and align row edges and dividers.
- Add the approved welcome video with skip, background pause, reduced motion and failure fallback, and redesign both initial and saved gateway setup for all skins.
- Redesign Edit profile with separate assistant and user sections and fields, buttons and containers appropriate to each skin.
- Let new users choose local assistant identity before connecting; preserve existing identities on upgrade and keep names separate from gateway accounts and Agent Profiles.
- Replace the long feature overview with six categories and nineteen task guides, nested step pages, search and welcome replay in Chinese and English.
- Use a smaller “I’m + name” home identity header, localized in Chinese and with overflow handling for long names.

## 3.6.2 · 2026-09-09

- Adopt the selected silver-haired character app icon, adapted for circular and rounded launcher masks.
- Use the selected Hermes portrait and a generic illustrated user avatar; keep custom avatars intact.
- Give Glass inputs a distinct fill, visible border and focus state, with clear primary and secondary actions.
- Apply consistent fields to detailed settings, custom voice values, workspace paths, scheduled tasks and prompt editing.
- Restore all documented release entries, correct the 3.0.3 / 3.0.3a mix-up, and provide Chinese and English history.
- Ship a non-debuggable Release build while retaining the three home layouts, dock masking, Windows paths and language switching.

## 3.6.1-preview · 2026-09-09

- A compact glass home with two recent items, centered artwork, and no content leaking below navigation
- Consistent group corners, dropdowns, and settings controls across all three skins
- Workspaces support Windows drive paths, UNC shares, and Linux absolute paths
- Added System, Simplified Chinese, and English language options while preserving conversations and drafts

## 3.6.0-preview · 2026-09-09

- Three distinct assistant home layouts: split view, centered glass, and focused reading
- Silver-haired character animations: one-time welcome and state-aware recording and processing actions
- Custom prompt snippets with editing, sorting, deletion, and reset to defaults
- Sheets extend to the bottom of the screen while their content avoids gestures and the keyboard
- Consistent icon surfaces and selection controls, with full-width settings descriptions
- Removed the home avatar menu, simplified the conversation menu, and added reduced motion

## 3.5.1-preview · 2026-09-09

- Redesign the three skins across pages, navigation and overlays; Glass samples the page with a fallback for older Android versions.
- Group history by time and reorganize voice and model settings, allowing long model names to wrap.
- Unify menus and dialogs with explicit confirmation; clean internal preview markers and format numeric scheduled-run timestamps.

## 3.5.0-preview · 2026-09-09

- Introduce Warm, Glass and Quiet skins, with separate Light, Dark and System appearance choices.

## 3.4.1-preview · 2026-09-08

- Added a fixed daily assistant entry; Talk to me continues the same conversation
- The daily conversation is independent of project selection and is recovered on reconnect
- Shared content goes to the daily assistant by default, with other conversations available
- Removed extra acceptance and scheduling steps and separate assistant profile syncing
- Memory, files, and scheduled items use existing Hermes capabilities
- Retained recording retries, draft recovery, five-tab navigation, and the original home illustration

## 3.4.0-preview · 2026-09-08

- Rework the assistant from the 3.2 baseline with quick questions, notes, planning drafts and native scheduled follow-ups.
- Improve note synchronization, offline recovery, Profile isolation and retries after recording failures.
- The separate assistant workflow and automatic note synchronization from this release were removed in 3.4.1.

## 3.3.0-preview · 2026-09-08

- Trial the B assistant home, collaborative to-dos, voice memos and word-based text selection.
- Collaboration used a separate service. This experimental branch was not carried into 3.4, and the current app does not require that service.

## 3.2.0-preview · 2026-09-08

- Align the home artwork with its card, add the Tasks tab and mask content below navigation.
- Use actual recording levels, ambient noise and sustained speech to reduce false voice triggers.
- Add Everyday, Noisy and Quiet capture modes, adjustable while recording.

## 3.1.6-release · 2026-09-08

- Simplify the assistant home and refine floating navigation and dark-mode artwork.
- Send after a speaking pause, validate Chinese voices, read complete segmented replies and offer per-session quick answers.
- Prevent late voice results after leaving voice mode and preserve unsent drafts and attachments.

## 3.1.5-release · 2026-09-07

- Simplify chat menus, move commands into the add panel and attach workspace files to the original conversation.
- Fix file paths, encoding and source Profile reads; refine navigation, create buttons and the app icon.

## 3.1.4-release · 2026-09-07

- Simplify home menus and execution-center entry points, and unify navigation selection and approval icons.
- Extend the launcher artwork background to improve adaptive cropping.

## 3.1.3-release · 2026-09-07

- Unify switches, navigation, create buttons and page transitions; improve file-directory and project controls.
- Read the default directory from the active Profile and remember project selection per server and Profile.
- Fix stale selections, parent/child path matching and late directory responses overwriting newer ones.

## 3.1.2-release · 2026-09-07

- Introduce a sky-blue light theme, white cards and consistent home icons and navigation feedback.
- Integrate the approved character launcher icon, including system masks and a themed monochrome icon.

## 3.1.1-release · 2026-09-07

- Updated the home layout to match the reference, using the original resting-chin illustration
- Combined recent items into one card and restored compact input and navigation controls
- Fixed excess blank space caused by navigation padding when the keyboard opens
- Work details now use a progress timeline, grouped resources, and adjust/stop actions
- Home attachment and voice buttons now connect to conversation actions

## 3.1.0-release · 2026-09-07

- Added assistant home sections for pending decisions, active work, and recent conversations
- Three-tab navigation: Assistant, History, and Me, with task and file shortcuts
- Switch between the latest reply and full conversation for focused reading
- Separate decision panel with scrollable options and a fixed confirmation button
- Consistent colors, spacing, corners, and readability

## 3.0.4a-release · 2026-09-07

- Fixed Session not found when creating an empty conversation
- Shortened routine action notices, such as deletion, to about 1.5 seconds
- The running status bar no longer covers the new conversation button

## 3.0.4-release · 2026-09-07

- New conversations inherit the selected project folder; existing conversations keep their original folder
- Multiple conversations can run independently, with separate replies, stopping, queues, and recovery
- The task screen shows all active conversations, each with its own view and stop actions
- Fixed background failures affecting foreground drafts, plus attachment and project-switching issues

## 3.0.3a-release · 2026-08-23

- Prevent streamed reasoning and final replies from appearing twice.
- Restore multiple-choice clarification controls and support old and new Gateway clarification events.

## 3.0.3-release · 2026-08-22

- Allow the user avatar and Hermes avatar to be changed independently.
- Show the Hermes avatar consistently in history, replies and the assistant panel.

## 3.0.2-release · 2026-08-22

- Live Agent reasoning, with approvals, clarifications, and answer choices handled in the conversation
- Fixed premature completion status while long tasks or background subagents are still running
- Horizontally scrolling attachments; up to 10 images or files per selection
- Fixed offline background Cron checks being recorded as app crashes and improved gateway disconnect notices
- Fixed relative-path output previews, missing Memory/Soul compatibility errors, and reasoning-effort support

## 3.0.0-release · 2026-08-18

- Unified Telegram-inspired design, Hermes icons, profiles, immersive avatars, and animated empty states
- Improved conversations, full-text search, commands, task management, expert review, approvals, and background work
- Upgraded workspace and output previews, single and continuous voice, Agent STT/TTS, Chinese transcription, and long-conversation performance

## 2.8.1-release · 2026-08-18

- Refined pinned icons and the circular new-task control; removed green checks from profile bios and the gateway primary action
- Moved Cron conversations from history to task execution records; renamed memory and identity entries
- Removed partial avatar blur and added swipe-up dismissal; moved unnumbered help topics to Me
- Added looping headphone-character animations to new conversations, empty history, and help, with static fallback on older Android
- Grouped expert review results by expert so subagent output cannot be mistaken for user messages
- Unified processing and thinking layouts, moved background work to a floating status control, and added an animated voice orb
- Removed the organizing-results panel; added silent-stream checks, longer recovery windows, and improved approval/clarification resumption
- Source parsing supports reference links and parentheses in URLs, while ignoring code and image addresses

## 2.8.0-release · 2026-08-17

- Fixed system-back behavior in search, settings navigation, and returning from expert review to the assistant panel
- Assistant and attachment sheets now open fully with a drag handle; aligned completion status and increased long-reply line spacing
- Reorganized Me with direct memory and identity file entries, immersive avatars, and profile actions
- Raised the visual center of continuous voice and added microphone-driven waves that remain still in silence
- Compacted Markdown tools and new-task controls and improved initial history loading and background refresh
- Redrew the Hermes assistant icon, aligned detail sizes, and improved help, setup, and troubleshooting guidance

## 2.7.0 · 2026-08-17

- Integrated 236 refined SVG icons as native Android vectors across navigation, conversations, files, tasks, settings, and statuses
- Replaced legacy bitmap and vector references with a semantic icon catalog that adapts to light and dark modes
- Added 22dp outlined/filled navigation icons and 17–20dp content icons, retaining 40–48dp touch targets
- Added dedicated Markdown, web, folder, source conversation, save, share, and download icons
- Limited semantic colors to blue, teal, purple, green, amber, and red for actions, statuses, and file types

## 2.6.0 · 2026-08-17

- Introduced a Telegram-inspired vector icon system across navigation, home, conversations, profiles, and settings
- Outlined inactive and filled active navigation icons animate with the selection surface
- Added lighter rounded icons for search, new conversations, the composer, assistant panels, Memory/Soul, and profile actions
- Settings use white symbols on distinct semantic surfaces, adapted to light and dark modes

## 2.5.1 · 2026-08-16

- Memory/Soul entries now open the current profile's actual files instead of configuration pages
- Memory reads memories/MEMORY.md; Soul reads SOUL.md at the current profile root
- Added read-only Markdown previews with path copying, sharing, and saving, returning to Me on dismissal
- Supports servers using a host folder or HERMES_HOME directly as the file root

## 2.5.0 · 2026-08-16

- Removed unnecessary gray press overlays while retaining navigation, segmented-control, and switch animations
- Added a small gap between dropdown menus and their filter controls
- Swipe conversations left to reveal archive and delete actions, with confirmation
- Added Memory and Soul shortcuts to Me
- Avatars expand smoothly into a header image and collapse in reverse

## 2.4.0 · 2026-08-16

- Conversation completion now uses a light status row aligned with the text, without duplicating the reply
- Matched dropdown and filter typography, with left-aligned labels and right-aligned checks
- Rebuilt Me around the avatar, photo selection, profile editing, and settings
- Moved settings to a separate list and added version, gateway, and help entries
- Restored photo selection, cropping, private storage, and smooth expansion into an immersive avatar view

## 2.3.0 · 2026-08-16

- Moved expert review into the assistant panel and simplified composer attachment, command, and voice/send controls
- Aligned menu labels and checks and reduced the new-conversation button to a circle
- Added an immersive profile view with an enlarged avatar and smooth transitions
- Added sliding selection animations to navigation and file/task segments
- Unified switch spring animations and theme colors across notifications, tasks, and advanced settings

## 2.2.0 · 2026-08-16

- Removed repeated home titles and compacted the header, filters, rows, and navigation
- Compacted dropdowns and fixed pressed backgrounds extending beyond rounded corners
- Simplified search to a single toolbar and reduced composer surfaces and height
- Grouped consecutive assistant messages under one avatar and name; added compact source-conversation links to outputs
- Fixed new-conversation and task icons and compacted Tasks, Me, and Settings

## 2.1.0 · 2026-08-16

- Unified the office interface around a Telegram-inspired design, replacing the previous office/glass split
- Moved search to the top right and new conversations to a floating action; removed batch renaming
- Removed subtitles from Me entries in favor of compact grouped lists
- Added muted initial avatars and consistent surfaces and action blue across lists, sheets, and navigation
- Added the execution-center heading and Agent status, retaining light, dark, and system modes

## 2.0.0 · 2026-08-16

- Applied UI 2.0 across conversations, files, tasks, profiles, gateway, voice, and all settings
- Updated light and dark palettes with neutral content surfaces and semantic accent colors
- Office mode uses clean surfaces and light borders; glass mode uses readable translucency, thin edges, and restrained light
- Simplified the composer to neutral tools and one emphasized send action, with document-style assistant replies
- Unified floating navigation, compact menus, sheet typography, dividers, icon sizes, and muted surfaces

## 1.7.0 · 2026-08-15

- Introduced UI 2.0 typography, spacing, corners, menus, and surface hierarchy
- Reduced colored blocks, pills, shadows, and icon surfaces in office mode to clarify content priority
- Updated rounded cards to glass with soft translucency, thin borders, and light shadows
- Redesigned filters, dropdowns, conversation icons, bottom navigation, and settings lists
- Refined dark-mode semantic colors for readable headings, body text, and supporting text

## 1.6.1 · 2026-08-15

- Added Chinese transcription script options: simplified, traditional, or original
- Simplified Chinese is the default without requiring existing users to reconfigure
- Single voice, continuous voice, Agent STT, and phone recognition share script preferences

## 1.6.0 · 2026-08-15

- Restored single voice input in the composer, inserting recognized text or sending according to preferences
- Added a separate continuous-voice entry and removed the repeated text-conversation card
- Added per-profile STT/TTS providers, models, Chinese preference, and voice selection
- Added Agent STT recording tests, TTS previews, and a separate phone-service test

## 1.5.1 · 2026-08-15

- Connected composer voice input to Hermes Agent voice conversations without requiring a preinstalled phone assistant
- Detects incompatible Agent STT versions and provides clear repair guidance
- Voice error screens link directly to Agent checks and updates, then return to retry
- Improved fallback guidance when no phone speech service is available

## 1.5.0 · 2026-08-15

- Long conversations initially load the latest 60 messages, with earlier pages available on scroll
- Coalesced streaming updates and delayed full Markdown rendering until completion to reduce stutter
- Incremental output indexing scans only changed conversations
- Added a searchable, categorized Hermes command panel to the composer
- Added expert review using real subagents in deep mode and server MoA presets in quick mode
- Expert review shows progress, agreement, disagreements, evidence risks, and conclusions without raw inter-agent discussion

## 1.4.0 · 2026-08-15

- Upgraded Tasks to an execution center for approvals, clarifications, progress, and recent results
- Approval and clarification notifications link to the request or source conversation
- Added voice conversations with Agent STT/TTS, Android fallback, read-aloud, interruption, and continuous mode
- The gateway screen checks Agent version, installation method, and pending commits
- Supports safe Agent updates through the official gateway, with automatic reconnection and verification

## 1.3.0 · 2026-08-15

- Android sharing sends web pages, text, images, and common text files to Hermes
- Output previews support Markdown, PDF, isolated HTML, and common text formats
- Copy output paths, save files, share, or return to the source message
- Automatically detects Agent/Gateway versions and advertised server capabilities
- Collects reply links into reference cards
- Fixed black conversation headings and other text in dark mode

## 1.2.0 · 2026-08-15

- Added recent outputs, collecting files generated in the current profile's conversations
- Files and source conversations link to each other and locate the relevant message
- Conversation search now searches message bodies and displays matching context
- Search results open the conversation and highlight the matching message
- Added gateway API, login, live-channel, and capability diagnostics

## 1.1.0 · 2026-08-14

- Added a global running-status bar for progress and quick return outside the conversation
- Supports follow-up instructions during execution and queuing the next message
- Handle Hermes approvals and clarifications directly in the conversation
- Added completion cards with a summary and generated outputs
- System notifications open the relevant profile, conversation, or task
- Fixed unsupported PDF types in attachment selection and restored consistent avatars

## 1.0.0

- Connect remotely to self-hosted Hermes Agent and manage profiles and conversations
- Project workspaces, Markdown editing, image previews, and conversation outputs
- Scheduled tasks, system notifications, voice input, and dark mode
- Model, skill, tool, approval, memory, and context settings

## 0.8.1

- Fix jumping to the end of long replies, consolidate filters, synchronize avatar crop previews and correct Profile descriptions.

## 0.8

- Preserve drafts and failed-send attachments; add session filtering, avatar cropping and recovery from Profile failures.

## 0.6.5.11

- Add server Profile switching and restore the last selection; isolate sessions, projects, settings and caches by Profile.

## 0.6.5.10

- Show inline chat images with fullscreen previews and return opened files to the originating conversation.

## 0.6.5.9

- Turn Hermes file references into document cards and fix Markdown table row heights and borders.

## 0.6.5.8

- Temporarily use fixed user and Hermes avatars and simplify profile settings; later releases restore custom avatars.

## 0.6.5.7

- Fix imported avatars not updating by reading private image files directly and validating decoding.

## 0.6.5.6

- Update system photo selection and private avatar storage; unify back icons and support chat file links.

## 0.6.5.5

- Isolate avatar URI and decoding failures to prevent permission-related crashes, and increase launcher crop margins.

## 0.6.5.4

- Fix Android 16 avatar permission crashes, copy new avatars into private storage and fall back safely when unavailable.

## 0.6.5.3

- Adopt the approved light character icon for circular, rounded and other launcher masks.

## 0.6.5.2

- Update the brown-haired headphone character icon and monochrome outline; use the configured Hermes avatar in history.

## 0.6.5.1

- Refine navigation icons and unread dots, and improve avatar, name and launch-window transitions.

## 0.6.5

- Continue replies after leaving chat, add completion notifications and unread indicators, and improve independent identity profiles.

## 0.6.4

- Integrate the Hermes Light icon set and dark palette, and update adaptive character launcher artwork.

## 0.6.3

- Redraw semantic icons and unify actions, status, launcher and notification artwork.

## 0.6.2

- Keep the profile card fixed, refine history filters and spacing, and group task refresh and create actions.

## 0.6.1

- Improve dark-mode hierarchy with solid rounded cards; refine settings switches, user identity and Markdown tables.

## 0.6.0

- Unify Office and Glass hierarchy, spacing and inputs, and reorganize core pages and bottom navigation.

## 0.5.1

- Improve connection keepalives and recovery without resending messages; preserve partial replies and show recovery status.

