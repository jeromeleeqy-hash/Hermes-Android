package com.qingyu.hermescompanion.i18n

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qingyu.hermescompanion.storage.SecureConfigStore
import java.util.Locale

/** Stable preference values; server content is never passed through this resource layer. */
enum class AppLanguageMode(val tag: String) {
    SYSTEM(""), CHINESE("zh-CN"), ENGLISH("en");

    companion object {
        fun fromTag(tag: String): AppLanguageMode = when {
            tag.startsWith("zh", true) -> CHINESE
            tag.startsWith("en", true) -> ENGLISH
            else -> SYSTEM
        }
    }
}

object AppLanguage {
    // An immutable snapshot makes background notifications and Compose use the same locale.
    private data class Snapshot(val mode: AppLanguageMode, val locale: Locale, val resources: Resources?)
    private var snapshot by mutableStateOf(Snapshot(AppLanguageMode.SYSTEM, Locale.SIMPLIFIED_CHINESE, null))
    val mode: AppLanguageMode get() = snapshot.mode
    val locale: Locale get() = snapshot.locale

    fun savedMode(context: Context): AppLanguageMode {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            return if (locales == null || locales.isEmpty) AppLanguageMode.SYSTEM
            else AppLanguageMode.fromTag(locales[0].toLanguageTag())
        }
        return SecureConfigStore(context).readLanguageMode()
            ?.let { runCatching { AppLanguageMode.valueOf(it) }.getOrNull() } ?: AppLanguageMode.SYSTEM
    }

    private fun resolve(context: Context, mode: AppLanguageMode): Locale {
        if (mode != AppLanguageMode.SYSTEM) return Locale.forLanguageTag(mode.tag)
        val systemLocales = if (Build.VERSION.SDK_INT >= 33)
            context.getSystemService(LocaleManager::class.java)?.systemLocales
        else Resources.getSystem().configuration.locales
        val system = systemLocales?.takeUnless { it.isEmpty }?.get(0) ?: Locale.ENGLISH
        return if (system.language == "zh") Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
    }

    fun localizedContext(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        val locale = resolve(context, savedMode(context))
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun refresh(context: Context) {
        val mode = savedMode(context)
        val locale = resolve(context, mode)
        val config = Configuration(context.resources.configuration).apply { setLocales(LocaleList(locale)) }
        snapshot = Snapshot(mode, locale, context.createConfigurationContext(config).resources)
    }

    fun setMode(context: Context, mode: AppLanguageMode) {
        SecureConfigStore(context).saveLanguageMode(mode.name)
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(mode.tag)
        }
        refresh(context)
    }

    internal fun text(@StringRes id: Int, fallback: String, args: Array<out Any?>): String {
        val current = snapshot
        val template = current.resources?.getString(id) ?: fallback
        return if (args.isEmpty()) template else String.format(current.locale, template, *args)
    }
}

/** Resource access for both UI and asynchronous work. Only app-owned literals use this function. */
fun uiText(@StringRes id: Int, fallback: String, vararg args: Any?): String = AppLanguage.text(id, fallback, args)
