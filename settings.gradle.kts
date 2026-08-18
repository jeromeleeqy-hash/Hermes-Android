pluginManagement {
    val hermesLocalMaven = System.getenv("HERMES_LOCAL_MAVEN")?.trim()?.trimEnd('/')
    repositories {
        if (!hermesLocalMaven.isNullOrBlank()) {
            maven("$hermesLocalMaven/maven") { isAllowInsecureProtocol = true }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val hermesLocalMaven = System.getenv("HERMES_LOCAL_MAVEN")?.trim()?.trimEnd('/')
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (!hermesLocalMaven.isNullOrBlank()) {
            maven("$hermesLocalMaven/maven") { isAllowInsecureProtocol = true }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "HermesCompanion"
include(":app")
