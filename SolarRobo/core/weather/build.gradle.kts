plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.solarrobo.core.weather"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:contracts"))
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    testImplementation(libs.junit)
}
