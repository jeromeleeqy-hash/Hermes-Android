package com.qingyu.hermescompanion.data

import java.util.Locale

private val driveRoot = Regex("^[A-Za-z]:[\\\\/]")
private fun unquotedPath(raw: String): String = raw.trim().let {
    if (it.length >= 2 && it.first() == '"' && it.last() == '"') it.substring(1, it.length - 1) else it
}

/** These are paths on the server, never paths on the Android phone. */
internal fun isAbsoluteRemotePath(raw: String): Boolean {
    val path = unquotedPath(raw)
    if (path.isEmpty() || path.any { it.code < 32 } || path.startsWith("\\\\.\\")) return false
    if (path.startsWith("\\\\?\\")) {
        val tail = path.substring(4)
        return driveRoot.containsMatchIn(tail) || (tail.startsWith("UNC\\", true) && validUnc(tail.substring(4)))
    }
    return driveRoot.containsMatchIn(path) || when {
        path.startsWith("\\\\") -> validUnc(path.substring(2))
        path.startsWith('/') -> true
        else -> false
    }
}

private fun validUnc(tail: String): Boolean {
    val parts = tail.replace('\\', '/').split('/').filter(String::isNotEmpty)
    return parts.size >= 2 && parts.take(2).none { it in setOf(".", "..", "?") || ':' in it }
}

internal fun isWindowsRemotePath(path: String): Boolean =
    driveRoot.containsMatchIn(path) || path.startsWith("\\\\") || (path.startsWith("//") && validUnc(path.substring(2)))

internal fun normalizeWorkspacePath(raw: String): String {
    val path = unquotedPath(raw)
    val windows = isWindowsRemotePath(path)
    val trimmed = if (windows) path.trimEnd('/', '\\') else path.trimEnd('/')
    return when {
        path.startsWith('/') && trimmed.isEmpty() -> "/"
        Regex("^[A-Za-z]:$").matches(trimmed) && driveRoot.containsMatchIn(path) -> trimmed + path[2]
        Regex("^\\\\\\\\\\?\\\\[A-Za-z]:$").matches(trimmed) -> "$trimmed\\"
        else -> trimmed
    }
}

internal fun remotePathKey(raw: String): String {
    var path = normalizeWorkspacePath(raw)
    if (path.startsWith("\\\\?\\UNC\\", true)) path = "\\\\" + path.substring(8)
    else if (path.startsWith("\\\\?\\")) path = path.substring(4)
    if (isWindowsRemotePath(path)) return path.replace('\\', '/').trimEnd('/').lowercase(Locale.ROOT)
    return path
}

internal fun remotePathsEqual(a: String, b: String): Boolean = remotePathKey(a) == remotePathKey(b)

internal fun isRemotePathWithin(root: String, target: String): Boolean {
    if (!isAbsoluteRemotePath(root) || !isAbsoluteRemotePath(target)) return false
    val value = if (isWindowsRemotePath(target)) target.replace('\\', '/') else target
    if (value.split('/').any { it == ".." }) return false
    val base = remotePathKey(root)
    val path = remotePathKey(target)
    return if (base == "/") path.startsWith('/') && !isWindowsRemotePath(target)
        else path == base || path.startsWith("$base/")
}

internal fun joinServerPath(root: String, child: String): String {
    val separator = if (isWindowsRemotePath(root) && '\\' in root) '\\' else '/'
    val tail = if (isWindowsRemotePath(root)) child.replace('/', separator).replace('\\', separator) else child
    return root.trimEnd('/', '\\') + separator + tail.trimStart('/', '\\')
}

internal fun remoteParentPath(raw: String): String {
    val path = normalizeWorkspacePath(raw)
    val key = if (isWindowsRemotePath(path)) path.replace('\\', '/') else path
    val index = key.lastIndexOf('/')
    if (index < 0) return ""
    return normalizeWorkspacePath(path.substring(0, index + if (index == 0 || index == 2 && driveRoot.containsMatchIn(path)) 1 else 0))
}
