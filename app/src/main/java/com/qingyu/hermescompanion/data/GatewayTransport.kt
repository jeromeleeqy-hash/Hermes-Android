package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.today.todayText
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Contains a category and operation, never a URL, cookie, payload, file path or transcript. */
internal class GatewayTransportException(val operation: String, val category: String, val attempts: Int, val original: IOException, val readOnly: Boolean) :
    IOException(operation + "：" + category + if (readOnly) todayText("，请稍后重试", "; try again") else todayText("；未收到操作确认，请先核对结果", "; confirmation was not received; check the result first"))

internal fun transportCategory(error: Throwable): String = when (error) {
    is SocketTimeoutException -> todayText("服务器响应超时", "Server response timed out")
    is UnknownHostException -> todayText("服务器域名解析失败", "Server name could not be resolved")
    is SSLException -> todayText("安全连接校验失败", "Secure connection failed")
    is java.net.ConnectException -> todayText("无法连接服务器", "Could not connect to server")
    is EOFException, is SocketException -> todayText("连接中途断开", "Connection interrupted")
    else -> todayText("网络请求未完成", "Network request did not complete")
}
internal fun canRetryRead(error: IOException): Boolean = error !is SSLException &&
    (error is SocketTimeoutException || error is SocketException || error is EOFException || error is UnknownHostException ||
        (error is java.net.ProtocolException && error.message.orEmpty().contains("unexpected end", true)))

internal fun transportOperation(path: String): String = when {
    path.startsWith("/api/auth/ws-ticket") -> todayText("建立实时连接", "Open live connection")
    path.startsWith("/api/auth") || path.startsWith("/auth") -> todayText("验证登录", "Check sign-in")
    path.startsWith("/api/sessions") -> todayText("读取或更新对话", "Read or update conversation")
    path.startsWith("/api/files") -> todayText("读取或保存文件", "Read or save file")
    path.startsWith("/api/cron") -> todayText("读取或更新定时任务", "Read or update scheduled tasks")
    path.startsWith("/api/config") -> todayText("读取或更新配置", "Read or update settings")
    path.startsWith("/api/status") -> todayText("检查服务器", "Check server")
    else -> todayText("访问服务器", "Contact server")
}

/** Expire only a probe actually sent while the scheduler was running continuously. */
internal class GatewayHeartbeat(private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    enum class Tick { WAIT, PING, EXPIRED }
    private var active = false
    private var lastInbound = 0L
    private var lastPing = 0L
    private var lastTick = 0L
    private var probeStarted: Long? = null
    private var foreground = true
    fun start() { active = true; lastInbound = clock(); lastPing = lastInbound; lastTick = lastInbound; probeStarted = null }
    fun received() { lastInbound = clock(); probeStarted = null }
    fun setForeground(value: Boolean) {
        foreground = value
        probeStarted = null
        lastTick = clock()
        lastPing = lastTick - 15_000
    }
    fun stop() { active = false }
    fun tick(): Tick {
        if (!active || !foreground) return Tick.WAIT
        val now = clock()
        // Android can suspend a timer. A delayed callback is not evidence of a dead server.
        if (now - lastTick > 30_000 || now < lastTick) probeStarted = null
        lastTick = now
        if (probeStarted?.let { now - it >= 45_000 } == true) return Tick.EXPIRED
        if (now - lastPing < 15_000) return Tick.WAIT
        lastPing = now
        if (probeStarted == null) probeStarted = now
        return Tick.PING
    }
}
