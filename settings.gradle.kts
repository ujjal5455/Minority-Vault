pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MinorityVault"

include(
    ":app",
    ":engine",
    ":protocol",
    ":net",
    ":ai",
    ":data",
    ":designsystem",
    ":audio",
    ":feature:lobby",
    ":feature:round",
    ":feature:results",
    ":feature:history",
    ":feature:settings",
    ":feature:tutorial",
)
