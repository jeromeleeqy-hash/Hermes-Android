package com.qingyu.hermescompanion.update

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingyu.hermescompanion.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

enum class AppUpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, PAUSED, VERIFYING, READY, FAILED }
data class AppUpdateState(
    val visible: Boolean = false,
    val phase: AppUpdatePhase = AppUpdatePhase.IDLE,
    val release: AppRelease? = null,
    val downloadedBytes: Long = 0,
    val automatic: Boolean = true,
    val wifiOnly: Boolean = true,
    val error: UpdateError? = null,
    val pauseReason: Int = 0,
) {
    val hasUpdate get() = release?.versionCode?.let { it > BuildConfig.VERSION_CODE } == true
    val inProgress get() = phase in setOf(AppUpdatePhase.DOWNLOADING, AppUpdatePhase.PAUSED, AppUpdatePhase.VERIFYING)
    val compatible get() = release?.minSdk?.let { it <= Build.VERSION.SDK_INT } != false
}

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppUpdateRepository(application)
    private val mutex = Mutex()
    private var pending = repo.pending()
    private val mutable = MutableStateFlow(AppUpdateState(
        release = pending?.release ?: repo.cachedRelease(), automatic = repo.automatic, wifiOnly = repo.wifiOnly,
        phase = if (pending != null) AppUpdatePhase.DOWNLOADING else AppUpdatePhase.IDLE))
    val state = mutable.asStateFlow()

    private fun operation(block: suspend () -> Unit) = viewModelScope.launch {
        if (!mutex.tryLock()) return@launch
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutable.update { it.copy(phase = AppUpdatePhase.FAILED,
                error = (e as? UpdateFailure)?.reason ?: UpdateError.NETWORK) }
        } finally { mutex.unlock() }
    }
    fun open() {
        mutable.update { it.copy(visible = true) }
        if (pending == null) check() else poll()
    }
    fun close() { mutable.update { it.copy(visible = false) } }
    fun automatic(value: Boolean) { repo.automatic = value; mutable.update { it.copy(automatic = value) } }
    fun wifiOnly(value: Boolean) { repo.wifiOnly = value; mutable.update { it.copy(wifiOnly = value) } }
    fun onForeground() {
        if (pending != null) poll()
        else if (repo.shouldCheck(System.currentTimeMillis())) check()
    }
    fun check() = operation {
        if (pending != null) {
            if (mutable.value.phase != AppUpdatePhase.FAILED) return@operation
            withContext(Dispatchers.IO) { repo.clearDownload() }
            pending = null
        }
        repo.markAttempt(System.currentTimeMillis())
        mutable.update { it.copy(phase = AppUpdatePhase.CHECKING, error = null) }
        val release = withContext(Dispatchers.IO) { repo.fetch() }
        mutable.update { it.copy(release = release,
            phase = if (release.versionCode > BuildConfig.VERSION_CODE) AppUpdatePhase.AVAILABLE else AppUpdatePhase.CURRENT) }
    }
    fun download() = operation {
        val release = mutable.value.release ?: return@operation
        if (mutable.value.inProgress) return@operation
        val active = withContext(Dispatchers.IO) { repo.start(release) }
        pending = active
        mutable.update { it.copy(phase = AppUpdatePhase.DOWNLOADING, downloadedBytes = 0, error = null, pauseReason = 0) }
    }
    fun cancel() = operation {
        withContext(Dispatchers.IO) { repo.clearDownload() }
        pending = null
        mutable.update { it.copy(phase = if (it.hasUpdate) AppUpdatePhase.AVAILABLE else AppUpdatePhase.IDLE,
            downloadedBytes = 0, error = null) }
    }
    fun poll() = operation {
        val active = pending ?: return@operation
        if (active.release.versionCode <= BuildConfig.VERSION_CODE) {
            withContext(Dispatchers.IO) { repo.clearDownload() }
            pending = null
            mutable.update { it.copy(release = null, phase = AppUpdatePhase.CURRENT, error = null) }
            return@operation
        }
        if (mutable.value.phase == AppUpdatePhase.READY || mutable.value.phase == AppUpdatePhase.FAILED) return@operation
        val progress = withContext(Dispatchers.IO) { repo.progress(active) }
        mutable.update { it.copy(downloadedBytes = progress.bytes.coerceAtLeast(0), pauseReason = progress.reason) }
        when (progress.status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                mutable.update { it.copy(phase = AppUpdatePhase.VERIFYING) }
                withContext(Dispatchers.IO) { repo.verify(active.release) }
                mutable.update { it.copy(phase = AppUpdatePhase.READY, error = null) }
            }
            DownloadManager.STATUS_FAILED -> throw UpdateFailure(when (progress.reason) {
                401, 403 -> UpdateError.FORBIDDEN
                404 -> UpdateError.NOT_PUBLISHED
                DownloadManager.ERROR_INSUFFICIENT_SPACE, DownloadManager.ERROR_DEVICE_NOT_FOUND -> UpdateError.STORAGE
                else -> UpdateError.DOWNLOAD
            })
            DownloadManager.STATUS_PAUSED -> mutable.update { it.copy(phase = AppUpdatePhase.PAUSED) }
            else -> mutable.update { it.copy(phase = AppUpdatePhase.DOWNLOADING) }
        }
    }
    fun install(launch: (Intent) -> Unit) = operation {
        val active = pending ?: return@operation
        if (mutable.value.phase != AppUpdatePhase.READY) return@operation
        mutable.update { it.copy(phase = AppUpdatePhase.VERIFYING, error = null) }
        val intent = withContext(Dispatchers.IO) { repo.installerIntent(active.release) }
        mutable.update { it.copy(phase = AppUpdatePhase.READY) }
        try { launch(intent) }
        catch (e: Exception) { mutable.update { it.copy(error = UpdateError.INSTALLER) } }
    }
    fun permissionError() { mutable.update { it.copy(error = UpdateError.PERMISSION) } }
}
