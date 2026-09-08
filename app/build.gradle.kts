import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val stableDebugKeystore = file(providers.gradleProperty("hermesSigningFile").orNull ?: rootProject.file("signing/hermes-debug.keystore"))
val hermesPreview = providers.gradleProperty("hermesPreview").map(String::toBoolean).getOrElse(false)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.qingyu.hermescompanion"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.qingyu.hermescompanion"
        minSdk = 26
        targetSdk = 36
        versionCode = 320
        versionName = "3.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        manifestPlaceholders["hermesAppLabel"] = if (hermesPreview) "Hermes 预览" else "Hermes"
    }

    signingConfigs {
        if (stableDebugKeystore.exists()) {
            create("stableDebug") {
                storeFile = stableDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = if (hermesPreview) ".preview" else ".debug"
            versionNameSuffix = if (hermesPreview) "-preview" else "-debug"
            signingConfigs.findByName("stableDebug")?.let { signingConfig = it }
            // Keep the installable build in one DEX for compatibility with OEM runtimes.
            // Resource shrinking stays disabled while the new icon system is being verified.
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("stableDebug")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.16")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Start Mockito's instrumentation with the test JVM; works in CI where self-attach is unavailable.
val mockitoAgent by configurations.creating

dependencies {
    mockitoAgent("org.mockito:mockito-core:5.14.2") { isTransitive = false }
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    System.getenv("HERMES_LOCAL_MAVEN")?.let { systemProperty("robolectric.dependency.repo.url", "$it/maven") }

    doFirst { jvmArgs("-javaagent:${mockitoAgent.asPath}") }
}
