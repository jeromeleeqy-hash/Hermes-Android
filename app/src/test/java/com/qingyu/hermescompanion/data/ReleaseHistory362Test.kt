package com.qingyu.hermescompanion.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ReleaseHistory362Test {
    private fun history() = parseReleaseHistory(File("src/main/assets/release-history.json").readText())

    @Test fun historyIncludesEveryArchivedReleaseInBothLanguages() {
        val rows = history()
        val versions = rows.map { it.version.substringBefore('-') }.toSet()
        val archived = File("..").listFiles()!!.mapNotNull { file ->
            Regex("Hermes-v(.+)-(?:release|preview)-notes\\.md").matchEntire(file.name)?.groupValues?.get(1)
        }.map { if (it == "1.0") "1.0.0" else it }
        assertTrue(versions.containsAll(archived))
        assertEquals(rows.size, versions.size)
        assertEquals(com.qingyu.hermescompanion.BuildConfig.VERSION_NAME.substringBefore('-'), rows.first().version)
        assertTrue(rows.all { it.chinese.isNotEmpty() && it.chinese.size == it.english.size })
        assertTrue(rows.all { row -> row.english.none { Regex("[\\u4e00-\\u9fff]").containsMatchIn(it) } })
    }

    @Test fun avatarAndStreamingFixesBelongToTheirOriginalReleases() {
        val rows = history().associateBy { it.version }
        assertTrue(rows.getValue("3.0.3-release").english.any { it.contains("avatar") })
        assertTrue(rows.getValue("3.0.3a-release").english.any { it.contains("clarification") })
        assertTrue(rows.getValue("3.0.3a-release").english.none { it.contains("avatar") })
        assertTrue(rows.getValue("0.5.1").date.isBlank())
    }
}
