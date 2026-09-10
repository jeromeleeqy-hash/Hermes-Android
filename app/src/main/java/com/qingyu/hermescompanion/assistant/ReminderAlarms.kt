package com.qingyu.hermescompanion.assistant

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.qingyu.hermescompanion.model.CronJob
import com.qingyu.hermescompanion.notification.HermesNotifications
import com.qingyu.hermescompanion.storage.SecureConfigStore
import org.json.JSONObject

/** A local notification mirror of a confirmed native Hermes cron, never a task execution engine. */
object ReminderAlarms {
    private const val STORE = "assistant-reminders"
    private fun key(scope: AssistantScope, record: AssistantRecord) = "${scope.key}:${record.entity}"
    private fun pending(context: Context, key: String): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, AssistantReminderReceiver::class.java).setAction("assistant.remind").setData(Uri.parse("hermes-reminder://local/$key")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun exactAvailable(context: Context): Boolean = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun schedule(context: Context, scope: AssistantScope, record: AssistantRecord) {
        if (!record.confirmed || record.cronId.isBlank() || record.archived || record.dueAt <= System.currentTimeMillis()) return
        val key = key(scope, record)
        val data = JSONObject().put("server", scope.server.trimEnd('/')).put("account", scope.account).put("profile", scope.profile)
            .put("session", record.sourceSession).put("title", record.title).put("due", record.dueAt).put("cron", record.cronId)
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putString(key, data.toString()).apply()
        scheduleAt(context, key, record.dueAt)
    }
    private fun scheduleAt(context: Context, key: String, due: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context, key)
        manager.cancel(intent)
        try {
            if (exactAvailable(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent)
        } catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, intent) }
    }
    fun cancel(context: Context, scope: AssistantScope, record: AssistantRecord) {
        val key = key(scope, record)
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, key))
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().remove(key).apply()
    }
    fun reconcile(context: Context, scope: AssistantScope, records: List<AssistantRecord>, jobs: List<CronJob>?) {
        if (jobs == null) return // A failed fetch cannot mean all reminders were deleted.
        val active = records.filter { it.kind == RecordKind.FOLLOWUP && it.confirmed && !it.archived && it.cronId.isNotBlank() }
        val allowed = active.filter { record -> jobs.any { it.id == record.cronId && it.enabled } }
        val keep = allowed.map { key(scope, it) }.toSet()
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        prefs.all.keys.filter { it.startsWith(scope.key + ":") && it !in keep }.forEach {
            context.getSystemService(AlarmManager::class.java).cancel(pending(context, it)); prefs.edit().remove(it).apply()
        }
        allowed.forEach { schedule(context, scope, it) }
    }
    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, raw) ->
            runCatching {
                val due = JSONObject(raw as String).getLong("due")
                if (due > System.currentTimeMillis()) scheduleAt(context, key, due)
                else if (System.currentTimeMillis() - due < 24 * 60 * 60_000L) scheduleAt(context, key, System.currentTimeMillis() + 3_000)
                else prefs.edit().remove(key).apply()
            }
        }
    }
    /** Retire only obsolete 3.4.0 local mirrors after a successful native job refresh. */
    fun reconcileExisting(context: Context, server: String, account: String, profile: String, jobs: List<CronJob>) {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, raw) ->
            val data = runCatching { JSONObject(raw as String) }.getOrNull() ?: return@forEach
            if (data.optString("server") != server.trimEnd('/') || data.optString("account") != account || data.optString("profile") != profile) return@forEach
            if (jobs.none { it.id == data.optString("cron") && it.enabled }) {
                context.getSystemService(AlarmManager::class.java).cancel(pending(context, key))
                prefs.edit().remove(key).apply()
            }
        }
    }
    fun fire(context: Context, key: String) {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return
        val data = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val config = SecureConfigStore(context).read() ?: return
        // A notification from an old account must never open a coincidentally matching session on a new server.
        if (config.baseUrl.trimEnd('/') != data.optString("server") || config.username != data.optString("account")) return
        prefs.edit().remove(key).apply()
        HermesNotifications.showMessage(context, uiText(R.string.ui_0048, "约好的跟进时间到了"), data.optString("title"),
            data.optString("profile"), data.optString("session").takeIf { it.isNotBlank() }, if (data.optString("session").isBlank()) "tasks" else "chat")
    }
}
class AssistantReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) ReminderAlarms.restore(context)
        else intent.data?.lastPathSegment?.let { ReminderAlarms.fire(context, it) }
    }
}
