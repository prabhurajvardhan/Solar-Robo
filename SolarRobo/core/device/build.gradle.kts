plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.solarrobo.core.device"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":core:contracts"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
