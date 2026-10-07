pluginManagement {
    repositories {
        google()
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

rootProject.name = "SolarRobo"
include(":app")
include(":core:contracts")
include(":core:common")
include(":core:database")
include(":core:storage")
include(":feature:notifications")
include(":feature:settings")
include(":feature:simulator")
