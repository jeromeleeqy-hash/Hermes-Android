package com.qingyu.hermescompanion.model

import java.util.UUID

data class ConnectionConfig(
    val baseUrl: String,
    val username: String,
)

data class GatewayInfo(
    val agentVersion: String = "",
    val gatewayVersion: String = "",
    val capabilities: List<String> = emptyList(),
)

data class AgentUpdateCommit(
    val sha: String,
    val summary: String,
    val author: String = "",
    val at: String = "",
)

data class AgentUpdateInfo(
    val currentVersion: String = "",
    val installMethod: String = "",
    val behind: Int? = null,
    val updateAvailable: Boolean = false,
    val canApply: Boolean = false,
    val updateCommand: String = "",
    val message: String = "",
    val commits: List<AgentUpdateCommit> = emptyList(),
)

data class AgentUpdateProgress(
    val started: Boolean = false,
    val running: Boolean = false,
    val exitCode: Int? = null,
    val lines: String = "",
)

data class HermesProfile(
    val name: String,
    val path: String = "",
    val isDefault: Boolean = false,
    val model: String = "",
    val provider: String = "",
    val description: String = "",
    val skillCount: Int = 0,
    val gatewayRunning: Boolean = false,
)

data class HermesSession(
    val id: String,
    val title: String,
    val preview: String = "",
    val updatedAt: String = "",
    val source: String = "dashboard",
    val messageCount: Int = 0,
    val model: String = "",
    val provider: String = "",
    val isPinned: Boolean = false,
    val workspacePath: String = "",
    val runtimeId: String? = null,
    val profile: String = "default",
)

val HermesSession.scopedId: String
    get() = "$profile::$id"

data class SessionPage(
    val sessions: List<HermesSession>,
    val totalCount: Int,
)

data class MessagePage(
    val messages: List<ChatMessage>,
    val offset: Int,
    val totalCount: Int,
)

data class HermesProject(
    val id: String,
    val name: String,
    val primaryPath: String,
    val paths: List<String> = emptyList(),
    val isAuto: Boolean = false,
)

data class ModelProvider(
    val slug: String,
    val name: String,
    val models: List<String>,
)

data class ModelCatalog(
    val currentModel: String = "",
    val currentProvider: String = "",
    val providers: List<ModelProvider> = emptyList(),
)

data class SlashCommand(
    val command: String,
    val description: String = "",
    val category: String = "",
    val argsHint: String = "",
)

data class ModelChoice(
    val provider: String = "auto",
    val model: String = "",
)

data class FallbackModel(
    val provider: String = "",
    val model: String = "",
)

data class ServerModelSettings(
    val provider: String = "",
    val model: String = "",
    val reasoningEffort: String = "",
    val contextLength: Int = 0,
    val auxiliary: Map<String, ModelChoice> = emptyMap(),
    val fallbackModels: List<FallbackModel> = emptyList(),
    val moaReferenceModels: List<String> = emptyList(),
    val moaAggregatorModel: String = "",
)

data class ConversationStyleSettings(
    val personality: String = "",
    val timezone: String = "",
    val showReasoning: Boolean = true,
)

data class ApprovalSettings(
    val mode: String = "smart",
    val timeoutSeconds: Int = 60,
)

data class MemoryContextSettings(
    val memoryEnabled: Boolean = true,
    val userProfileEnabled: Boolean = true,
    val memoryCharLimit: Int = 2200,
    val userCharLimit: Int = 1375,
    val compressionEnabled: Boolean = true,
    val compressionThreshold: Double = 0.50,
    val compressionTargetRatio: Double = 0.20,
    val protectLastMessages: Int = 20,
)

data class ServerSttSettings(
    val enabled: Boolean = true,
    val provider: String = "local",
    val model: String = "base",
    val language: String = "zh",
)

data class ServerTtsSettings(
    val provider: String = "edge",
    val model: String = "",
    val voice: String = "zh-CN-XiaoxiaoNeural",
)

data class ServerVoiceSettings(
    val stt: ServerSttSettings = ServerSttSettings(),
    val tts: ServerTtsSettings = ServerTtsSettings(),
)

data class ServerSettings(
    val rawConfig: String = "{}",
    val models: ServerModelSettings = ServerModelSettings(),
    val conversation: ConversationStyleSettings = ConversationStyleSettings(),
    val approvals: ApprovalSettings = ApprovalSettings(),
    val memory: MemoryContextSettings = MemoryContextSettings(),
    val voice: ServerVoiceSettings = ServerVoiceSettings(),
)

data class ServerSkill(
    val name: String,
    val description: String = "",
    val category: String = "其他",
    val enabled: Boolean = true,
    val provenance: String = "",
)

data class ToolsetInfo(
    val name: String,
    val label: String = name,
    val description: String = "",
    val tools: List<String> = emptyList(),
    val enabled: Boolean = true,
    val configured: Boolean = true,
)

data class McpServerInfo(
    val name: String,
    val transport: String = "",
    val enabled: Boolean = true,
    val status: String = "",
    val toolCount: Int = 0,
)

enum class MessageRole {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val createdAt: String = "",
    val isStreaming: Boolean = false,
    val images: List<ChatImage> = emptyList(),
)

data class ChatImage(
    val name: String = "图片",
    val source: String,
    val mimeType: String = "image/*",
)

data class ToolActivity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val preview: String = "",
    val status: ToolStatus,
)

data class ChatArtifact(
    val path: String,
    val name: String,
    val kind: String,
)

data class ChatTodo(
    val id: String,
    val content: String,
    val status: TodoStatus,
)

enum class TodoStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
}

enum class ToolStatus {
    RUNNING,
    COMPLETED,
    FAILED,
}

data class PendingAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val dataUrl: String? = null,
    val textContent: String? = null,
)

data class IncomingShare(
    val sharedText: String = "",
    val instruction: String = "",
    val attachments: List<PendingAttachment> = emptyList(),
)

data class FailedSend(
    val prompt: String,
    val attachments: List<PendingAttachment> = emptyList(),
)

enum class AgentRequestType {
    APPROVAL,
    CLARIFICATION,
}

data class AgentRequestChoice(
    val label: String,
    val value: String = label,
)

data class AgentRequest(
    val requestId: String,
    val runtimeSessionId: String,
    val conversationId: String = "",
    val type: AgentRequestType,
    val title: String,
    val detail: String = "",
    val choices: List<AgentRequestChoice> = emptyList(),
    val allowSession: Boolean = true,
    val allowPermanent: Boolean = false,
    val isResponding: Boolean = false,
)

data class QueuedRunMessage(
    val session: HermesSession,
    val prompt: String,
    val attachments: List<PendingAttachment> = emptyList(),
)

data class RunCompletionSummary(
    val sessionId: String,
    val title: String,
    val summary: String,
    val artifacts: List<ChatArtifact> = emptyList(),
    val completedAtMillis: Long = System.currentTimeMillis(),
)

data class ActiveRunSnapshot(
    val profile: String,
    val sessionId: String,
    val title: String,
    val submittedPrompt: String,
    val baselineAssistantSignature: String,
    val startedAtMillis: Long,
)

data class RecentArtifact(
    val profile: String,
    val sessionId: String,
    val sessionTitle: String,
    val messageId: String = "",
    val path: String,
    val name: String,
    val kind: String,
    val seenAtMillis: Long = System.currentTimeMillis(),
)

data class SessionSearchResult(
    val session: HermesSession,
    val snippet: String,
    val messageId: String? = null,
    val matchedMessage: Boolean = false,
)

enum class DiagnosticStatus {
    CHECKING,
    PASSED,
    WARNING,
    FAILED,
}

data class ConnectionDiagnosticItem(
    val key: String,
    val title: String,
    val detail: String,
    val status: DiagnosticStatus,
)

data class WorkspaceEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long? = null,
    val modifiedAt: Double = 0.0,
    val mimeType: String? = null,
)

data class WorkspaceListing(
    val projectName: String? = null,
    val path: String,
    val parent: String? = null,
    val root: String? = null,
    val entries: List<WorkspaceEntry> = emptyList(),
)

data class WorkspaceDocument(
    val name: String,
    val path: String,
    val mimeType: String,
    val content: String,
    val bytes: ByteArray = content.toByteArray(Charsets.UTF_8),
)

enum class HermesProfileFile(val fileName: String) {
    MEMORY("MEMORY.md"),
    SOUL("SOUL.md"),
}

data class ImagePreview(
    val name: String,
    val source: String,
    val mimeType: String,
    val bytes: ByteArray,
)

data class CronSchedule(
    val kind: String = "cron",
    val expression: String,
    val display: String = expression,
)

data class CronJob(
    val id: String,
    val name: String,
    val prompt: String,
    val schedule: CronSchedule,
    val enabled: Boolean = true,
    val state: String = "scheduled",
    val deliver: String = "local",
    val nextRunAt: String = "",
    val lastRunAt: String = "",
    val lastStatus: String = "",
    val model: String = "",
    val provider: String = "",
)

data class NotificationPreferences(
    val enabled: Boolean = true,
    val messageAlerts: Boolean = true,
    val taskAlerts: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val badge: Boolean = true,
)

data class VoicePreferences(
    val enabled: Boolean = true,
    val language: String = "zh-CN",
    val transcriptScript: String = "simplified",
    val autoSend: Boolean = false,
    val engine: String = "automatic",
    val autoRead: Boolean = true,
    val continuous: Boolean = false,
    val speechRate: Float = 1.0f,
)

enum class VoicePhase {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING,
    ERROR,
}

data class VoiceConversationState(
    val active: Boolean = false,
    val phase: VoicePhase = VoicePhase.IDLE,
    val transcript: String = "",
    val provider: String = "",
    val message: String = "",
    val agentSttAvailable: Boolean? = null,
    val agentTtsAvailable: Boolean? = null,
    val requiresAgentUpdate: Boolean = false,
    val inputLevel: Float = 0f,
)

enum class VoiceCaptureTarget {
    CHAT_INPUT,
    SETTINGS_TEST,
}

data class VoiceCaptureState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val target: VoiceCaptureTarget = VoiceCaptureTarget.CHAT_INPUT,
    val transcript: String = "",
    val provider: String = "",
    val message: String = "",
    val agentSttAvailable: Boolean? = null,
    val requiresAgentUpdate: Boolean = false,
)

data class SpeechTranscription(
    val transcript: String,
    val provider: String = "",
)

data class SpeechAudio(
    val bytes: ByteArray,
    val mimeType: String,
    val provider: String = "",
)

data class UserProfilePreferences(
    val displayName: String = "",
    val bio: String = "个人工作助理",
    val avatarUri: String = "",
    val hermesDisplayName: String = "Hermes",
    val hermesAvatarUri: String = "",
)

sealed interface StreamEvent {
    data class RunStarted(val runId: String) : StreamEvent
    data class AssistantDelta(val text: String) : StreamEvent
    data class AssistantCompleted(val content: String) : StreamEvent
    data class ToolStarted(
        val name: String,
        val preview: String,
        val todos: List<ChatTodo> = emptyList(),
    ) : StreamEvent
    data class ToolCompleted(
        val name: String,
        val preview: String,
        val todos: List<ChatTodo> = emptyList(),
    ) : StreamEvent
    data class ToolProgress(val name: String, val preview: String) : StreamEvent
    data class ToolFailed(val name: String, val preview: String) : StreamEvent
    data class AgentRequestPending(val request: AgentRequest) : StreamEvent
    data class AgentRequestExpired(val requestId: String) : StreamEvent
    data class ConnectionInterrupted(val message: String) : StreamEvent
    data class Error(val message: String) : StreamEvent
    data object Completed : StreamEvent
}
