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

// Feature Modules (Implemented)
include(":feature:onboarding")
include(":feature:camera")
include(":feature:simulator")
include(":feature:notifications")
include(":feature:settings")
include(":feature:home")
include(":feature:energy")

// Feature Modules (Planned - Unbuilt)
// include(":feature:control")
// include(":feature:safety")
// include(":feature:environment")
// include(":feature:activity")
// include(":feature:health")
// include(":feature:analytics")
