package com.qingyu.hermescompanion.data

import org.json.JSONArray

data class ReleaseHistoryItem(val version: String, val date: String, val chinese: List<String>, val english: List<String>)

internal fun parseReleaseHistory(source: String): List<ReleaseHistoryItem> {
    val rows = JSONArray(source)
    return List(rows.length()) { index ->
        val row = rows.getJSONObject(index)
        fun strings(key: String): List<String> = row.getJSONArray(key).let { values ->
            List(values.length()) { values.getString(it) }
        }
        ReleaseHistoryItem(row.getString("version"), row.optString("date"), strings("zh"), strings("en"))
    }
}
