package com.qingyu.hermescompanion.ui.format

import com.qingyu.hermescompanion.model.HermesProject

/** Prefer the most specific project; Windows paths compare case-insensitively. */
internal fun projectForWorkspace(projects: List<HermesProject>, rawPath: String): HermesProject? {
    if (rawPath.isBlank()) return null
    return projects.mapNotNull { project ->
        val depth = (project.paths + project.primaryPath).filter(String::isNotBlank)
            .filter { com.qingyu.hermescompanion.data.isRemotePathWithin(it, rawPath) }
            .maxOfOrNull { com.qingyu.hermescompanion.data.remotePathKey(it).length }
        depth?.let { project to it }
    }.maxByOrNull { it.second }?.first
}
