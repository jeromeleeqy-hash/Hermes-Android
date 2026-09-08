package com.qingyu.hermescompanion.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import androidx.lifecycle.ViewModelStore
import androidx.compose.runtime.MutableState
import com.qingyu.hermescompanion.data.ApiException
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureConfigStore
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
    private val a = HermesSession(id = "a", title = "Project A", profile = "default", workspacePath = "/projects/a", messageCount = 4)
    private val b = HermesSession(id = "b", title = "Project B", profile = "default", workspacePath = "/projects/b", messageCount = 4)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        stores = mockConstruction(SecureConfigStore::class.java) { store, _ ->
            `when`(store.readActiveHermesProfile()).thenReturn("default")
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
        }
        val app = mock(Application::class.java)
        val prefs = mock(SharedPreferences::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        vm = HermesViewModel(app)
        client = mock(HermesApiClient::class.java)
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
        `when`(client.createSession("/projects/a")).thenReturn(created)
        `when`(client.loadRecentMessagePage(created, 60)).thenReturn(MessagePage(emptyList(), 0, 0))
        vm.createSession()
        assertEquals(project.id, vm.uiState.selectedProjectId)
        dispatcher.scheduler.runCurrent()
        // The IO call is verified with a bounded timeout, not by asserting implementation strings.
        verify(client, timeout(3000)).createSession("/projects/a")
        awaitState { vm.uiState.selectedSession?.id == "new-a" && !vm.uiState.isBusy }
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
        `when`(client.createSession(null)).thenReturn(created)
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
        `when`(client.createSession(null)).thenReturn(created)
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
        `when`(client.createSession(null)).thenReturn(created)
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
        `when`(client.createSession("/projects/b")).thenReturn(created)
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
        `when`(client.createSession(null)).thenReturn(created)
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

}
