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
    // will only run on the cloud macOS CI set up in Phase 6.
    val xcfName = "Shared"
    val xcf = org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig(project, xcfName)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            xcf.add(this)
            isStatic = true
        }
    }

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
            implementation("io.insert-koin:koin-compose:4.0.0")
            implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            // Backs the Android actual of ProactiveSuggestionScheduler (WorkManager is Android-only;
            // the iOS actual uses BGTaskScheduler instead).
            implementation("androidx.work:work-runtime-ktx:2.8.1")
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

// Something in the dependency graph pulls koin-compose-viewmodel up to 4.2.2 on the iOS
// (Kotlin/Native) configurations despite the explicit 4.0.0 request above -- 4.2.2's iOS klibs
// were built with a newer Kotlin compiler (ABI 2.3.0) than this project's Kotlin 2.0.21 can
// consume. Force every configuration to the version this project can actually use.
configurations.configureEach {
    resolutionStrategy.force(
        "io.insert-koin:koin-compose-viewmodel:4.0.0",
        "io.insert-koin:koin-compose:4.0.0",
        "io.insert-koin:koin-android:4.0.0"
    )
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
