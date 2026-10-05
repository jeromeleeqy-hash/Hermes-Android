package com.qingyu.hermescompanion.ui

import android.app.Application
import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import androidx.lifecycle.ViewModelStore
import androidx.compose.runtime.MutableState
import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureConfigStore
import com.qingyu.hermescompanion.today.*
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.MockedConstruction
import org.mockito.Mockito.*
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrentSessionsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var stores: MockedConstruction<SecureConfigStore>
    private lateinit var vm: HermesViewModel
    private lateinit var client: HermesApiClient
    private val drafts = ConcurrentHashMap<String, String>()
    private val dailyBindings = ConcurrentHashMap<String, String>()
    private val cardBindings = ConcurrentHashMap<String, String>()
    private val manualTitles = ConcurrentHashMap<String, String>()
    private val a = HermesSession(id = "a", title = "Project A", profile = "default", workspacePath = "/projects/a", messageCount = 4)
    private val b = HermesSession(id = "b", title = "Project B", profile = "default", workspacePath = "/projects/b", messageCount = 4)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        stores = mockConstruction(SecureConfigStore::class.java) { store, _ ->
            `when`(store.readActiveHermesProfile()).thenReturn("default")
            `when`(store.readTodayOperations(anyString())).thenReturn("[]")
            `when`(store.readTodayConversation(anyString(), anyString())).thenAnswer { cardBindings[it.arguments.take(2).joinToString("|")] }
            doAnswer { cardBindings[it.arguments.take(2).joinToString("|")] = it.arguments[2] as String; null }
                .`when`(store).saveTodayConversation(anyString(), anyString(), anyString())
            `when`(store.readManualSessionTitle(anyString(), anyString())).thenAnswer { manualTitles[it.arguments.take(2).joinToString("|")] }
            doAnswer { manualTitles[it.arguments.take(2).joinToString("|")] = it.arguments[2] as String; null }
                .`when`(store).saveManualSessionTitle(anyString(), anyString(), anyString())
            `when`(store.readTaskSessionKeys(anyString())).thenReturn(emptySet())
            `when`(store.readInspectedTaskSessions(anyString())).thenReturn(emptySet())
            `when`(store.readUnreadSessionIds()).thenReturn(emptySet())
            `when`(store.readUserProfile()).thenReturn(UserProfilePreferences())
            `when`(store.readNotificationPreferences()).thenReturn(NotificationPreferences(enabled = false))
            `when`(store.readVoicePreferences()).thenReturn(VoicePreferences())
            `when`(store.readRecentArtifacts()).thenReturn(emptyList())
            `when`(store.readPendingAgentRequests()).thenReturn(emptyList())
            `when`(store.readRecentCompletions()).thenReturn(emptyList())
            `when`(store.readActiveRunSnapshots()).thenReturn(emptyList())
            `when`(store.readDraft(anyString(), anyString())).thenAnswer { drafts["${it.arguments[0]}::${it.arguments[1]}"].orEmpty() }
            doAnswer { drafts["${it.arguments[0]}::${it.arguments[1]}"] = it.arguments[2] as String; null }
                .`when`(store).saveDraft(anyString(), anyString(), anyString())
            doAnswer { drafts.remove("${it.arguments[0]}::${it.arguments[1]}"); null }
                .`when`(store).clearDraft(anyString(), anyString())
            `when`(store.readDailyConversation(anyString(), anyString(), anyString())).thenAnswer {
                dailyBindings[it.arguments.take(3).joinToString("|")]
            }
            doAnswer {
                val key = it.arguments.take(3).joinToString("|")
                val value = it.arguments[3] as String?
                if (value == null) dailyBindings.remove(key) else dailyBindings[key] = value
                null
            }.`when`(store).saveDailyConversation(anyString(), anyString(), anyString(), any())
        }
        val app = mock(Application::class.java)
        val prefs = mock(SharedPreferences::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        val assets = mock(AssetManager::class.java)
        `when`(app.assets).thenReturn(assets)
        `when`(assets.open(anyString())).thenAnswer { File("src/main/assets", it.arguments[0] as String).inputStream() }
        vm = HermesViewModel(app)
        client = mock(HermesApiClient::class.java)
        doAnswer { throw ApiException(404, "Missing directory") }.`when`(client)
            .listWorkspaceForProfile(org.mockito.ArgumentMatchers.endsWith("/.hermes-app/today"), anyString())
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.listSessions()).thenReturn(SessionPage(emptyList(), 0))
        `when`(client.projectCatalog()).thenReturn(emptyList())
        HermesViewModel::class.java.getDeclaredField("apiClient").apply { isAccessible = true }.set(vm, client)
        setState(vm.uiState.copy(route = AppRoute.CHAT, selectedSession = a, sessions = listOf(a, b)))
    }

    @After fun close() {
        runs().values.toList().forEach { vm.stopSessionRun(it.session) }
        dispatcher.scheduler.runCurrent()
        val job = vm.viewModelScope.coroutineContext[Job]!!
        ViewModelStore().apply { put("test", vm); clear() }
        awaitState { job.isCompleted }
        stores.close()
        Dispatchers.resetMain()
    }

    @Suppress("UNCHECKED_CAST")
    private fun runs(): Map<String, SessionRun> = HermesViewModel::class.java.getDeclaredField("activeRuns")
        .apply { isAccessible = true }.get(vm) as Map<String, SessionRun>

    @Suppress("UNCHECKED_CAST")
    private fun setState(state: AppUiState) {
        val value = HermesViewModel::class.java.getDeclaredField("uiState\$delegate").apply { isAccessible = true }.get(vm) as MutableState<AppUiState>
        value.value = state
    }

    private fun emit(run: SessionRun, event: StreamEvent) {
        HermesViewModel::class.java.getDeclaredMethod("handleStreamEvent", SessionRun::class.java, StreamEvent::class.java)
            .apply { isAccessible = true }.invoke(vm, run, event)
    }

    private fun startBoth(): Pair<SessionRun, SessionRun> {
        vm.updateDraft("A question"); vm.sendMessage()
        val ra = runs().getValue(a.scopedId)
        // Opening B and loading its history is orthogonal to the running A task.
        setState(vm.uiState.copy(selectedSession = b, messages = emptyList(), draft = "B question"))
        vm.sendMessage()
        return ra to runs().getValue(b.scopedId)
    }

    @Test fun streamingSwitchingAndStoppingAreIsolated() {
        val (ra, rb) = startBoth()
        assertEquals(2, vm.uiState.runningRuns.size)
        emit(ra, StreamEvent.AssistantDelta("A reply"))
        emit(rb, StreamEvent.AssistantDelta("B reply"))
        dispatcher.scheduler.advanceTimeBy(100)
        dispatcher.scheduler.runCurrent()
        assertEquals("B reply", vm.uiState.messages.last().content)
        vm.openSession(a)
        assertEquals("A reply", vm.uiState.messages.last().content)
        vm.stopGeneration()
        assertEquals(listOf("b"), vm.uiState.runningSessions.map { it.id })
        emit(ra, StreamEvent.AssistantDelta("late stale callback"))
        vm.openSession(b)
        emit(rb, StreamEvent.AssistantCompleted("B reply complete"))
        emit(rb, StreamEvent.Completed)
        assertEquals("B reply complete", vm.uiState.messages.last().content)
        assertFalse(vm.uiState.isStreaming)
        assertFalse(vm.uiState.messages.any { "A reply" in it.content || "stale" in it.content })
    }

    @Test fun queuesAndDraftsRemainWithTheirOwner() {
        val (ra, rb) = startBoth()
        vm.updateDraft("B next"); vm.queueCurrentMessage()
        vm.openSession(a)
        assertNull(vm.uiState.queuedRunMessage)
        vm.updateDraft("A next"); vm.queueCurrentMessage()
        assertEquals("A next", ra.queued?.prompt)
        assertEquals("B next", rb.queued?.prompt)
        vm.openSession(b)
        vm.updateDraft("B unsent draft")
        emit(ra, StreamEvent.AssistantCompleted("A done"))
        emit(ra, StreamEvent.Completed)
        assertEquals("B unsent draft", vm.uiState.draft)
        assertEquals("B next", vm.uiState.queuedRunMessage?.prompt)
        assertEquals("A next", runs().getValue(a.scopedId).originalPrompt)
        assertSame(rb, runs().getValue(b.scopedId))
    }

    @Test fun failedBackgroundSendDoesNotOverwriteForegroundDraftOrStopPeer() {
        val (ra, rb) = startBoth()
        vm.updateDraft("B draft to keep")
        emit(ra, StreamEvent.Error("test A failure"))
        assertEquals("B draft to keep", vm.uiState.draft)
        assertSame(rb, runs().getValue(b.scopedId))
        assertFalse(runs().containsKey(a.scopedId))
        assertEquals("A question", drafts[a.scopedId])
        assertNull(vm.uiState.failedSend)
    }

    @Test fun projectChoiceSurvivesNavigationAndIsSentToNewSession() {
        val project = HermesProject("pa", "Project A", "/projects/a")
        setState(vm.uiState.copy(projects = listOf(project)))
        vm.selectProject(project.id)
        // createSession captures the selected path before launching its network work.
        val created = a.copy(id = "new-a", runtimeId = "runtime-new-a", messageCount = 0)
        `when`(client.createSessionForProfile("/projects/a", "default")).thenReturn(created)
        `when`(client.loadRecentMessagePage(created, 60)).thenReturn(MessagePage(emptyList(), 0, 0))
        vm.createSession()
        assertEquals(project.id, vm.uiState.selectedProjectId)
        dispatcher.scheduler.runCurrent()
        // The IO call is verified with a bounded timeout, not by asserting implementation strings.
        verify(client, timeout(3000)).createSessionForProfile("/projects/a", "default")
        awaitState { vm.uiState.selectedSession?.id == "new-a" && !vm.uiState.isBusy }
    }

    @Test fun dailyEntryReusesServerConversationAcrossProjectChanges() {
        val daily = a.copy(id = "daily", title = "日常助理", workspacePath = "/daily")
        `when`(client.findSessionByTitleForProfile("日常助理", "default")).thenReturn(daily)
        `when`(client.sessionForProfile("daily", "default")).thenReturn(daily)
        `when`(client.loadRecentMessagePage(daily, 60)).thenReturn(MessagePage(emptyList(), 0, 0))
        setState(vm.uiState.copy(route = AppRoute.HOME, selectedProjectId = "unrelated-project"))
        vm.openDailyConversation(); vm.openDailyConversation()
        awaitState { vm.uiState.selectedSession?.id == "daily" && !vm.uiState.isDailyOpening && !vm.uiState.isBusy }
        assertEquals("/daily", vm.uiState.selectedSession!!.workspacePath)
        setState(vm.uiState.copy(route = AppRoute.HOME, selectedProjectId = "another-project"))
        vm.openDailyConversation()
        awaitState { vm.uiState.route == AppRoute.CHAT && !vm.uiState.isDailyOpening && !vm.uiState.isBusy }
        verify(client, times(1)).findSessionByTitleForProfile("日常助理", "default")
        verify(client, times(1)).sessionForProfile("daily", "default")
        verify(client, never()).createSessionForProfile(any(), anyString())
    }

    @Test fun dailyLookupFailureDoesNotCreateAnEmptyReplacement() {
        `when`(client.findSessionByTitleForProfile("日常助理", "default")).thenAnswer { throw ApiException(503, "暂时不可用") }
        setState(vm.uiState.copy(route = AppRoute.HOME))
        vm.openDailyConversation()
        awaitState { !vm.uiState.isDailyOpening && vm.uiState.errorMessage != null }
        assertEquals(AppRoute.HOME, vm.uiState.route)
        verify(client, never()).createSessionForProfile(any(), anyString())
    }

    @Test fun delayedDailyLookupDoesNotTakeOverAnotherPage() {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val daily = a.copy(id = "daily", title = "日常助理")
        `when`(client.findSessionByTitleForProfile("日常助理", "default")).thenAnswer {
            entered.countDown(); check(release.await(3, java.util.concurrent.TimeUnit.SECONDS)); daily
        }
        setState(vm.uiState.copy(route = AppRoute.HOME))
        vm.openDailyConversation(); dispatcher.scheduler.runCurrent()
        assertTrue(entered.await(3, java.util.concurrent.TimeUnit.SECONDS))
        setState(vm.uiState.copy(route = AppRoute.SESSIONS))
        release.countDown()
        awaitState { !vm.uiState.isDailyOpening }
        assertEquals(AppRoute.SESSIONS, vm.uiState.route)
    }

    @Test fun sharingToDailyUsesNativeConversationAndDoesNotStartAnotherSession() {
        val daily = a.copy(id = "daily", title = "日常助理")
        `when`(client.findSessionByTitleForProfile("日常助理", "default")).thenReturn(daily)
        `when`(client.loadMessages(daily, 60)).thenReturn(emptyList())
        setState(vm.uiState.copy(incomingShare = IncomingShare(sharedText = "https://example.com/video", instruction = "记一下这个开头")))
        vm.sendIncomingShare(com.qingyu.hermescompanion.assistant.DailyConversation.SHARE_TARGET)
        awaitState { runs().containsKey(daily.scopedId) }
        val run = runs().getValue(daily.scopedId)
        assertTrue(run.originalPrompt.contains("记一下这个开头"))
        assertTrue(run.originalPrompt.contains("https://example.com/video"))
        assertEquals(run.originalPrompt, com.qingyu.hermescompanion.assistant.AssistantPrompts.visibleText(run.submittedPrompt))
        assertFalse(run.submittedPrompt.contains("hermes-assistant-"))
        assertNull(vm.uiState.incomingShare)
        verify(client, never()).createSessionForProfile(any(), anyString())
    }

    @Test fun stoppingReadAloudDiscardsLateAudio() {
        val entered=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        val playback=mock(com.qingyu.hermescompanion.data.VoicePlaybackController::class.java)
        HermesViewModel::class.java.getDeclaredField("replyPlayback").apply { isAccessible=true }.set(vm,playback)
        val message=ChatMessage("read-a", MessageRole.ASSISTANT,"An English answer.")
        setState(vm.uiState.copy(messages=listOf(message), voicePreferences=VoicePreferences(engine="agent")))
        `when`(client.synthesizeSpeech(anyString(),anyString())).thenAnswer {
            entered.countDown()
            check(release.await(5,java.util.concurrent.TimeUnit.SECONDS))
            SpeechAudio(byteArrayOf(1,2,3),"audio/mpeg","test")
        }
        try {
            vm.toggleReadAloud(message)
            awaitState { entered.count==0L }
            val job=HermesViewModel::class.java.getDeclaredField("readAloudJob").apply { isAccessible=true }.get(vm) as Job
            assertEquals(message.id,vm.uiState.readAloudMessageId)
            vm.onAppBackgrounded()
            assertNull(vm.uiState.readAloudMessageId)
            release.countDown()
            awaitState { job.isCompleted }
            assertTrue(mockingDetails(playback).invocations.none { it.method.name == "play" })
        } finally { release.countDown() }
    }

    @Test fun switchingReadAloudKeepsNewAnswerActiveWhenOldRequestFinishes() {
        val firstEntered=java.util.concurrent.CountDownLatch(1)
        val secondEntered=java.util.concurrent.CountDownLatch(1)
        val firstRelease=java.util.concurrent.CountDownLatch(1)
        val secondRelease=java.util.concurrent.CountDownLatch(1)
        val playback=mock(com.qingyu.hermescompanion.data.VoicePlaybackController::class.java)
        HermesViewModel::class.java.getDeclaredField("replyPlayback").apply { isAccessible=true }.set(vm,playback)
        val first=ChatMessage("read-a",MessageRole.ASSISTANT,"First answer.")
        val second=ChatMessage("read-b",MessageRole.ASSISTANT,"Second answer.")
        setState(vm.uiState.copy(messages=listOf(first,second), voicePreferences=VoicePreferences(engine="agent")))
        `when`(client.synthesizeSpeech(anyString(),anyString())).thenAnswer {
            val old=it.arguments[0]==first.content
            (if(old)firstEntered else secondEntered).countDown()
            check((if(old)firstRelease else secondRelease).await(5,java.util.concurrent.TimeUnit.SECONDS))
            SpeechAudio(byteArrayOf(1,2,3),"audio/mpeg","test")
        }
        try {
            vm.toggleReadAloud(first)
            awaitState { firstEntered.count==0L }
            val oldJob=HermesViewModel::class.java.getDeclaredField("readAloudJob").apply { isAccessible=true }.get(vm) as Job
            vm.toggleReadAloud(second)
            awaitState { secondEntered.count==0L }
            firstRelease.countDown()
            awaitState { oldJob.isCompleted }
            assertEquals(second.id,vm.uiState.readAloudMessageId)
            assertTrue(vm.uiState.isReadAloudPreparing)
            vm.stopReadAloud()
            secondRelease.countDown()
        } finally { firstRelease.countDown();secondRelease.countDown() }
    }

    private fun awaitState(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 3_000_000_000L
        while (!predicate() && System.nanoTime() < deadline) {
            dispatcher.scheduler.runCurrent()
            Thread.sleep(5)
        }
        dispatcher.scheduler.runCurrent()
        assertTrue("ViewModel did not reach expected state: ${vm.uiState.errorMessage}", predicate())
    }

    @Test fun emptyNewRuntimeDoesNotReadUnpersistedHistoryEvenWhenReopened() {
        val created = HermesSession(id = "fresh", title = "新会话", runtimeId = "runtime-fresh")
        `when`(client.createSessionForProfile(null, "default")).thenReturn(created)
        `when`(client.loadRecentMessagePage(created, 60)).thenAnswer { throw ApiException(404, "Session not found") }
        vm.createSession()
        awaitState { vm.uiState.selectedSession?.id == "fresh" && !vm.uiState.isBusy }
        assertNull(vm.uiState.errorMessage)
        assertTrue(vm.uiState.messages.isEmpty())
        vm.openSession(created)
        dispatcher.scheduler.runCurrent()
        verify(client, never()).loadRecentMessagePage(created, 60)
        assertNull(vm.uiState.errorMessage)
    }

    @Test fun newConversationCanStartWhileAnotherTaskContinues() {
        val created = HermesSession(id = "fresh", title = "新会话", runtimeId = "runtime-fresh")
        `when`(client.createSessionForProfile(null, "default")).thenReturn(created)
        vm.updateDraft("A question")
        vm.sendMessage()
        val runningA = runs().getValue(a.scopedId)
        vm.createSession()
        awaitState { vm.uiState.selectedSession?.id == "fresh" && !vm.uiState.isBusy }
        assertSame(runningA, runs()[a.scopedId])
        assertNull(vm.uiState.errorMessage)
        vm.updateDraft("New question")
        vm.sendMessage()
        assertEquals(setOf(a.scopedId, created.scopedId), runs().keys)
        assertSame(runningA, runs()[a.scopedId])
        verify(client, never()).loadRecentMessagePage(created, 60)
    }

    @Test fun missingHistoricalSessionStillShowsItsError() {
        `when`(client.loadRecentMessagePage(b, 60)).thenAnswer { throw ApiException(404, "Session not found") }
        vm.openSession(b)
        awaitState { vm.uiState.errorMessage != null }
        assertEquals("Session not found", vm.uiState.errorMessage)
    }

    @Test fun homeStartsWithDraftAndReturnsHomeWithoutSending() {
        val created = HermesSession(id = "home-new", title = "新对话", runtimeId = "runtime-home")
        `when`(client.createSessionForProfile(null, "default")).thenReturn(created)
        setState(vm.uiState.copy(route = AppRoute.HOME, selectedSession = null))
        vm.startFromHome("整理今天的运营记录")
        awaitState { vm.uiState.selectedSession?.id == created.id && !vm.uiState.isBusy }
        assertEquals("整理今天的运营记录", vm.uiState.draft)
        assertTrue(runs().isEmpty())
        assertTrue(vm.uiState.messages.isEmpty())
        vm.backToSessions()
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertEquals("整理今天的运营记录", drafts[created.scopedId])
    }

    private fun prepareInteractiveToday(): Pair<TodayCard, String> {
        val raw = File("src/main/assets/hermes-today-interactions.json").readText()
        val board = TodayBoard.decode(raw, "/work")
        setState(vm.uiState.copy(route = AppRoute.HOME, homeMode = HomeMode.DEEP, selectedSession = null,
            today = TodayState(profile = "default", root = "/work", rootVerified = true, loaded = true, fileExists = true, board = board, rawJson = raw)))
        `when`(client.initialWorkspaceForProfile("default")).thenReturn(WorkspaceListing(path = "/work", entries = listOf(WorkspaceEntry(TodayBoard.FILE, "/work/${TodayBoard.FILE}", false))))
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "default")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", raw))
        return board.cards.first() to raw
    }

    @Test fun interactiveSubmissionChecksFreshnessAndSendsScopedAttachmentsOnce() {
        val (card, raw) = prepareInteractiveToday()
        val created = HermesSession(id = "card-action", title = "Card action", runtimeId = "runtime-card", workspacePath = "/work")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "action-123")
        vm.submitTodayInteraction(card.id, request)
        vm.submitTodayInteraction(card.id, request) // A second tap while checking cannot create another run.
        awaitState { runs().containsKey(created.scopedId) }
        verify(client, times(1)).createSessionForProfile("/work", "default")
        val run = runs().getValue(created.scopedId)
        val action = JSONObject(run.submittedAttachments.single { it.name == "today-card-action.json" }.textContent!!)
        assertEquals(todayFingerprint(raw), action.getString("expected_board_sha256"))
        assertEquals("action-123", action.getString("operation_id"))
        assertEquals("/work", action.getString("workspace"))
        assertEquals("same", action.getJSONObject("input").getString("selection"))
        assertTrue(run.submittedAttachments.any { it.name == "hermes-today-examples.json" })
        assertEquals("open", vm.uiState.today.board!!.cards.first().status)
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertEquals("running", vm.uiState.todayActions.values.single().status)
        assertEquals("", vm.uiState.draft)
        verify(client, never()).saveWorkspaceDocumentForProfile(anyString(), anyString(), anyString())
    }

    @Test fun cardCompletionAutomaticallyReadsMatchingReceiptEvenAfterSwitchingToSimpleHome() {
        val (card, raw) = prepareInteractiveToday()
        mockTodayCache()
        val created = HermesSession(id = "confirmed-action", title = "Card action", runtimeId = "runtime-card", workspacePath = "/work")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "receipt-123")
        vm.submitTodayInteraction(card.id, request)
        awaitState { runs().containsKey(created.scopedId) }
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertEquals("running", vm.uiState.todayActions.values.single().status)
        val confirmed = JSONObject(raw).apply {
            getJSONArray("cards").getJSONObject(0).put("status", "done")
            put("action_receipts", org.json.JSONArray().put(JSONObject().put("operation_id", "receipt-123")
                .put("card_id", card.id).put("status", "applied").put("message", "关联已确认，事项已收口")))
        }
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "default"))
            .thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", confirmed.toString()))
        setState(vm.uiState.copy(homeMode = HomeMode.SIMPLE))
        emit(runs().getValue(created.scopedId), StreamEvent.Completed)
        awaitState { vm.uiState.todayActions.values.singleOrNull()?.status == "applied" }
        assertEquals("关联已确认，事项已收口", vm.uiState.todayActions.values.single().message)
        assertEquals("done", vm.uiState.today.board!!.cards.first().status)
        assertEquals(AppRoute.HOME, vm.uiState.route)
        verify(client, times(1)).createSessionForProfile("/work", "default")
        verify(client, never()).saveWorkspaceDocumentForProfile(anyString(), anyString(), anyString())
    }

    @Test fun pendingApprovalUpdatesCardAndExpirationKeepsOperationForRecovery() {
        val (card, _) = prepareInteractiveToday()
        val created = HermesSession(id = "approval-action", title = "Card", runtimeId = "runtime-card", workspacePath = "/work")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        vm.submitTodayInteraction(card.id, todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "approval-original"))
        awaitState { runs().containsKey(created.scopedId) }
        val run = runs().getValue(created.scopedId)
        emit(run, StreamEvent.AgentRequestPending(AgentRequest("srq-one", "runtime-card", type = AgentRequestType.APPROVAL, title = "write JSON")))
        assertEquals("awaiting_input", vm.uiState.todayActions.values.single().status)
        assertEquals(AppRoute.HOME, vm.uiState.route)
        emit(run, StreamEvent.AgentRequestExpired("srq-one", "timeout"))
        assertEquals("uncertain", vm.uiState.todayActions.values.single().status)
        assertEquals("approval-original", vm.uiState.todayActions.values.single().operationId)
        assertTrue(vm.uiState.pendingAgentRequests.isEmpty())
    }

    @Test fun explicitRecoveryReusesSessionInputAndOperationWithoutCreatingAnotherConversation() {
        val (card, _) = prepareInteractiveToday()
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "keep-original")
        val action = TodayActionState("keep-original", card.id, "default", "/work", request, "old-operation", "uncertain")
        val previousSession = HermesSession("old-operation", "Previous card operation", workspacePath = "/work")
        setState(vm.uiState.copy(todayActions = mapOf(action.key to action), sessions = vm.uiState.sessions + previousSession))
        `when`(client.inspectAgentRequests(previousSession)).thenReturn(emptyList())
        `when`(client.isSessionConfirmedIdle(previousSession)).thenReturn(true)
        vm.discussTodayCard(card.id, "recover")
        vm.discussTodayCard(card.id, "recover")
        awaitState { runs().containsKey("default::old-operation") }
        val run = runs().getValue("default::old-operation")
        val input = JSONObject(run.submittedAttachments.single { it.name == "today-card-action.json" }.textContent!!)
        assertEquals("keep-original", input.getString("operation_id"))
        assertEquals("same", input.getJSONObject("input").getString("selection"))
        assertTrue(input.getBoolean("resume_only"))
        assertEquals(AppRoute.HOME, vm.uiState.route)
        verify(client, never()).createSessionForProfile(anyString(), anyString())
    }

    @Test fun recoveryDoesNotResubmitWhileServerSessionIsStillRunning() {
        val (card, _) = prepareInteractiveToday()
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "still-running")
        val action = TodayActionState("still-running", card.id, "default", "/work", request, "remote-running", "uncertain")
        val session = HermesSession("remote-running", "Running remotely", workspacePath = "/work")
        setState(vm.uiState.copy(todayActions = mapOf(action.key to action), sessions = listOf(session)))
        `when`(client.inspectAgentRequests(session)).thenReturn(emptyList())
        `when`(client.isSessionConfirmedIdle(session)).thenReturn(false)
        vm.discussTodayCard(card.id, "recover")
        awaitState { vm.uiState.todayActions.values.single().status == "uncertain" }
        assertTrue(runs().isEmpty())
        assertEquals("still-running", vm.uiState.todayActions.values.single().operationId)
    }

    @Test fun changedServerCardBlocksSubmissionBeforeCreatingAConversation() {
        val (card, raw) = prepareInteractiveToday()
        val changed = JSONObject(raw).apply { getJSONArray("cards").getJSONObject(0).put("summary", "Updated server record") }
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "default")).thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", changed.toString()))
        vm.submitTodayInteraction(card.id, todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString()))
        awaitState { !vm.uiState.isBusy && vm.uiState.todayActions.values.singleOrNull()?.status == "failed" }
        verify(client, never()).createSessionForProfile(any(), anyString())
        assertTrue(runs().isEmpty())
        assertEquals(AppRoute.HOME, vm.uiState.route)
    }

    @Test fun scheduleSetupSendsThePersistentContractAndExplicitTimezone() {
        prepareInteractiveToday()
        val created = HermesSession(id = "schedule", title = "Schedule setup", runtimeId = "runtime-schedule", workspacePath = "/work")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        vm.configureTodaySchedule("08:30", "20:30", "Asia/Shanghai")
        awaitState { runs().containsKey(created.scopedId) }
        val run = runs().getValue(created.scopedId)
        assertTrue(run.submittedPrompt.contains("Asia/Shanghai"))
        assertTrue(run.submittedPrompt.contains("08:30"))
        assertTrue(run.submittedPrompt.contains("hermes-app-today:"))
        assertEquals(5, run.submittedAttachments.size)
        assertEquals("configure_card_schedule", JSONObject(run.submittedAttachments.first().textContent!!).getString("mode"))
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertTrue(created.scopedId in vm.uiState.taskSessionKeys)
        verify(client, never()).saveWorkspaceDocumentForProfile(anyString(), anyString(), anyString())
    }

    @Test fun openingActiveWorkFromHomeKeepsReturnRouteAndRun() {
        vm.updateDraft("A question"); vm.sendMessage()
        val run = runs().getValue(a.scopedId)
        setState(vm.uiState.copy(route = AppRoute.HOME))
        vm.openSession(a)
        assertEquals(AppRoute.CHAT, vm.uiState.route)
        vm.backToSessions()
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertSame(run, runs()[a.scopedId])
    }

    @Test fun homeNewConversationRetainsProjectWhileAnotherRuns() {
        val project = HermesProject("pb", "Project B", "/projects/b")
        val created = b.copy(id = "home-b", runtimeId = "runtime-home-b", messageCount = 0)
        // Complete stubbing before the background stream can touch this mock.
        `when`(client.createSessionForProfile("/projects/b", "default")).thenReturn(created)
        vm.updateDraft("A question"); vm.sendMessage()
        val run = runs().getValue(a.scopedId)
        setState(vm.uiState.copy(route = AppRoute.HOME, projects = listOf(project), selectedProjectId = project.id))
        vm.startFromHome("新的运营计划")
        awaitState { vm.uiState.selectedSession?.id == created.id && !vm.uiState.isBusy }
        assertEquals("新的运营计划", vm.uiState.draft)
        assertEquals(project.id, vm.uiState.selectedProjectId)
        assertSame(run, runs()[a.scopedId])
        assertNull(vm.uiState.errorMessage)
    }

    @Test fun homeVoiceEntryIsConsumedOnceAfterCreatingSession() {
        val created = HermesSession(id="home-voice", title="新对话", runtimeId="runtime-voice")
        `when`(client.createSessionForProfile(null, "default")).thenReturn(created)
        setState(vm.uiState.copy(route=AppRoute.HOME, selectedSession=null))
        vm.startWithVoiceFromHome("已有草稿")
        awaitState { vm.uiState.selectedSession?.id == created.id && !vm.uiState.isBusy }
        assertEquals(ChatEntryAction.VOICE, vm.uiState.chatEntryAction)
        assertEquals("已有草稿", vm.uiState.draft)
        vm.consumeChatEntryAction()
        assertEquals(ChatEntryAction.NONE, vm.uiState.chatEntryAction)
        assertTrue(runs().isEmpty())
    }

    @Test fun searchReturnsToItsHomeEntryPoint() {
        setState(vm.uiState.copy(route=AppRoute.HOME))
        vm.showSessionSearch()
        assertEquals(AppRoute.SEARCH, vm.uiState.route)
        vm.closeSessionSearch()
        assertEquals(AppRoute.HOME, vm.uiState.route)
    }

    @Test fun filesTabUsesSelectedProjectInsteadOfLastChat() {
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.listWorkspaceForProfile("/projects/b","default")).thenReturn(WorkspaceListing(path="/projects/b"))
        setState(vm.uiState.copy(route=AppRoute.HOME,sessions=emptyList(),projects=listOf(HermesProject("pb","B","/projects/b")),selectedProjectId="pb"))
        vm.showWorkspace()
        awaitState { !vm.uiState.isWorkspaceLoading }
        assertEquals("/projects/b",vm.uiState.workspaceRootPath)
        verify(client,never()).listWorkspaceForProfile("/projects/a","default")
    }
    @Test fun chatFilesKeepThatConversationsDirectory() {
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.listWorkspaceForProfile("/projects/a","default")).thenReturn(WorkspaceListing(path="/projects/a"))
        setState(vm.uiState.copy(sessions=emptyList(),projects=listOf(HermesProject("pb","B","/projects/b")),selectedProjectId="pb"))
        vm.showWorkspace()
        awaitState { !vm.uiState.isWorkspaceLoading }
        assertEquals("/projects/a",vm.uiState.workspaceRootPath)
    }
    @Test fun filesWithoutProjectUseProfileDirectoryNotLastSession() {
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.initialWorkspaceForProfile("default")).thenReturn(WorkspaceListing(path="/work/default"))
        setState(vm.uiState.copy(route=AppRoute.HOME,sessions=emptyList()))
        vm.showWorkspace()
        awaitState { !vm.uiState.isWorkspaceLoading }
        assertEquals("/work/default",vm.uiState.workspaceRootPath)
    }
    @Test fun delayedOldProjectListingCannotOverwriteNewProject() {
        val started=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        val finished=java.util.concurrent.CountDownLatch(1)
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.listWorkspaceForProfile("/projects/a","default")).thenAnswer {
            started.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS)
            finished.countDown();WorkspaceListing(path="/projects/a")
        }
        `when`(client.listWorkspaceForProfile("/projects/b","default")).thenReturn(WorkspaceListing(path="/projects/b"))
        setState(vm.uiState.copy(route=AppRoute.HOME,sessions=emptyList(),projects=listOf(HermesProject("pa","A","/projects/a"),HermesProject("pb","B","/projects/b")),selectedProjectId="pa"))
        try {
            vm.showWorkspace();dispatcher.scheduler.runCurrent()
            assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS))
            vm.selectProject("pb");vm.showWorkspace()
            awaitState { vm.uiState.workspaceRootPath=="/projects/b" }
            release.countDown()
            assertTrue(finished.await(5,java.util.concurrent.TimeUnit.SECONDS))
            awaitState { vm.viewModelScope.coroutineContext[Job]!!.children.none { it.isActive } }
            assertEquals("/projects/b",vm.uiState.workspaceListing?.path)
        } finally { release.countDown() }
    }
    @Test fun switchingProfileClearsFileRootAndRestoresItsProjectChoice() {
        val active=java.util.concurrent.atomic.AtomicReference("default")
        `when`(client.currentProfile()).thenAnswer { active.get() }
        doAnswer { active.set(it.arguments[0] as String);null }.`when`(client).setProfile(anyString())
        `when`(client.listSessions()).thenReturn(SessionPage(emptyList(),0))
        `when`(client.projectCatalog()).thenReturn(listOf(HermesProject("personal-project","Personal","/work/personal")))
        `when`(stores.constructed().first().readSelectedProject("personal")).thenReturn("personal-project")
        setState(vm.uiState.copy(route=AppRoute.WORKSPACE,workspaceListing=WorkspaceListing(path="/projects/a"),workspaceRootPath="/projects/a",selectedProjectId="pa"))
        vm.selectProfile(HermesProfile("personal"))
        assertNull(vm.uiState.workspaceRootPath)
        assertNull(vm.uiState.workspaceListing)
        assertEquals("personal-project",vm.uiState.selectedProjectId)
        awaitState { !vm.uiState.isProfileSwitching && !vm.uiState.isProjectsLoading }
        assertEquals("personal",vm.uiState.activeProfile)
    }

    private fun setupAttachmentPicker() {
        `when`(client.currentProfile()).thenReturn("default")
        `when`(client.listWorkspaceForProfile("/projects/a", "default")).thenReturn(WorkspaceListing(path="/projects/a"))
        setState(vm.uiState.copy(sessions=emptyList(),draft="请帮我汇总",attachments=listOf(PendingAttachment(name="已有.txt",mimeType="text/plain",textContent="原文"))))
        vm.showWorkspaceAttachmentPicker()
        awaitState { !vm.uiState.isWorkspaceLoading }
    }
    @Test fun workspaceAttachmentReturnsToSameChatAndKeepsDraftAndExistingFiles() {
        `when`(client.readWorkspaceDocumentForProfile("/projects/a/report.md","default")).thenReturn(WorkspaceDocument("report.md","/projects/a/report.md","text/markdown","报告内容"))
        setupAttachmentPicker()
        vm.attachWorkspaceFile(WorkspaceEntry("report.md","/projects/a/report.md",false))
        awaitState { vm.uiState.route==AppRoute.CHAT }
        assertEquals(a.scopedId,vm.uiState.selectedSession?.scopedId)
        assertEquals("请帮我汇总",vm.uiState.draft)
        assertEquals(listOf("已有.txt","report.md"),vm.uiState.attachments.map { it.name })
        assertEquals("报告内容",vm.uiState.attachments.last().textContent)
        assertNull(vm.uiState.workspaceAttachmentTarget)
        assertNull(vm.uiState.workspaceDocument)
    }
    @Test fun recentArtifactAttachesSourceFileInsteadOfOpeningPreview() {
        `when`(client.readWorkspaceDocumentForProfile("/projects/b/out/report.md","default")).thenReturn(WorkspaceDocument("report.md","/projects/b/out/report.md","text/markdown","B 的报告"))
        setupAttachmentPicker()
        vm.attachRecentArtifact(RecentArtifact("default","b","B","m","out/report.md","report.md","Markdown","/projects/b"))
        awaitState { vm.uiState.route==AppRoute.CHAT }
        assertEquals(a.scopedId,vm.uiState.selectedSession?.scopedId)
        assertEquals("B 的报告",vm.uiState.attachments.last().textContent)
        assertNull(vm.uiState.workspaceDocument)
    }
    @Test fun attachmentReadFailureKeepsPickerAndDraftForRetry() {
        `when`(client.readWorkspaceDocumentForProfile("/projects/a/report.md","default")).thenAnswer { throw ApiException(403,"没有读取权限") }
        setupAttachmentPicker()
        vm.attachWorkspaceFile(WorkspaceEntry("report.md","/projects/a/report.md",false))
        awaitState { !vm.uiState.isWorkspaceAttaching }
        assertEquals(AppRoute.WORKSPACE,vm.uiState.route)
        assertNotNull(vm.uiState.workspaceAttachmentTarget)
        assertEquals("请帮我汇总",vm.uiState.draft)
        assertEquals(1,vm.uiState.attachments.size)
        assertTrue(vm.uiState.errorMessage.orEmpty().contains("权限"))
        vm.cancelWorkspaceAttachmentPicker()
        assertEquals(AppRoute.CHAT,vm.uiState.route)
    }
    @Test fun cancelledDelayedAttachmentNeverAppearsInAnotherConversation() {
        val started=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        `when`(client.readWorkspaceDocumentForProfile("/projects/a/report.md","default")).thenAnswer {
            started.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS)
            WorkspaceDocument("report.md","/projects/a/report.md","text/markdown","迟到文件")
        }
        setupAttachmentPicker()
        try {
            vm.attachWorkspaceFile(WorkspaceEntry("report.md","/projects/a/report.md",false))
            dispatcher.scheduler.runCurrent();assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS))
            vm.cancelWorkspaceAttachmentPicker()
            setState(vm.uiState.copy(selectedSession=b,attachments=emptyList(),draft="B 的草稿"))
            release.countDown()
            awaitState { vm.viewModelScope.coroutineContext[Job]!!.children.none { it.isActive } }
            assertEquals(AppRoute.CHAT,vm.uiState.route)
            assertTrue(vm.uiState.attachments.isEmpty());assertEquals("B 的草稿",vm.uiState.draft)
        } finally { release.countDown() }
    }

    @Test fun continuousVoicePreservesUnsentTextAndAttachments() {
        val attachment = PendingAttachment(name = "待发送资料.txt", mimeType = "text/plain", textContent = "原附件")
        vm.updateDraft("今天的安排")
        setState(vm.uiState.copy(attachments = listOf(attachment)))
        vm.openVoiceConversation()
        vm.submitVoiceConversationText("今天的安排")
        assertEquals("今天的安排", vm.uiState.draft)
        assertEquals(listOf(attachment), vm.uiState.attachments)
        assertTrue(runs().getValue(a.scopedId).submittedAttachments.isEmpty())
        assertEquals("今天的安排", drafts["default::a"])
    }

    @Test fun lateVoiceResultCannotSendAfterClosingOrSwitchingConversation() {
        vm.openVoiceConversation()
        vm.closeVoiceConversation()
        vm.submitVoiceConversationText("迟到的识别结果")
        assertTrue(runs().isEmpty())
        vm.openVoiceConversation()
        setState(vm.uiState.copy(selectedSession = b))
        vm.submitVoiceConversationText("不能发到 B")
        assertTrue(runs().isEmpty())
    }

    private fun mockTodayCache() {
        val cache = mock(TodayCache::class.java)
        HermesViewModel::class.java.getDeclaredField("todayCacheDelegate").apply { isAccessible = true }.set(vm, lazyOf(cache))
    }
    private fun todaySyncIdle(): Boolean = (HermesViewModel::class.java.getDeclaredField("todaySyncJob").apply { isAccessible = true }.get(vm) as Job?)?.isActive != true

    @Test fun simpleHomeSkipsPassiveReadsAndEnteringDeepOnlyReadsExistingOverview() {
        prepareInteractiveToday(); mockTodayCache()
        setState(vm.uiState.copy(homeMode = HomeMode.SIMPLE, hasSavedConnection = true))
        vm.syncToday(); vm.syncToday(force = true); dispatcher.scheduler.runCurrent()
        verify(client, never()).initialWorkspaceForProfile("default")
        vm.setHomeMode(HomeMode.DEEP)
        awaitState { todaySyncIdle() && vm.uiState.today.syncedAt != null }
        verify(client, times(1)).initialWorkspaceForProfile("default")
        verify(client, never()).createSessionForProfile(any(), anyString())
        verify(stores.constructed().first()).saveHomeMode("DEEP")
    }

    @Test fun switchingHomeRetainsRunningWorkDraftAndOverview() {
        prepareInteractiveToday()
        val overview = vm.uiState.today
        setState(vm.uiState.copy(selectedSession = a))
        val (first, second) = startBoth()
        vm.updateDraft("保留未发送的补充")
        val before = vm.uiState
        vm.setHomeMode(HomeMode.SIMPLE)
        assertEquals(HomeMode.SIMPLE, vm.uiState.homeMode)
        assertEquals(before.draft, vm.uiState.draft)
        assertEquals(before.todayActions, vm.uiState.todayActions)
        assertEquals(overview, vm.uiState.today)
        assertSame(first, runs()[a.scopedId]); assertSame(second, runs()[b.scopedId])
        assertEquals(2, vm.uiState.runningRuns.size)
        verify(stores.constructed().first(), atLeastOnce()).saveHomeMode("SIMPLE")
    }

    @Test fun todayBackgroundSyncIsSilentCoalescesAndThrottlesReentry() {
        prepareInteractiveToday(); mockTodayCache()
        setState(vm.uiState.copy(route = AppRoute.HOME, hasSavedConnection = true))
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val listing = WorkspaceListing(path = "/work", entries = listOf(WorkspaceEntry(TodayBoard.FILE, "/work/${TodayBoard.FILE}", false)))
        `when`(client.initialWorkspaceForProfile("default")).thenAnswer {
            if (calls.incrementAndGet() == 1) { started.countDown(); check(release.await(3, java.util.concurrent.TimeUnit.SECONDS)) }
            listing
        }
        try {
            vm.syncToday(); dispatcher.scheduler.runCurrent()
            awaitState { started.count == 0L }
            assertFalse(vm.uiState.today.loading)
            repeat(5) { vm.syncToday() }
            repeat(3) { vm.syncToday(force = true) }
            assertEquals(1, calls.get())
            release.countDown()
            awaitState { calls.get() == 2 && todaySyncIdle() }
            assertFalse(vm.uiState.today.loading)
            vm.syncToday(); dispatcher.scheduler.runCurrent()
            assertEquals(2, calls.get())
            vm.refreshToday(); assertTrue(vm.uiState.today.loading)
            awaitState { calls.get() == 3 && todaySyncIdle() }
            assertFalse(vm.uiState.today.loading)
        } finally { release.countDown() }
    }
    @Test fun otherPagesDoNotPollAndFailedForcedSyncRetainsSavedCards() {
        prepareInteractiveToday(); mockTodayCache()
        val original = vm.uiState.today.board
        setState(vm.uiState.copy(route = AppRoute.CHAT, hasSavedConnection = true))
        vm.syncToday(); dispatcher.scheduler.runCurrent()
        verify(client, never()).initialWorkspaceForProfile("default")
        `when`(client.initialWorkspaceForProfile("default")).thenThrow(IllegalStateException("offline"))
        vm.syncToday(force = true)
        awaitState { todaySyncIdle() && vm.uiState.today.error == "offline" }
        assertSame(original, vm.uiState.today.board)
        assertFalse(vm.uiState.today.loading)
        assertTrue(vm.uiState.today.fromCache)
    }
    @Test fun improvingBriefSendsCurrentScopeSeparatelyFromPersistentRules() {
        prepareInteractiveToday()
        val created = HermesSession(id = "improve", title = "Improve", runtimeId = "runtime-improve", profile = "default", workspacePath = "/work")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        vm.compactToday()
        awaitState { runs().containsKey(created.scopedId) }
        val run = runs().getValue(created.scopedId)
        val req = JSONObject(run.submittedAttachments.single { it.name == "hermes-today-request.json" }.textContent!!)
        assertEquals("improve_overview_and_schedule", req.getString("mode"))
        assertEquals("/work", req.getString("workspace"))
        assertEquals("default", req.getString("profile"))
        val contract = run.submittedAttachments.single { it.name == "hermes-today-contract.md" }.textContent!!
        assertTrue(contract.startsWith("# Hermes App"))
        assertFalse(contract.startsWith("Mode:"))
        assertTrue(contract.contains("不能新建") || contract.contains("不创建新任务"))
        assertTrue(run.submittedAttachments.any { it.name == "hermes-today-cron-template.txt" })
    }
    @Test fun missingProcessingConversationKeepsHomeAndShowsRecoveryMessage() {
        val (card, _) = prepareInteractiveToday()
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "lost-operation")
        val action = TodayActionState("lost-operation", card.id, "default", "/work", request, "missing", "uncertain")
        setState(vm.uiState.copy(route = AppRoute.HOME, todayActions = mapOf(action.key to action)))
        doAnswer { throw ApiException(404, "Session not found") }.`when`(client).sessionForProfile("missing", "default")
        vm.discussTodayCard(card.id, "conversation")
        awaitState { vm.uiState.noticeMessage != null }
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertEquals("lost-operation", vm.uiState.todayActions.getValue(action.key).operationId)
    }

    @Test fun lostSessionCreatesReconciliationOnlyWithOriginalOperationId() {
        val (card, _) = prepareInteractiveToday()
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "lost-original")
        val action = TodayActionState("lost-original", card.id, "default", "/work", request, "missing", "uncertain")
        val missing = HermesSession("missing", "Old", profile = "default", workspacePath = "/work")
        val recovered = HermesSession("reconcile", "Recovery", profile = "default", workspacePath = "/work", runtimeId = "runtime-reconcile")
        setState(vm.uiState.copy(route = AppRoute.HOME, todayActions = mapOf(action.key to action), sessions = listOf(missing)))
        doAnswer { throw ApiException(404, "Session not found") }.`when`(client).inspectAgentRequests(missing)
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(recovered)
        vm.discussTodayCard(card.id, "recover")
        vm.discussTodayCard(card.id, "recover")
        awaitState { runs().containsKey(recovered.scopedId) }
        val input = JSONObject(runs().getValue(recovered.scopedId).submittedAttachments.single { it.name == "today-card-action.json" }.textContent!!)
        assertEquals("lost-original", input.getString("operation_id"))
        assertTrue(input.getBoolean("reconcile_only"))
        assertEquals("missing", input.getString("original_session_id"))
        verify(client, times(1)).createSessionForProfile("/work", "default")
        assertFalse(runs().containsKey(missing.scopedId))
    }

    @Test fun recoveryNetworkFailureNeverCreatesAReplacementOperation() {
        val (card, _) = prepareInteractiveToday()
        val request = todayInteractionRequest(card, JSONObject(initialTodayInput(card)).put("selection", "same").toString(), "offline-operation")
        val action = TodayActionState("offline-operation", card.id, "default", "/work", request, "old", "uncertain")
        val old = HermesSession("old", "Old", profile="default", workspacePath="/work")
        setState(vm.uiState.copy(todayActions=mapOf(action.key to action), sessions=listOf(old)))
        doAnswer { throw java.io.IOException("offline") }.`when`(client).inspectAgentRequests(old)
        vm.discussTodayCard(card.id, "recover")
        awaitState { vm.uiState.todayActions[action.key]?.status == "uncertain" }
        assertTrue(vm.uiState.todayActions.getValue(action.key).message.contains("offline"))
        assertNotNull(vm.uiState.noticeMessage)
        verify(client, never()).createSessionForProfile(anyString(), anyString())
    }

    @Test fun manualOverviewUpdateCarriesRecentProgressAndStartsOnlyOnce() {
        prepareInteractiveToday()
        val recent = HermesSession("progress", "Recent update", profile="default", workspacePath="/work", messageCount=2)
        val created = HermesSession("overview-refresh", "Overview", profile="default", workspacePath="/work", runtimeId="runtime-overview")
        val background = recent.copy(id="old-setup", title="配置任务")
        setState(vm.uiState.copy(route=AppRoute.HOME, sessions=listOf(recent, background), taskSessionKeys=setOf(background.scopedId), selectedSession=null, hasSavedConnection=true))
        HermesViewModel::class.java.getDeclaredField("taskConversationScope").apply { isAccessible=true }.set(vm, vm.uiState.baseUrl.trimEnd('/') + "\n" + vm.uiState.username)
        `when`(client.loadOverviewMessages(recent)).thenReturn(listOf(ChatMessage(role=MessageRole.USER, content="两件待办都已经处理完成")))
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(created)
        vm.regenerateToday()
        vm.regenerateToday()
        awaitState { runs().containsKey(created.scopedId) }
        val run = runs().getValue(created.scopedId)
        val request = JSONObject(run.submittedAttachments.single { it.name == "hermes-today-request.json" }.textContent!!)
        assertEquals(vm.uiState.todayRefresh.requestId, request.getString("refresh_id"))
        assertEquals("refresh_overview_only", request.getString("mode"))
        val evidence = run.submittedAttachments.single { it.name == "today-recent-conversations.json" }.textContent!!
        assertTrue(evidence.contains("两件待办都已经处理完成"))
        assertFalse(evidence.contains("old-setup"))
        verify(client, never()).loadOverviewMessages(background)
        assertEquals(AppRoute.HOME, vm.uiState.route)
        assertTrue(vm.uiState.todayRefresh.busy)
        assertTrue(created.scopedId in vm.uiState.taskSessionKeys)
        verify(stores.constructed().single()).saveTaskSessionKeys(vm.uiState.baseUrl.trimEnd('/') + "\n" + vm.uiState.username, setOf(background.scopedId, created.scopedId))
        verify(client, times(1)).createSessionForProfile("/work", "default")
    }

    private fun bindCardConversation(session: HermesSession, cardId: String, root: String = "/work") {
        stores.constructed().single().saveTodayConversation(vm.uiState.baseUrl.trimEnd('/') + "\n" + vm.uiState.username,
            session.scopedId, TodayConversationBinding(session.profile, root, session.id, cardId).encode())
    }

    private fun completeConversation(session: HermesSession, progress: String, recovered: Boolean = false): SessionRun {
        setState(vm.uiState.copy(route = AppRoute.CHAT, selectedSession = session, messages = emptyList(), draft = progress,
            sessions = (listOf(session) + vm.uiState.sessions).distinctBy { it.scopedId }))
        vm.sendMessage()
        val run = runs().getValue(session.scopedId)
        emit(run, StreamEvent.AssistantCompleted("已了解你的最新进展。"))
        if (recovered) {
            run.recovering = true
            HermesViewModel::class.java.getDeclaredMethod("finishStreaming", SessionRun::class.java)
                .apply { isAccessible = true }.invoke(vm, run)
        } else emit(run, StreamEvent.Completed)
        return run
    }

    @Test fun cardChatAutomaticallySyncsCompletedEvidenceWithoutChangingNavigationOrClaimingCompletion() {
        val (card, raw) = prepareInteractiveToday(); mockTodayCache()
        val chat = HermesSession("card-chat", "继续聊", workspacePath = "/work", runtimeId = "chat-runtime")
        val refresh = HermesSession("auto-overview", "自动更新", workspacePath = "/work", runtimeId = "refresh-runtime")
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(chat, refresh)
        vm.discussTodayCard(card.id, "progress")
        awaitState { vm.uiState.selectedSession?.id == chat.id && !vm.uiState.isBusy }
        assertTrue(cardBindings.values.any { it.contains(card.id) })
        assertFalse(vm.uiState.attachments.any { it.name == "hermes-today-writer.py" || it.name == "hermes-today-request.json" })
        val context = vm.uiState.attachments.single { it.name == "today-card-context.json.txt" }.textContent!!
        assertFalse(context.contains("\"interaction\"")); assertFalse(context.contains("\"layout\""))
        vm.updateDraft("这件事已经完成，原记录已经核实")
        vm.sendMessage()
        val chatRun = runs().getValue(chat.scopedId)
        emit(chatRun, StreamEvent.AssistantCompleted("这件事已处理完。"))
        emit(chatRun, StreamEvent.Completed)
        vm.updateDraft("下一条尚未发送")
        assertEquals("open", vm.uiState.today.board!!.cards.first().status)
        dispatcher.scheduler.advanceTimeBy(1_100)
        awaitState { runs().containsKey(refresh.scopedId) }
        val run = runs().getValue(refresh.scopedId)
        val request = JSONObject(run.submittedAttachments.single { it.name == "hermes-today-request.json" }.textContent!!)
        assertEquals(listOf(card.id), request.getJSONArray("card_ids").let { List(it.length()) { index -> it.getString(index) } })
        val evidence = JSONObject(run.submittedAttachments.single { it.name == "today-recent-conversations.json" }.textContent!!)
        assertEquals(1, evidence.getJSONArray("conversations").length())
        assertTrue(evidence.toString().contains("这件事已经完成"))
        verify(client, never()).loadOverviewMessages(anySession())
        assertTrue(refresh.scopedId in vm.uiState.taskSessionKeys)
        assertEquals(AppRoute.CHAT, vm.uiState.route)
        assertEquals(chat.id, vm.uiState.selectedSession?.id)
        assertEquals("下一条尚未发送", vm.uiState.draft)
        val updated = JSONObject(raw).apply {
            getJSONArray("cards").getJSONObject(0).put("status", "done")
            put("refresh_receipts", org.json.JSONArray().put(JSONObject().put("request_id", request.getString("refresh_id")).put("status", "applied")))
        }
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "default"))
            .thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", updated.toString()))
        emit(run, StreamEvent.Completed)
        awaitState { vm.uiState.today.board?.cards?.first()?.status == "done" && !vm.uiState.todayRefresh.busy }
        assertEquals("", vm.uiState.todayRefresh.message)
        dispatcher.scheduler.advanceTimeBy(6_000); dispatcher.scheduler.runCurrent()
        verify(client, times(2)).createSessionForProfile("/work", "default")
        assertEquals("下一条尚未发送", vm.uiState.draft)
    }

    @Test fun recoveredAndConsecutiveCardTurnsCoalesceAndQueueOneFollowupBehindAnActiveRefresh() {
        val (card, raw) = prepareInteractiveToday(); mockTodayCache()
        val chat = HermesSession("recovered-card", "事项对话", workspacePath = "/work", messageCount = 4)
        val first = HermesSession("sync-first", "更新", workspacePath = "/work", runtimeId = "first-runtime")
        val second = first.copy(id = "sync-second", runtimeId = "second-runtime")
        bindCardConversation(chat, card.id)
        `when`(client.createSessionForProfile("/work", "default")).thenReturn(first, second)
        completeConversation(chat, "已完成第一部分", recovered = true)
        completeConversation(chat, "现在已完成第二部分")
        dispatcher.scheduler.advanceTimeBy(1_100)
        awaitState { runs().containsKey(first.scopedId) }
        val initial = runs().getValue(first.scopedId)
        assertTrue(initial.submittedAttachments.single { it.name == "today-recent-conversations.json" }.textContent!!.contains("现在已完成第二部分"))
        completeConversation(chat, "又补充了最后一点")
        dispatcher.scheduler.advanceTimeBy(1_100); dispatcher.scheduler.runCurrent()
        verify(client, times(1)).createSessionForProfile("/work", "default")
        val receipt = JSONObject(raw).put("refresh_receipts", org.json.JSONArray().put(JSONObject()
            .put("request_id", vm.uiState.todayRefresh.requestId).put("status", "applied")))
        `when`(client.readWorkspaceDocumentForProfile("/work/${TodayBoard.FILE}", "default"))
            .thenReturn(WorkspaceDocument(TodayBoard.FILE, "/work/${TodayBoard.FILE}", "application/json", receipt.toString()))
        emit(initial, StreamEvent.Completed)
        awaitState { !vm.uiState.todayRefresh.busy }
        dispatcher.scheduler.advanceTimeBy(1_100)
        awaitState { runs().containsKey(second.scopedId) }
        val followup = runs().getValue(second.scopedId)
        assertTrue(followup.submittedAttachments.single { it.name == "today-recent-conversations.json" }.textContent!!.contains("又补充了最后一点"))
        assertEquals("open", vm.uiState.today.board!!.cards.first().status)
        verify(client, times(2)).createSessionForProfile("/work", "default")
    }

    @Test fun ordinaryOrMismatchedCardConversationsNeverStartAnOverviewAgent() {
        val (card, _) = prepareInteractiveToday(); mockTodayCache()
        setState(vm.uiState.copy(homeMode = HomeMode.SIMPLE))
        bindCardConversation(a, card.id) // A belongs to /projects/a, not the card's /work.
        completeConversation(a, "随便聊聊")
        completeConversation(b, "普通聊天")
        dispatcher.scheduler.advanceTimeBy(2_000); dispatcher.scheduler.runCurrent()
        verify(client, never()).createSessionForProfile(anyString(), anyString())
        verify(client, never()).initialWorkspaceForProfile(anyString())
    }

    @Test fun manualRenameUsesExactInputAndCannotMutateAnotherProfile() {
        val name = "项目 A · 十月份详细复盘与下一阶段计划"
        `when`(client.setSessionTitleForProfile(a.id, name, a.profile)).thenReturn(name)
        vm.renameSession(a, "  $name  ")
        awaitState { vm.uiState.selectedSession?.title == name && vm.uiState.sessionActionId == null }
        verify(client).setSessionTitleForProfile(a.id, name, a.profile)
        assertTrue(manualTitles.values.contains(name))
        verify(client, never()).generateSessionTitles(anyList())
        vm.renameSession(a.copy(profile = "other"), "other title")
        vm.renameSession(a, " ")
        verify(client, times(1)).setSessionTitleForProfile(anyString(), anyString(), anyString())
    }

    private fun anySession(): HermesSession = any(HermesSession::class.java) ?: a
    private fun anyRequest(): AgentRequest = any(AgentRequest::class.java) ?: pendingRequest()
    private fun pendingRequest(id: String = "approval") = AgentRequest(id, "runtime-a", "a", AgentRequestType.APPROVAL,
        "write selected record", profile = "default", serverRequestId = id)

    @Test fun pcHandledApprovalDisappearsAfterAuthoritativeCheckIncludingLegacyRequest() {
        val request = pendingRequest().copy(serverRequestId = "")
        val other = pendingRequest("other").copy(profile = "personal")
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request, other)))
        doAnswer { invocation ->
            if ((invocation.arguments[0] as HermesSession).profile == "default") emptyList<AgentRequest>() else listOf(other)
        }.`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.pendingAgentRequests == listOf(other) }
        verify(stores.constructed().single(), atLeastOnce()).savePendingAgentRequests(listOf(other))
        verify(client, never()).respondAgentRequest(anyRequest(), anyString())
    }

    @Test fun missingSessionClearsGhostRequestButOfflineOrUnknownSnapshotKeepsIt() {
        val request = pendingRequest()
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        doAnswer { throw java.io.IOException("offline") }.`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.noticeMessage?.contains("保留") == true }
        assertEquals(listOf(request), vm.uiState.pendingAgentRequests)
        doReturn(null).`when`(client).inspectAgentRequests(anySession())
        setState(vm.uiState.copy(noticeMessage = null))
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.noticeMessage?.contains("保留") == true }
        assertEquals(listOf(request), vm.uiState.pendingAgentRequests)
        doAnswer { throw ApiException(404, "Session not found") }.`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.pendingAgentRequests.isEmpty() }
    }

    @Test fun answeringARequestAlreadyHandledOnPcNeverSubmitsApproval() {
        val request = pendingRequest()
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        `when`(client.inspectAgentRequests(anySession())).thenReturn(emptyList())
        vm.respondToAgentRequest(request, "once")
        awaitState { vm.uiState.pendingAgentRequests.isEmpty() }
        verify(client, never()).respondAgentRequest(anyRequest(), anyString())
        assertTrue(vm.uiState.noticeMessage.orEmpty().contains("其他端"))
    }

    @Test fun changedApprovalMustBeReviewedAgainBeforeSendingTheOldAnswer() {
        val request = pendingRequest()
        val changed = request.copy(title = "different command", runtimeSessionId = "new-runtime")
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        `when`(client.inspectAgentRequests(anySession())).thenReturn(listOf(changed))
        vm.respondToAgentRequest(request, "once")
        awaitState { vm.uiState.agentRequestChecks.values.any { it.message.contains("变化") } }
        assertEquals(listOf(changed), vm.uiState.pendingAgentRequests)
        verify(client, never()).respondAgentRequest(anyRequest(), anyString())
    }

    @Test fun runtimeReattachmentDoesNotDuplicateOrResetThePendingRequest() {
        val request = pendingRequest()
        val resumed = request.copy(runtimeSessionId = "new-runtime")
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        `when`(client.inspectAgentRequests(anySession())).thenReturn(listOf(resumed))
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.pendingAgentRequests == listOf(resumed) }
        assertEquals(1, vm.uiState.pendingAgentRequests.size)
    }

    @Test fun switchingConnectionDiscardsInFlightApprovalSnapshot() {
        val request = pendingRequest()
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        doAnswer { started.countDown(); release.await(5, java.util.concurrent.TimeUnit.SECONDS); emptyList<AgentRequest>() }
            .`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        dispatcher.scheduler.runCurrent()
        assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS))
        val other = request.copy(title = "Other server")
        setState(vm.uiState.copy(baseUrl = "https://other.example", pendingAgentRequests = listOf(other)))
        release.countDown()
        awaitState { !(HermesViewModel::class.java.getDeclaredField("agentRequestRefreshJob").apply { isAccessible = true }.get(vm) as Job).isActive }
        assertEquals(listOf(other), vm.uiState.pendingAgentRequests)
    }

    @Test fun checkingProgressAndUnknownStatusAreAvailableInsideTheRequestWindow() {
        val request = pendingRequest()
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        doAnswer { started.countDown(); release.await(5, java.util.concurrent.TimeUnit.SECONDS); null }
            .`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        assertTrue(vm.uiState.agentRequestChecks.values.single().checking)
        dispatcher.scheduler.runCurrent()
        assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS))
        vm.refreshPendingAgentRequests() // duplicate clicks cannot start another check
        release.countDown()
        awaitState { vm.uiState.agentRequestChecks.values.single().canDismiss }
        assertFalse(vm.uiState.agentRequestChecks.values.single().checking)
        assertTrue(vm.uiState.agentRequestChecks.values.single().message.contains("未提供"))
        assertEquals(listOf(request), vm.uiState.pendingAgentRequests)
        verify(client, times(1)).inspectAgentRequests(anySession())
        vm.dismissAgentRequestReminder(request)
        assertTrue(vm.uiState.pendingAgentRequests.isEmpty())
        verify(stores.constructed().single(), atLeastOnce()).savePendingAgentRequests(emptyList())
        verify(client, never()).respondAgentRequest(anyRequest(), anyString())
    }

    @Test fun verifiedLiveRequestCannotBeLocallyRemovedAsAnOutdatedReminder() {
        val request = pendingRequest()
        setState(vm.uiState.copy(pendingAgentRequests = listOf(request)))
        doReturn(null).`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests()
        awaitState { vm.uiState.agentRequestChecks.values.singleOrNull()?.canDismiss == true }
        doReturn(listOf(request)).`when`(client).inspectAgentRequests(anySession())
        vm.refreshPendingAgentRequests(manual = false)
        awaitState { vm.uiState.agentRequestChecks.values.singleOrNull()?.message?.contains("仍在等待") == true }
        vm.dismissAgentRequestReminder(request)
        assertEquals(listOf(request), vm.uiState.pendingAgentRequests)
        assertFalse(vm.uiState.agentRequestChecks.values.single().canDismiss)
    }

    @Test fun ordinaryUserChatStaysInConversationListAndIsNotMarkedAsTask() {
        val created = HermesSession("chat", "My conversation", runtimeId = "runtime-chat")
        `when`(client.createSessionForProfile(null, "default")).thenReturn(created)
        setState(vm.uiState.copy(route = AppRoute.HOME))
        vm.startFromHome("帮我讨论一下新的工作安排")
        awaitState { vm.uiState.selectedSession?.id == created.id }
        assertEquals(AppRoute.CHAT, vm.uiState.route)
        assertFalse(created.scopedId in vm.uiState.taskSessionKeys)
        assertTrue(runs().isEmpty())
    }

    @Test fun savedTaskClassificationIsScopedToConnectionAndAccount() {
        val store = stores.constructed().single()
        `when`(store.readTaskSessionKeys("https://one\njerome")).thenReturn(setOf(a.scopedId))
        setState(vm.uiState.copy(baseUrl="https://one", username="jerome"))
        vm.refreshSessions()
        assertEquals(setOf(a.scopedId), vm.uiState.taskSessionKeys)
        setState(vm.uiState.copy(baseUrl="https://two"))
        vm.refreshSessions()
        assertTrue(vm.uiState.taskSessionKeys.isEmpty())
    }

}
