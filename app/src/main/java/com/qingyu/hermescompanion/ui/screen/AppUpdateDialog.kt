package com.qingyu.hermescompanion.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.BuildConfig
import com.qingyu.hermescompanion.i18n.AppLanguage
import com.qingyu.hermescompanion.today.todayText
import com.qingyu.hermescompanion.ui.component.HermesAlertDialog
import com.qingyu.hermescompanion.update.*
import java.util.Locale

@Composable
fun AppUpdateDialog(state: AppUpdateState, viewModel: AppUpdateViewModel) {
    val context = LocalContext.current
    var explainPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!context.packageManager.canRequestPackageInstalls()) viewModel.permissionError()
    }
    AppUpdateContent(state, viewModel::close, { viewModel.check() }, { viewModel.download() }, { viewModel.cancel() },
        onInstall = {
            if (!context.packageManager.canRequestPackageInstalls()) explainPermission = true
            else viewModel.install { context.startActivity(it) }
        }, viewModel::automatic, viewModel::wifiOnly)
    if (explainPermission) HermesAlertDialog(
        onDismissRequest = { explainPermission = false },
        title = { Text(todayText("允许 Hermes 安装更新", "Allow Hermes updates")) },
        text = { Text(todayText("在系统设置中打开“允许来自此来源的应用”，返回后点“安装更新”。每次安装仍由你确认。",
            "Enable “Allow from this source” in system settings, then return and tap Install update. You confirm each installation.")) },
        confirmButton = { TextButton(onClick = {
            explainPermission = false
            try { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))) }
            catch (_: Exception) { viewModel.permissionError() }
        }) { Text(todayText("去设置", "Open settings")) } },
        dismissButton = { TextButton(onClick = { explainPermission = false }) { Text(todayText("稍后", "Later")) } })
}

@Composable
internal fun AppUpdateContent(
    state: AppUpdateState, onClose: () -> Unit, onCheck: () -> Unit, onDownload: () -> Unit,
    onCancel: () -> Unit, onInstall: () -> Unit, onAutomatic: (Boolean) -> Unit, onWifiOnly: (Boolean) -> Unit,
) {
    val release = state.release
    HermesAlertDialog(onDismissRequest = onClose,
        title = { Text(todayText("软件更新", "App updates")) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())
                .testTag("app-update-content"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(todayText("当前版本 ${BuildConfig.VERSION_NAME}", "Installed ${BuildConfig.VERSION_NAME}"), style = MaterialTheme.typography.bodySmall)
                    Text(when (state.phase) {
                        AppUpdatePhase.CHECKING -> todayText("正在检查新版…", "Checking for updates…")
                        AppUpdatePhase.CURRENT -> todayText("已是最新版本", "You're up to date")
                        AppUpdatePhase.VERIFYING -> todayText("正在校验安装包…", "Verifying the download…")
                        AppUpdatePhase.READY -> todayText("下载完成，可以安装", "Ready to install")
                        AppUpdatePhase.PAUSED -> todayText("等待网络，稍后会继续下载", "Waiting for a connection to resume")
                        AppUpdatePhase.DOWNLOADING -> todayText("正在下载更新", "Downloading update")
                        else -> if (state.hasUpdate) todayText("发现新版本 ${release?.versionName}", "Version ${release?.versionName} available")
                            else todayText("保持 Hermes 常用常新", "Keep Hermes up to date")
                    }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                if (release != null && state.hasUpdate) {
                    Text("${release.versionName} · ${updateMegabytes(release.sizeBytes)} MB", style = MaterialTheme.typography.labelLarge)
                    val notes = if (AppLanguage.locale.language == "zh" || release.notesEn.isEmpty()) release.notes else release.notesEn
                    notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    if (!state.compatible) Text(todayText("此版本需要更新的 Android 系统，暂时无法安装。", "This release needs a newer Android system and cannot be installed on this device yet."), color = MaterialTheme.colorScheme.error)
                }
                if (state.phase in setOf(AppUpdatePhase.DOWNLOADING, AppUpdatePhase.PAUSED) && release != null) {
                    LinearProgressIndicator(progress = { (state.downloadedBytes.toFloat() / release.sizeBytes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("${updateMegabytes(state.downloadedBytes)} / ${updateMegabytes(release.sizeBytes)} MB", style = MaterialTheme.typography.bodySmall)
                    Text(todayText("可以关闭这个窗口，下载会继续。", "You can close this window; the download will continue."), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onCancel) { Text(todayText("取消下载", "Cancel download")) }
                }
                if (state.phase in setOf(AppUpdatePhase.CHECKING, AppUpdatePhase.VERIFYING)) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(updateErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                if (state.phase == AppUpdatePhase.FAILED && state.hasUpdate) {
                    TextButton(onClick = onCheck) { Text(todayText("重新检查版本", "Refresh release information")) }
                }
                if (state.phase == AppUpdatePhase.READY) Text(todayText("安装包已通过校验，接下来由系统确认安装。", "The package passed verification. Android will ask you to confirm installation."), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(todayText("自动检查新版", "Check automatically"), style = MaterialTheme.typography.bodyMedium)
                        Text(todayText("每天打开 App 时检查，不自动下载", "Checks once a day when opened; never downloads automatically"), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(state.automatic, onCheckedChange = onAutomatic, modifier = Modifier.testTag("update-auto"))
                }
                if (state.hasUpdate && !state.inProgress && state.phase != AppUpdatePhase.READY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(todayText("仅通过 Wi-Fi 下载", "Download on Wi-Fi only"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(state.wifiOnly, onCheckedChange = onWifiOnly)
                    }
                    if (!state.wifiOnly) Text(todayText("下载可能使用移动流量。", "Downloading may use mobile data."), style = MaterialTheme.typography.bodySmall)
                }
                Text("download.leaier.com", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            when {
                state.phase == AppUpdatePhase.READY -> TextButton(onClick = onInstall) { Text(todayText("安装更新", "Install update")) }
                state.hasUpdate && !state.inProgress && state.phase != AppUpdatePhase.CHECKING -> {
                    TextButton(onClick = onDownload, enabled = state.compatible) { Text(todayText("下载更新", "Download update")) }
                }
                !state.inProgress -> TextButton(onClick = onCheck, enabled = state.phase != AppUpdatePhase.CHECKING) { Text(todayText("检查更新", "Check for updates")) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(todayText("关闭", "Close")) } })
}

private fun updateMegabytes(bytes: Long) = String.format(Locale.ROOT, "%.1f", bytes / 1048576.0)
internal fun updateErrorText(reason: UpdateError): String = when (reason) {
    UpdateError.NOT_PUBLISHED -> todayText("更新文件尚未发布，请稍后再试。", "The update file has not been published yet. Try again later.")
    UpdateError.FORBIDDEN -> todayText("更新源拒绝访问，请联系发布者检查下载权限。", "Access to the update was denied. Ask the publisher to check download permissions.")
    UpdateError.INVALID_MANIFEST -> todayText("版本信息格式有误，暂时无法更新。", "The release information is invalid.")
    UpdateError.WRONG_PACKAGE -> todayText("安装包不适用于当前 App，已阻止安装。", "This package is for a different app. Installation blocked.")
    UpdateError.INTEGRITY -> todayText("安装包校验失败，请重新下载。", "Package verification failed. Download it again.")
    UpdateError.SIGNATURE -> todayText("安装包签名与当前 App 不一致，已阻止安装。", "The signing identity does not match this app. Installation blocked.")
    UpdateError.STORAGE -> todayText("存储空间不足或不可用，请清理后重试。", "Storage is unavailable or full. Free up space and try again.")
    UpdateError.INCOMPATIBLE -> todayText("此安装包不适用于当前系统或版本。", "This update is incompatible with the installed app or Android version.")
    UpdateError.MISSING_FILE -> todayText("下载文件已被清理，请重新下载。", "The download was removed. Download it again.")
    UpdateError.INSTALLER -> todayText("无法打开系统安装界面，请稍后再试。", "Could not open the system installer. Try again later.")
    UpdateError.PERMISSION -> todayText("尚未获得安装权限，请点“安装更新”重试。", "Installation permission is not enabled. Tap Install update to retry.")
    UpdateError.DOWNLOAD -> todayText("下载未完成，请检查网络后重新下载。", "The download did not complete. Check your connection and retry.")
    UpdateError.SERVER -> todayText("更新源暂时不可用，请稍后重试。", "The update service is unavailable. Try again later.")
    UpdateError.NETWORK -> todayText("无法连接更新源，请检查网络后重试。", "Cannot reach the update service. Check your connection and retry.")
}
