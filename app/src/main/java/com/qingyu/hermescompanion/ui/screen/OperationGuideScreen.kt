package com.qingyu.hermescompanion.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.AppLanguage
import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.ui.component.*
import org.json.JSONArray
import org.json.JSONObject

internal data class GuideArticle(val id: String, val title: String, val steps: List<String>, val tip: String)
internal data class GuideCategory(val id: String, val title: String, val hint: String, val articles: List<GuideArticle>)
internal fun parseOperationGuide(json: String, language: String): List<GuideCategory> {
    val lang = if (language == "zh") "zh" else "en"
    fun JSONObject.localized(key: String) = getJSONObject(key).getString(lang)
    fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    return JSONObject(json).getJSONArray("categories").objects().map { category ->
        GuideCategory(category.getString("id"), category.localized("title"), category.localized("hint"),
            category.getJSONArray("articles").objects().map { article ->
                val steps = article.getJSONObject("steps").getJSONArray(lang)
                GuideArticle(article.getString("id"), article.localized("title"),
                    (0 until steps.length()).map { steps.getString(it) }, article.localized("tip"))
            })
    }
}

@Composable
fun OperationGuideScreen(contentPadding: PaddingValues, onBack: () -> Unit, onReplayIntro: () -> Unit = {}) {
    val context = LocalContext.current
    val locale = AppLanguage.locale.language
    val guide = remember(context, locale) { parseOperationGuide(context.assets.open("operation-guide.json").bufferedReader().use { it.readText() }, locale) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var articleId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val category = guide.find { it.id == categoryId }
    val article = guide.flatMap { it.articles }.find { it.id == articleId }
    val goBack = { if (articleId != null) articleId = null else if (categoryId != null) categoryId = null else onBack() }
    BackHandler(onBack = goBack)
    Column(Modifier.fillMaxSize().statusBarsPadding().testTag("operation_guide")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = goBack, modifier = Modifier.testTag("guide_back")) {
                HermesMulticolorIcon(HermesIconKind.BACK, uiText(R.string.ui_0554, "返回"))
            }
            Text(if (article != null) category?.title ?: uiText(R.string.ui_0865, "使用说明") else category?.title ?: uiText(R.string.ui_0865, "使用说明"),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        key(categoryId, articleId) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("guide_list"), contentPadding = PaddingValues(start = 22.dp, end = 22.dp,
                top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (article != null) {
                    item { Text(article.title, Modifier.testTag("guide_article_title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
                    items(article.steps.size) { index ->
                        SetupSection(uiText(R.string.guide_step, "第 %1\$s 步", index + 1)) {
                            Text(article.steps[index], style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (article.tip.isNotBlank()) item {
                        Text(article.tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (category != null) {
                    item { Text(category.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(category.articles, key = { it.id }) { task -> GuideEntry(task.title, null, "guide_task_${task.id}") { articleId = task.id } }
                } else {
                    item { Text(uiText(R.string.guide_question, "你想完成什么？"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
                    item { HermesOutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("guide_search"),
                        placeholder = { Text(uiText(R.string.guide_search, "搜索操作，例如：语音、文件")) }, singleLine = true,
                        leadingIcon = { HermesMulticolorIcon(HermesIconKind.SEARCH, null, iconSize = 20.dp) }) }
                    if (query.isBlank()) {
                        items(guide, key = { it.id }) { group -> GuideEntry(group.title, group.hint, "guide_category_${group.id}") { categoryId = group.id } }
                        item { TextButton(onClick = onReplayIntro, modifier = Modifier.fillMaxWidth().testTag("guide_replay_intro")) {
                            Text(uiText(R.string.intro_replay, "重看开场动画"))
                        } }
                    } else {
                        val matches = guide.flatMap { group -> group.articles.filter { task ->
                            (listOf(task.title, task.tip) + task.steps).any { it.contains(query.trim(), ignoreCase = true) }
                        }.map { group to it } }
                        if (matches.isEmpty()) item { Text(uiText(R.string.guide_no_results, "没有找到相关操作，试试更短的关键词。"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(matches, key = { it.second.id }) { (group, task) -> GuideEntry(task.title, group.title, "guide_task_${task.id}") { categoryId = group.id; articleId = task.id } }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideEntry(title: String, subtitle: String?, tag: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick)) {
        SetupSection(title, trailing = { HermesMulticolorIcon(HermesIconKind.CHEVRON_RIGHT, null, iconSize = 18.dp) }) {
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
