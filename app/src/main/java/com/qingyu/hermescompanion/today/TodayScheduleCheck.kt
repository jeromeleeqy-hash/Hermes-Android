package com.qingyu.hermescompanion.today

import com.qingyu.hermescompanion.model.CronJob
import org.json.JSONObject

data class TodayScheduleCheck(
    val profile: String = "", val root: String = "", val loading: Boolean = false,
    val message: String = "", val jobs: List<String> = emptyList(), val verified: Boolean = false,
    val connectionScope: String = "",
    val morning: String = "", val evening: String = "", val timezone: String = "",
)

/** Read actual jobs by stored ID; a successful Agent reply alone is never proof of installation. */
fun verifyTodaySchedule(raw: String, jobs: List<CronJob>, profile: String, root: String): TodayScheduleCheck {
    val settings = JSONObject(raw)
    require(settings.getString("profile") == profile && settings.getString("workspace") == root) {
        todayText("配置属于其他工作区，请重新配置", "Settings belong to another workspace; configure again")
    }
    val marker = "hermes-app-today:" + todayFingerprint(profile + "\n" + root).take(20)
    require(settings.getString("schedule_marker") == marker && settings.getString("contract_version") in setOf("3.7.7", "3.7.8", "3.8.0", "3.8.1", "3.8.3", "3.8.4")) {
        todayText("整理规则尚未升级，请重新配置", "Briefing rules need upgrading; configure again")
    }
    todayScheduleConfig(settings.getString("morning"), settings.getString("evening"), settings.getString("timezone"))
    val migrated = settings.optInt("storage_version", 1) >= 2
    if (migrated) require(settings.optString("overview_path") in setOf(TodayBoard.PATH, com.qingyu.hermescompanion.data.joinServerPath(root, TodayBoard.PATH))) {
        todayText("迁移后的概览路径不匹配，请核对配置", "Migrated overview path does not match; check settings")
    }
    val ids = settings.getJSONObject("cron_ids")
    require(ids.getString("morning").isNotBlank() && ids.getString("morning") != ids.getString("evening"))
    var valid = true
    val lines = listOf("morning", "evening").map { period ->
        val id = ids.getString(period)
        val label = if (period == "morning") todayText("早间", "Morning") else todayText("晚间", "Evening")
        val job = jobs.singleOrNull { it.id == id }
        val reason = when {
            job == null -> todayText("没有找到任务", "Job not found")
            !job.prompt.contains(marker) || !job.prompt.contains(root) || !job.prompt.contains(".hermes-app/today/") || !job.prompt.contains(period) ->
                todayText("任务规则与当前工作区不匹配", "Job rules do not match this workspace")
            migrated && !job.prompt.contains(com.qingyu.hermescompanion.data.joinServerPath(root, TodayBoard.PATH)) ->
                todayText("任务仍未引用迁移后的概览位置", "Job does not reference the migrated overview")
            !job.enabled -> todayText("任务已暂停", "Job is paused")
            job.nextRunAt.isBlank() -> todayText("服务器未返回下次运行时间", "Server returned no next run time")
            else -> ""
        }
        if (reason.isNotBlank()) valid = false
        "$label · $id\n" + if (reason.isNotBlank()) reason else todayText("下次运行：${job!!.nextRunAt}", "Next run: ${job!!.nextRunAt}")
    }
    return TodayScheduleCheck(profile, root, message = if (valid)
        todayText("已从服务器核对两项任务，均已启用。下次运行时间以服务器返回为准。", "Both server jobs are enabled. Next run times below are returned by the server.")
        else todayText("配置尚未完整生效，请查看下面的核对结果。", "Setup is not fully active. Review the results below."), jobs = lines, verified = valid,
        morning = settings.getString("morning"), evening = settings.getString("evening"), timezone = settings.getString("timezone"))
}
