package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.data.*
import com.qingyu.hermescompanion.i18n.AppLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

fun todayText(zh: String, en: String) = if (AppLanguage.locale.language == "zh") zh else en

enum class TodayDomain(val key: String, val zh: String, val en: String, val glyph: String) {
    GENERAL("general", "通用", "General", "sparkles"), WORK("work", "工作", "Work", "folder"),
    LIFE("life", "生活", "Life", "calendar"), LEARNING("learning", "学习", "Learning", "file"),
    INFORMATION("information", "信息", "Information", "search"), RELATIONSHIPS("relationships", "人际", "People", "profile"),
    HEALTH("health", "健康", "Health", "heart"), FINANCE("finance", "财务", "Finance", "file"),
    FAMILY("family", "家庭", "Family", "profile"), INTERESTS("interests", "兴趣", "Interests", "sparkles"),
    TRAVEL("travel", "出行", "Travel", "calendar");
    val label get() = todayText(zh, en)
}

enum class TodayKind(val key: String, val zh: String, val en: String) {
    DECISION("decision", "待我决定", "Decide"), SCHEDULE("schedule", "时间安排", "Schedule"),
    FOLLOWUP("followup", "持续跟进", "Follow up"), UPDATE("update", "重要变化", "Updates");
    val label get() = todayText(zh, en)
}

data class TodayOption(val title: String, val detail: String = "")
data class TodayStep(val title: String, val done: Boolean)
data class TodayMetric(val label: String, val value: String, val unit: String = "")
enum class TodayAttentionGroup(val key: String, val zh: String, val en: String) {
    FOCUS("focus", "关注中", "In focus"), FOLLOWUP("followup", "待收尾", "To follow up"), REMINDER("reminder", "提醒你", "Worth a look");
    val label get() = todayText(zh, en)
}
data class TodayAttention(val group: TodayAttentionGroup, val reason: String = "", val priority: Int = 2)
data class TodayPresentation(
    val options: List<TodayOption> = emptyList(), val steps: List<TodayStep> = emptyList(),
    val metrics: List<TodayMetric> = emptyList(),
    val question: String = "", val facts: List<String> = emptyList(), val background: String = "",
    val intent: String = "", val issue: String? = null, val invalidData: String = "",
    val interaction: TodayInteraction? = null,
    val layout: String = "", val caption: String = "",
)

data class TodayCard(
    val id: String, val title: String, val summary: String, val kind: TodayKind,
    val domain: TodayDomain = TodayDomain.GENERAL, val status: String = "open", val whenLabel: String = "",
    val sources: List<String> = emptyList(), val sessionId: String = "", val unavailableSources: List<String> = emptyList(),
    val presentation: TodayPresentation = TodayPresentation(),
    val attention: TodayAttention? = null,
) {
    val attentionGroup get() = attention?.group ?: when (kind) {
        TodayKind.DECISION, TodayKind.FOLLOWUP -> TodayAttentionGroup.FOLLOWUP
        TodayKind.SCHEDULE -> TodayAttentionGroup.FOCUS
        TodayKind.UPDATE -> TodayAttentionGroup.REMINDER
    }
    val isClosed get() = status == "done" || status == "archived"
    val label get() = if (kind == TodayKind.DECISION && presentation.intent == "clarify") todayText("待我确认", "Clarify") else kind.label
    val needsStructure get() = presentation.layout.isBlank() || title.length > 48 || presentation.issue != null
    // Preserve complete sentences. A short question is not a substitute for context.
    val readableContext: String get() {
        if (presentation.caption.isNotBlank()) return presentation.caption
        val text = summary.trim()
        if (text.length <= 90) return text
        val sentences = Regex("[^。！？!?\\n]+[。！？!?]|[^。！？!?\\n]+$").findAll(text).map { it.value.trim() }.toList()
        val first = sentences.firstOrNull().orEmpty()
        if (first.isNotBlank() && first.length <= 110) return first
        return ""
    }
    // Preserve old prose verbatim, but never expand it by default.
    val backgroundText get() = listOf(presentation.background, summary.takeIf { it.length > 160 }.orEmpty())
        .filter(String::isNotBlank).distinct().joinToString("\n\n")
    fun contextDocument() = JSONObject().apply {
        put("id", id); put("title", title); put("summary", summary); put("kind", kind.key)
        put("domain", domain.key); put("status", status); put("when", whenLabel)
        put("question", presentation.question); put("facts", JSONArray(presentation.facts))
        put("background", presentation.background); put("intent", presentation.intent)
        put("layout", presentation.layout); put("caption", presentation.caption)
        attention?.let { put("attention", JSONObject().put("group", it.group.key).put("reason", it.reason).put("priority", it.priority)) }
        presentation.interaction?.let { put("interaction", JSONObject(it.raw)) }
        if (presentation.issue != null) put("invalid_presentation_read_only", presentation.invalidData)
        put("sources", JSONArray(sources)); put("unavailable_sources_read_only", JSONArray(unavailableSources))
        put("options", JSONArray(presentation.options.map { JSONObject().put("title", it.title).put("detail", it.detail) }))
        put("steps", JSONArray(presentation.steps.map { JSONObject().put("title", it.title).put("status", if (it.done) "done" else "open") }))
        put("metrics", JSONArray(presentation.metrics.map { JSONObject().put("label", it.label).put("value", it.value).put("unit", it.unit) }))
    }.toString(2)
}

data class TodayBoard(val date: LocalDate, val generatedAt: OffsetDateTime, val headline: String, val summary: String, val cards: List<TodayCard>, val coverageNote: String = "", val presentationVersion: Int = 0) {
    companion object {
        const val FILE = "hermes-today.json"
        const val DIRECTORY = ".hermes-app/today"
        const val PATH = "$DIRECTORY/$FILE"
        const val MAX_BYTES = 1024 * 1024
        fun decode(text: String, root: String): TodayBoard {
            require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Overview exceeds 1 MiB" }
            require(isAbsoluteRemotePath(root)) { "Invalid workspace root" }
            val obj = JSONObject(text)
            require(obj.get("schema_version") == 1) { "Unsupported schema_version" }
            val version = obj.optInt("presentation_version", 0)
            require(version in 0..4) { "Unsupported presentation_version" }
            val date = LocalDate.parse(obj.requiredText("date", 10))
            val generated = OffsetDateTime.parse(obj.requiredText("generated_at", 80))
            val list = obj.getJSONArray("cards")
            require(list.length() <= 200) { "Too many cards" }
            val ids = mutableSetOf<String>()
            val cards = (0 until list.length()).map { index ->
                val card = list.getJSONObject(index)
                val id = card.requiredText("id", 96)
                require(id.matches(Regex("[A-Za-z0-9_-]{1,96}")) && ids.add(id)) { "Invalid or duplicate card id" }
                val kind = TodayKind.entries.firstOrNull { it.key == card.requiredText("kind", 20) }
                    ?: error("Invalid card kind")
                val domain = TodayDomain.entries.firstOrNull { it.key == card.optionalText("domain", 30).ifBlank { "general" } }
                    ?: error("Invalid domain")
                val status = card.optionalText("status", 20).ifBlank { "open" }
                require(status in setOf("open", "waiting", "done", "archived")) { "Invalid status" }
                val refs = if (!card.has("sources")) emptyList() else card.getJSONArray("sources").let { array ->
                    require(array.length() <= 20) { "Too many sources" }
                    (0 until array.length()).map { require(array.get(it) is String); array.getString(it).also { s -> require(s.length <= 4096) } }
                }
                val safe = refs.mapNotNull { safeTodayPath(root, it) }.distinct()
                val blocked = refs.filter { safeTodayPath(root, it) == null }.distinct()
                TodayCard(id, card.requiredText("title", 180), card.requiredText("summary", 8000, allowEmpty = true),
                    kind, domain, status, card.optionalText("when", 120), safe, card.optionalText("session_id", 180), blocked,
                    parsePresentation(card, version), parseAttention(card))
            }
            return TodayBoard(date, generated, obj.requiredText("headline", 180, true), obj.requiredText("summary", 12000, true), cards, obj.optionalText("coverage_note", 160), version)
        }
    }
}

// Optional visual hints can never prevent valid v1 text from being displayed.
private fun parseAttention(card: JSONObject): TodayAttention? = runCatching {
    val value = card.optJSONObject("attention") ?: return null
    val group = TodayAttentionGroup.entries.firstOrNull { it.key == value.requiredText("group", 16) } ?: error("Invalid attention group")
    val priority = if (value.has("priority")) value.get("priority").also { require(it is Int && it in 1..3) } as Int else 2
    TodayAttention(group, value.optionalText("reason", 100), priority)
}.getOrNull()

private fun parsePresentation(card: JSONObject, version: Int = 0): TodayPresentation = runCatching {
    val strict = version >= 3
    require(!strict || card.has("presentation")) { "Missing presentation" }
    if (!card.has("presentation")) return TodayPresentation()
    val obj = card.getJSONObject("presentation")
    val layout = obj.optionalText("layout", 20)
    if (strict) {
        require(layout in (if (version >= 4) setOf("interactive", "note", "metrics", "receipt", "schedule", "progress") else setOf("interactive", "note", "metrics", "receipt"))) { "Choose an explicit card layout" }
        require(obj.requiredText("caption", 100).isNotBlank()) { "Missing readable caption" }
        require((layout == "interactive") == obj.has("interaction")) { "Layout and interaction do not match" }
        if (version == 3) {
            require(layout == "interactive" || card.getString("kind") !in setOf("decision", "followup")) { "Decision and follow-up require interactive layout" }
            require(layout == "interactive" || obj.optJSONArray("options")?.length() in listOf(null, 0)) { "Options require interactive layout" }
            require(layout == "interactive" || obj.optJSONArray("steps")?.length() in listOf(null, 0)) { "Steps require interactive layout" }
        }
        if (layout == "schedule") require(card.optionalText("when", 120).isNotBlank()) { "Schedule requires a known time" }
        if (layout == "progress") require((obj.optJSONArray("steps")?.length() ?: 0) > 0) { "Progress requires recorded steps" }
        if (layout in setOf("metrics", "receipt")) require((obj.optJSONArray("metrics")?.length() ?: 0) > 0) { "Numeric layout requires metrics" }
    }
    fun array(name: String, max: Int): List<JSONObject> = if (!obj.has(name)) emptyList() else obj.getJSONArray(name).let { values ->
        require(values.length() <= max); (0 until values.length()).map { values.getJSONObject(it) }
    }
    val interactionResult = runCatching { if (obj.has("interaction")) parseTodayInteraction(obj.getJSONObject("interaction")) else null }
    TodayPresentation(
        options = array("options", 4).map { TodayOption(it.requiredText("title", 60), it.optionalText("detail", 120)) },
        steps = array("steps", 12).map {
            val status = it.requiredText("status", 10); require(status in setOf("done", "open"))
            TodayStep(it.requiredText("title", 120), status == "done")
        },
        metrics = array("metrics", 3).map { TodayMetric(it.requiredText("label", 40), it.requiredText("value", 40), it.optionalText("unit", 20)) },
        question = obj.optionalText("question", 80), background = obj.optionalText("background", 8000),
        intent = obj.optionalText("intent", 10).also { require(it in setOf("", "clarify", "decide")) { "Invalid intent" } },
        facts = if (!obj.has("facts")) emptyList() else obj.getJSONArray("facts").let { values ->
            require(values.length() <= 3) { "Too many facts" }
            (0 until values.length()).map { index ->
                val fact = values.get(index); require(fact is String && fact.isNotBlank() && fact.length <= 80) { "Invalid fact" }; fact
            }
        },
        interaction = interactionResult.getOrNull(),
        issue = interactionResult.exceptionOrNull()?.message,
        invalidData = if (interactionResult.isFailure) obj.opt("interaction").toString() else "",
        layout = layout, caption = obj.optionalText("caption", 100),
    )
}.getOrElse { TodayPresentation(issue = it.message ?: "Invalid presentation", invalidData = card.opt("presentation").toString()) }

private fun JSONObject.requiredText(name: String, max: Int, allowEmpty: Boolean = false): String {
    val value = get(name); require(value is String && value.length <= max && (allowEmpty || value.isNotBlank())) { "Invalid $name" }
    return value
}
private fun JSONObject.optionalText(name: String, max: Int): String = if (has(name)) requiredText(name, max, true) else ""

internal fun safeTodayPath(root: String, raw: String): String? {
    if (raw.isBlank() || raw.length > 4096 || raw.any { it.code < 32 } || raw.contains("://") || raw.contains('#')) return null
    if (raw.replace('\\', '/').split('/').any { it == ".." } || raw.startsWith("[") || raw.contains("](")) return null
    if (!isAbsoluteRemotePath(raw) && Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(raw)) return null
    val path = if (isAbsoluteRemotePath(raw)) raw else joinServerPath(root, raw)
    return path.takeIf { isRemotePathWithin(root, it) }
}

data class TodayLibraryEntry(val name: String, val path: String, val isDirectory: Boolean, val count: Int? = null)
data class TodayState(
    val profile: String = "", val root: String = "", val board: TodayBoard? = null, val library: List<TodayLibraryEntry> = emptyList(),
    val loading: Boolean = false, val loaded: Boolean = false, val error: String? = null, val fileExists: Boolean = false,
    val counting: Boolean = false, val countsLoaded: Boolean = false,
    val rawJson: String? = null, val fromCache: Boolean = false, val syncedAt: Long? = null,
    val cacheError: String? = null, val rootVerified: Boolean = false,
    val localSaved: Boolean = false,
    // Server metadata is a hint only: missing stamps and periodic revalidation require a full read.
    val filePath: String = "", val fileSize: Long? = null, val fileModifiedAt: Double? = null, val lastFullReadAt: Long? = null,
)

/** A failed request may use the last good copy only within the same workspace. */
fun retainTodayOnFailure(result: TodayState, previous: TodayState): TodayState =
    if (result.error != null && result.profile == previous.profile && previous.board != null &&
        (result.root.isBlank() || remotePathsEqual(result.root, previous.root))) {
        previous.copy(loading = false, loaded = true, error = result.error, fromCache = true,
            rootVerified = result.rootVerified, library = if (result.rootVerified) result.library else previous.library)
    } else result

/** Background polling must not fan out into dozens of folder-count requests. */
fun retainTodayCounts(result: TodayState, previous: TodayState): TodayState {
    fun shape(state: TodayState) = state.library.map { Triple(it.path, it.name, it.isDirectory) }
    return if (result.profile == previous.profile && result.root == previous.root && previous.countsLoaded && shape(result) == shape(previous))
        result.copy(library = previous.library, countsLoaded = true) else result
}

fun focusCards(cards: List<TodayCard>, showClosed: Boolean = false, domain: TodayDomain? = null): List<TodayCard> =
    cards.filter { (showClosed || !it.isClosed) && (domain == null || it.domain == domain) }
        .sortedWith(compareBy<TodayCard> { it.isClosed }.thenBy { it.attention?.priority ?: 2 })
