package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.model.HermesProject

/** Prefer the most specific project when workspace roots are nested. */
internal fun projectForWorkspace(projects: List<HermesProject>, rawPath: String): HermesProject? {
    if (rawPath.isBlank()) return null
    val path = rawPath.trim().replace('\\', '/').trimEnd('/').ifEmpty { "/" }
    return projects.mapNotNull { project ->
        val depth = (project.paths + project.primaryPath).filter(String::isNotBlank).map {
            it.trim().replace('\\', '/').trimEnd('/').ifEmpty { "/" }
        }.filter { root -> root == "/" || path == root || path.startsWith("$root/") }
            .maxOfOrNull(String::length)
        depth?.let { project to it }
    }.maxByOrNull { it.second }?.first
}
