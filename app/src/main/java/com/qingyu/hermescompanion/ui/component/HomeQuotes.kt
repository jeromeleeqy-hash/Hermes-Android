package com.qingyu.hermescompanion.ui.component

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.AppLanguage
import java.time.LocalDate
import kotlin.random.Random

/** One short line per home visit/day; time ticks and theme changes never shuffle it. */
@Composable
internal fun rememberHomeQuote(day: LocalDate): String {
    val context = LocalContext.current
    val locale = AppLanguage.locale
    val quotes = remember(context, locale) {
        context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
            .resources.getStringArray(R.array.home_quotes)
    }
    val index = rememberSaveable(day.toEpochDay()) { HomeQuoteSelection.next(quotes.size) }
    return quotes[index.mod(quotes.size)]
}

internal object HomeQuoteSelection {
    private var previous = -1
    @Synchronized fun next(count: Int): Int {
        require(count > 0)
        val choice = if (count == 1) 0 else if (previous in 0 until count) {
            Random.nextInt(count - 1).let { if (it >= previous) it + 1 else it }
        } else Random.nextInt(count)
        previous = choice
        return choice
    }
}
