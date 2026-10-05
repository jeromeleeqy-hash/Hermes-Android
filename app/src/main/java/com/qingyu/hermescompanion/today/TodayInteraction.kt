package com.qingyu.hermescompanion.today

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

enum class TodayLayout(val key: String, val zh: String, val en: String, val actionZh: String, val actionEn: String) {
    CLARIFICATION("clarification", "澄清确认", "Clarification", "提交确认", "Confirm relationship"),
    COMPARISON("comparison", "方案比较", "Comparison", "采用这个方向", "Choose direction"),
    PRIORITIES("priorities", "重点排序", "Priorities", "提交今日顺序", "Set priorities"),
    TIME_PICKER("time_picker", "时间选择", "Time selection", "准备安排", "Prepare schedule"),
    MEETING_ACTIONS("meeting_actions", "会议分工", "Meeting actions", "建立跟踪待办", "Create follow-ups"),
    CHECKLIST("checklist", "待办清单", "Checklist", "更新进度", "Update progress"),
    MILESTONES("milestones", "阶段验收", "Milestones", "提交阶段验收", "Review milestone"),
    BLOCKER("blocker", "阻塞处理", "Blocker", "补充这项信息", "Provide information"),
    EXECUTION("execution", "执行状态", "Execution", "核对任务状态", "Check run status"),
    DELIVERABLE("deliverable", "成果验收", "Deliverable", "提交审阅意见", "Submit review"),
    METRICS("metrics", "数据趋势", "Metrics", "分析变化原因", "Analyze changes"),
    CHANGE("change", "变化提醒", "Change", "提交处理决定", "Resolve change"),
    TIMELINE("timeline", "事件轨迹", "Timeline", "记录这条进展", "Record update"),
    EVIDENCE("evidence", "证据分析", "Evidence", "继续核验", "Investigate further"),
    REVISION("revision", "文稿修订", "Revision", "采用所选修改", "Apply selected changes"),
    TRIAGE("triage", "资料分拣", "Triage", "提交资料分类", "Classify material"),
    MEMORY_CHANGE("memory_change", "记忆变更", "Memory change", "提交记忆校正", "Correct memory"),
    PERSON_FOLLOWUP("person_followup", "人物跟进", "People", "起草跟进内容", "Draft follow-up"),
    QUICK_LOG("quick_log", "快速记录", "Quick log", "记录这一笔", "Record entry"),
    REFLECTION("reflection", "每日复盘", "Reflection", "保存这次复盘", "Save reflection");
    val label get() = todayText(zh, en)
    val actionLabel get() = todayText(actionZh, actionEn)
}

data class TodayInteractionItem(val id: String, val title: String, val detail: String = "", val value: String = "",
    val status: String = "open", val owner: String = "", val due: String = "")
data class TodayInputField(val id: String, val label: String, val kind: String, val value: String = "",
    val required: Boolean = false, val choices: List<String> = emptyList(), val min: Double = 0.0, val max: Double = 100000.0)
data class TodaySeriesPoint(val label: String, val value: Double)
data class TodayInteraction(val type: TodayLayout, val items: List<TodayInteractionItem>, val fields: List<TodayInputField>,
    val before: String, val after: String, val text: String, val note: String, val timezone: String,
    val series: List<TodaySeriesPoint>, val raw: String)

private val itemIdPattern = Regex("[A-Za-z0-9_-]{1,64}")
private fun JSONObject.text(key: String, max: Int, required: Boolean = false): String {
    if (!has(key)) { require(!required) { "Missing $key" }; return "" }
    val v = get(key)
    require(v is String && v.length <= max && (!required || v.isNotBlank())) { "Invalid $key" }
    return v
}
private fun JSONObject.objects(key: String, max: Int): List<JSONObject> {
    if (!has(key)) return emptyList()
    val values = getJSONArray(key); require(values.length() <= max) { "Too many $key" }
    return (0 until values.length()).map { values.getJSONObject(it) }
}

fun parseTodayInteraction(obj: JSONObject): TodayInteraction {
    require(obj.get("version") == 1) { "Unsupported interaction version" }
    val type = TodayLayout.entries.firstOrNull { it.key == obj.text("type", 32, true) } ?: error("Unsupported card type")
    val ids = mutableSetOf<String>()
    val items = obj.objects("items", 12).map {
        val id = it.text("id", 64, true)
        require(itemIdPattern.matches(id) && ids.add(id)) { "Invalid or duplicate item id" }
        val state = it.text("status", 16).ifBlank { "open" }
        require(state in setOf("open", "done", "waiting", "active")) { "Invalid item status" }
        val due = it.text("due", 10)
        if (due.isNotBlank()) LocalDate.parse(due)
        TodayInteractionItem(id, it.text("title", 60, true), it.text("detail", 200), it.text("value", 120), state, it.text("owner", 60), due)
    }
    val fields = obj.objects("fields", 5).map {
        val id = it.text("id", 64, true)
        require(itemIdPattern.matches(id) && ids.add(id)) { "Invalid or duplicate field id" }
        val kind = it.text("kind", 16, true)
        require(kind in setOf("text", "number", "date", "time", "select")) { "Invalid input kind" }
        val choices = if (it.has("choices")) it.getJSONArray("choices").let { a ->
            require(a.length() in 1..8)
            (0 until a.length()).map { n -> a.get(n).also { v -> require(v is String && v.isNotBlank() && v.length <= 60) } as String }
        } else emptyList()
        require(choices.distinct().size == choices.size) { "Duplicate choices" }
        if (kind == "select") require(choices.isNotEmpty())
        val required = if (it.has("required")) it.get("required").also { v -> require(v is Boolean) } as Boolean else false
        fun bound(key: String, default: Double): Double = if (!it.has(key)) default else (it.get(key) as? Number)?.toDouble()?.also { v -> require(v.isFinite()) } ?: error("Invalid $key")
        val min = bound("min", 0.0); val max = bound("max", 100000.0)
        require(min >= -1e9 && max <= 1e9 && min <= max)
        TodayInputField(id, it.text("label", 60, true), kind, it.text("value", 500), required, choices, min, max).also { field ->
            if (field.value.isNotBlank()) validateTodayField(field, field.value)
        }
    }
    val series = obj.objects("series", 12).map {
        val number = (it.get("value") as? Number)?.toDouble() ?: error("Invalid series value")
        require(number.isFinite() && number in -1e12..1e12)
        TodaySeriesPoint(it.text("label", 24, true), number)
    }
    val timezone = obj.text("timezone", 80)
    if (timezone.isNotEmpty()) require(timezone in ZoneId.getAvailableZoneIds()) { "Use an IANA timezone" }
    val needsItems = type !in setOf(TodayLayout.METRICS, TodayLayout.BLOCKER, TodayLayout.MEMORY_CHANGE, TodayLayout.QUICK_LOG, TodayLayout.REFLECTION)
    if (needsItems) require(items.isNotEmpty()) { "${type.key} requires items" }
    if (type in setOf(TodayLayout.CLARIFICATION, TodayLayout.COMPARISON, TodayLayout.TIME_PICKER)) require(items.size in 2..4)
    if (type == TodayLayout.TIME_PICKER) require(timezone.isNotBlank()) { "Time selection requires timezone" }
    if (type == TodayLayout.QUICK_LOG) require(fields.isNotEmpty())
    val before = obj.text("before", 160); val after = obj.text("after", 160)
    if (type in setOf(TodayLayout.MEMORY_CHANGE, TodayLayout.CHANGE)) require(before.isNotBlank() && after.isNotBlank())
    return TodayInteraction(type, items, fields, before, after, obj.text("text", 800), obj.text("note", 160), timezone, series, obj.toString())
}

fun validateTodayField(field: TodayInputField, value: String) {
    require(value.length <= 500) { todayText("${field.label}内容过长", "${field.label} is too long") }
    require(!field.required || value.isNotBlank()) { todayText("请填写${field.label}", "Enter ${field.label}") }
    if (value.isBlank()) return
    val valid = runCatching {
        when (field.kind) {
            "number" -> require(value.toDouble().let { it.isFinite() && it >= field.min && it <= field.max })
            "date" -> LocalDate.parse(value)
            "time" -> { require(value.matches(Regex("[0-2][0-9]:[0-5][0-9]"))); LocalTime.parse(value) }
            "select" -> require(value in field.choices)
        }
    }.isSuccess
    require(valid) { todayText("请检查${field.label}的格式或范围", "Check the format or range of ${field.label}") }
}

fun todayFingerprint(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
val TodayCard.interactionFingerprint get() = todayFingerprint(contextDocument())

fun initialTodayInput(card: TodayCard): String {
    val i = requireNotNull(card.presentation.interaction)
    return JSONObject().apply {
        put("selected_ids", JSONArray(i.items.filter { (i.type == TodayLayout.CHECKLIST && it.status == "done") || i.type in setOf(TodayLayout.MEETING_ACTIONS, TodayLayout.REVISION) }.map { it.id }))
        put("order", JSONArray(i.items.map { it.id }))
        put("fields", JSONObject().apply { i.fields.forEach { put(it.id, it.value) } })
        put("rows", JSONObject().apply { i.items.forEach { item -> put(item.id, JSONObject().put("owner", item.owner).put("due", item.due).put("classification", item.value.takeIf { it in setOf("archive", "lead", "later") } ?: "later")) } })
        put("selection", ""); put("note", ""); put("operation", ""); put("reviewed", false)
    }.toString()
}

/** Rebuild only known, validated inputs. Neither card data nor controls can supply arbitrary commands. */
fun normalizeTodayInput(card: TodayCard, raw: String): JSONObject {
    require(!card.isClosed) { todayText("事项已结束，请先核对状态", "This item is closed") }
    require(raw.toByteArray().size <= 24000) { "Input too large" }
    val i = requireNotNull(card.presentation.interaction)
    val source = JSONObject(raw)
    val validIds = i.items.map { it.id }.toSet()
    fun idList(key: String): List<String> {
        val a = source.getJSONArray(key); require(a.length() <= 12)
        return (0 until a.length()).map { a.get(it).also { v -> require(v is String && v in validIds) } as String }
            .also { require(it.distinct().size == it.size) }
    }
    val selected = idList("selected_ids")
    val order = idList("order").also { require(it.toSet() == validIds) { "Invalid order" } }
    val selection = source.text("selection", 64)
    if (i.type in setOf(TodayLayout.CLARIFICATION, TodayLayout.COMPARISON, TodayLayout.TIME_PICKER)) require(selection in validIds) {
        todayText("请先选择一项", "Choose an option first")
    }
    if (i.type == TodayLayout.MEETING_ACTIONS) require(selected.isNotEmpty()) { todayText("至少选择一条待办", "Select at least one item") }
    val fields = source.getJSONObject("fields")
    val normalizedFields = JSONObject().apply { i.fields.forEach { f ->
        val value = fields.text(f.id, 500); validateTodayField(f, value); put(f.id, value)
    } }
    val note = source.text("note", 1000).trim()
    if (i.type in setOf(TodayLayout.BLOCKER, TodayLayout.TIMELINE)) require(note.isNotBlank()) { todayText("请先补充具体内容", "Add the specific information first") }
    val operation = source.text("operation", 32)
    val allowed = when (i.type) {
        TodayLayout.DELIVERABLE -> setOf("adopt", "revise")
        TodayLayout.CHANGE -> setOf("accept", "keep")
        TodayLayout.MEMORY_CHANGE -> setOf("correct", "undo")
        else -> setOf("")
    }
    require(operation in allowed) { todayText("请选择处理方式", "Choose an action") }
    if ((i.type == TodayLayout.DELIVERABLE && operation == "revise") || (i.type == TodayLayout.MEMORY_CHANGE && operation == "correct"))
        require(note.isNotBlank()) { todayText("请填写具体修改内容", "Describe the correction") }
    val reviewed = source.get("reviewed").also { require(it is Boolean) } as Boolean
    if (i.type == TodayLayout.MILESTONES) require(reviewed) { todayText("请先核对验收条件", "Review the acceptance criteria first") }
    val rows = source.getJSONObject("rows")
    val normalizedRows = JSONObject().apply { i.items.forEach { item ->
        val row = rows.getJSONObject(item.id)
        val due = row.text("due", 10)
        if (due.isNotBlank()) require(runCatching { LocalDate.parse(due) }.isSuccess) { todayText("日期格式应为 YYYY-MM-DD", "Use YYYY-MM-DD dates") }
        val classification = row.text("classification", 16)
        require(classification in setOf("archive", "lead", "later"))
        put(item.id, JSONObject().put("owner", row.text("owner", 60)).put("due", due).put("classification", classification))
    } }
    return JSONObject().put("selection", selection).put("selected_ids", JSONArray(selected)).put("order", JSONArray(order))
        .put("fields", normalizedFields).put("rows", normalizedRows).put("note", note).put("operation", operation).put("reviewed", reviewed)
}

fun todayInteractionRequest(card: TodayCard, rawInput: String, operationId: String = UUID.randomUUID().toString()): String {
    require(operationId.matches(Regex("[A-Za-z0-9_-]{1,96}")))
    return JSONObject().put("version", 1).put("operation_id", operationId).put("card_id", card.id)
        .put("type", card.presentation.interaction!!.type.key).put("expected_card", card.interactionFingerprint)
        .put("input", normalizeTodayInput(card, rawInput)).toString()
}

fun todayScheduleConfig(morning: String, evening: String, timezone: String): JSONObject {
    listOf(morning, evening).forEach { value ->
        require(value.matches(Regex("[0-2][0-9]:[0-5][0-9]")) && runCatching { LocalTime.parse(value) }.isSuccess) {
            todayText("请用 HH:mm 填写时间，例如 09:00", "Use HH:mm, for example 09:00")
        }
    }
    require(morning != evening) { todayText("早晚时间需要不同", "Choose two different times") }
    require(timezone in ZoneId.getAvailableZoneIds()) { todayText("请填写地区时区，例如 Asia/Shanghai", "Enter an IANA timezone, such as Asia/Shanghai") }
    return JSONObject().put("version", 1).put("morning", morning).put("evening", evening).put("timezone", timezone)
}
