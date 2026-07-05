plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets can only be *built* on a macOS host (Xcode toolchain required by Kotlin/Native).
    // Declaring them here is safe on any OS; actual iosX64/iosArm64/iosSimulatorArm64 compilation
    // will only run on the cloud macOS CI set up in a later phase.
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("androidx.room:room-runtime:2.8.4")
            implementation("androidx.sqlite:sqlite-bundled:2.5.0")

            // Compose Multiplatform — core UI toolkit, shared across Android + iOS
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)

            // KMP-compatible ViewModel (androidx.lifecycle.ViewModel doesn't exist outside Android)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.4")

            // KMP-portable key-value storage backing SessionManager (SharedPreferences on Android
            // has no Kotlin/Native equivalent; this wraps SharedPreferences/NSUserDefaults per platform)
            implementation("com.russhwolf:multiplatform-settings:1.1.1")

            // KMP Navigation Compose (androidx.navigation:navigation-compose is Android-only)
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")

            // Koin's Compose Multiplatform integration (koin-androidx-compose is Android-only)
            implementation("io.insert-koin:koin-compose:4.2.2")
            implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.shoppilist.shared.resources"
}

android {
    namespace = "com.shoppilist.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
    // These KSP tasks won't actually run on this machine (iOS Kotlin/Native targets are disabled
    // outside macOS), but are declared now so Phase 6's cloud CI doesn't need Gradle-file changes.
    add("kspIosX64", "androidx.room:room-compiler:2.8.4")
    add("kspIosArm64", "androidx.room:room-compiler:2.8.4")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.8.4")
}
