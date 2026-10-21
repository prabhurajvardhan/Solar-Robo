import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm:1.8.1")
    }
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = "17"
        }
    }
}
