package com.qingyu.hermescompanion.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qingyu.hermescompanion.MainActivity
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.data.HermesApiClient
import com.qingyu.hermescompanion.model.NotificationPreferences
import com.qingyu.hermescompanion.storage.SecureConfigStore
import com.qingyu.hermescompanion.storage.SecureCookieJar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object HermesNotifications {
    const val ACTION_POLL_CRON = "com.qingyu.hermescompanion.POLL_CRON"
    const val EXTRA_PROFILE = "hermes_profile"
    const val EXTRA_SESSION_ID = "hermes_session_id"
    const val EXTRA_ROUTE = "hermes_route"
    private const val MESSAGE_CHANNEL = "hermes_messages"
    private const val TASK_CHANNEL = "hermes_tasks"
    private val nextId = AtomicInteger(2000)

    fun applyPreferences(context: Context, preferences: NotificationPreferences) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(MESSAGE_CHANNEL)
        manager.deleteNotificationChannel(TASK_CHANNEL)
        if (!preferences.enabled) return
        manager.createNotificationChannel(channel(MESSAGE_CHANNEL, "Hermes 消息", "AI 回复与对话结果", preferences))
        manager.createNotificationChannel(channel(TASK_CHANNEL, "Hermes 任务", "定时任务完成、失败与异常", preferences))
    }

    fun showMessage(
        context: Context,
        title: String,
        body: String,
        profile: String? = null,
        sessionId: String? = null,
        route: String = "sessions",
    ) {
        val preferences = SecureConfigStore(context).readNotificationPreferences()
        if (!preferences.enabled || !preferences.messageAlerts || !canNotify(context)) return
        ensureChannels(context, preferences)
        notify(context, MESSAGE_CHANNEL, title, body, preferences, profile, sessionId, route)
    }

    fun showTask(
        context: Context,
        title: String,
        body: String,
        profile: String? = null,
        route: String = "tasks",
    ) {
        val preferences = SecureConfigStore(context).readNotificationPreferences()
        if (!preferences.enabled || !preferences.taskAlerts || !canNotify(context)) return
        ensureChannels(context, preferences)
        notify(context, TASK_CHANNEL, title, body, preferences, profile, null, route)
    }

    fun showAgentRequest(
        context: Context,
        title: String,
        body: String,
        profile: String? = null,
        sessionId: String? = null,
    ) {
        val preferences = SecureConfigStore(context).readNotificationPreferences()
        if (!preferences.enabled || !preferences.taskAlerts || !canNotify(context)) return
        ensureChannels(context, preferences)
        notify(
            context,
            TASK_CHANNEL,
            title,
            body,
            preferences,
            profile,
            sessionId,
            route = "tasks",
            includeRequestActions = true,
        )
    }

    fun scheduleCronPolling(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pollingIntent(context)
        alarmManager.cancel(pending)
        if (!enabled) return
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 2 * 60_000L,
            15 * 60_000L,
            pending,
        )
    }

    private fun channel(
        id: String,
        name: String,
        description: String,
        preferences: NotificationPreferences,
    ): NotificationChannel {
        return NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
            this.description = description
            setShowBadge(preferences.badge)
            enableVibration(preferences.vibration)
            if (preferences.sound) {
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
            } else {
                setSound(null, null)
            }
        }
    }

    private fun ensureChannels(context: Context, preferences: NotificationPreferences) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(MESSAGE_CHANNEL) == null || manager.getNotificationChannel(TASK_CHANNEL) == null) {
            applyPreferences(context, preferences)
        }
    }

    private fun notify(
        context: Context,
        channel: String,
        title: String,
        body: String,
        preferences: NotificationPreferences,
        profile: String?,
        sessionId: String?,
        route: String,
        includeRequestActions: Boolean = false,
    ) {
        if (!canNotify(context)) return
        val notificationId = nextId.incrementAndGet()
        val deepLink = Uri.Builder()
            .scheme("hermes-companion")
            .authority("open")
            .appendPath(route)
            .apply {
                profile?.takeIf(String::isNotBlank)?.let { appendQueryParameter("profile", it) }
                sessionId?.takeIf(String::isNotBlank)?.let { appendQueryParameter("session", it) }
            }
            .build()
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java)
                .setData(deepLink)
                .putExtra(EXTRA_PROFILE, profile)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_hermes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setNumber(if (preferences.badge) 1 else 0)
            .setVibrate(if (preferences.vibration) longArrayOf(0, 180, 90, 180) else longArrayOf(0))
        if (includeRequestActions) {
            builder.addAction(R.drawable.ic_stat_hermes, "去处理", openApp)
            if (!sessionId.isNullOrBlank()) {
                val chatLink = deepLink.buildUpon().path("chat").build()
                val openChat = PendingIntent.getActivity(
                    context,
                    notificationId + 100_000,
                    Intent(context, MainActivity::class.java)
                        .setData(chatLink)
                        .putExtra(EXTRA_PROFILE, profile)
                        .putExtra(EXTRA_SESSION_ID, sessionId)
                        .putExtra(EXTRA_ROUTE, "chat")
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(R.drawable.ic_stat_hermes, "打开会话", openChat)
            }
        }
        val notification = builder.build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked between the check above and notify().
        }
    }

    private fun pollingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        501,
        Intent(context, CronNotificationReceiver::class.java).setAction(ACTION_POLL_CRON),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canNotify(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

class CronNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val store = SecureConfigStore(appContext)
        val preferences = store.readNotificationPreferences()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            HermesNotifications.scheduleCronPolling(appContext, preferences.enabled && preferences.taskAlerts)
        }
        if (!preferences.enabled || !preferences.taskAlerts) return
        val pending = goAsync()
        Executors.newSingleThreadExecutor().execute {
            try {
                val config = store.read() ?: return@execute
                val cookies = SecureCookieJar(store)
                if (!cookies.hasCookies()) return@execute
                val client = HermesApiClient(config, cookies)
                try {
                    val profile = store.readActiveHermesProfile()
                    client.setProfile(profile)
                    val jobs = client.listCronJobs()
                    val previous = store.readCronSnapshot()
                    val current = jobs.associate { job -> job.id to "${job.lastRunAt}|${job.lastStatus}" }
                    if (previous.isNotEmpty()) {
                        jobs.forEach { job ->
                            val snapshot = current[job.id].orEmpty()
                            if (snapshot != previous[job.id] && job.lastRunAt.isNotBlank()) {
                                val status = job.lastStatus.ifBlank { job.state }
                                HermesNotifications.showTask(
                                    appContext,
                                    if (status.contains("fail", true) || status.contains("error", true)) "定时任务执行失败" else "定时任务已完成",
                                    "${job.name} · ${status.ifBlank { "已更新" }}",
                                    profile = profile,
                                )
                            }
                        }
                    }
                    store.saveCronSnapshot(current)
                } finally {
                    client.close()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
