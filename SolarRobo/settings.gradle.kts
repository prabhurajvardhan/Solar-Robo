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

// Core Modules
include(":core:contracts")
include(":core:model")
include(":core:ui")
include(":core:common")
include(":core:device")
include(":core:ai")
include(":core:camera")
include(":core:database")
include(":core:storage")
include(":core:network")
include(":core:simulator")

// Feature Modules
include(":feature:onboarding")
include(":feature:home")
include(":feature:energy")
include(":feature:control")
include(":feature:talk")
include(":feature:safety")
include(":feature:camera")
include(":feature:environment")
include(":feature:activity")
include(":feature:health")
include(":feature:analytics")
include(":feature:notifications")
include(":feature:settings")
include(":feature:simulator")
