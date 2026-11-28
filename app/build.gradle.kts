import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseSigningPropsFile = listOfNotNull(
    System.getenv("SIDEKICK_SIGNING_PROPS")?.takeIf { it.isNotBlank() }?.let(::File),
    rootProject.file("keystore.properties"),
    File(System.getProperty("user.home"), ".config/sidekick-android/signing.properties"),
).firstOrNull { it.isFile }

val releaseSigningProps = Properties().apply {
    releaseSigningPropsFile?.inputStream()?.use(::load)
}

val releaseStoreFilePath = System.getenv("SIDEKICK_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    ?: releaseSigningProps.getProperty("storeFile")
val releaseStorePassword = System.getenv("SIDEKICK_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: releaseSigningProps.getProperty("storePassword")
val releaseKeyAlias = System.getenv("SIDEKICK_KEY_ALIAS")?.takeIf { it.isNotBlank() }
    ?: releaseSigningProps.getProperty("keyAlias")
val releaseKeyPassword = System.getenv("SIDEKICK_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: releaseSigningProps.getProperty("keyPassword")

val releaseKeystoreFile = releaseStoreFilePath
    ?.takeIf { it.isNotBlank() }
    ?.let { path -> File(path).let { if (it.isAbsolute) it else rootProject.file(path) } }

val hasReleaseSigning = releaseKeystoreFile?.isFile == true &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "dev.ddgrcf.sidekick"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.ddgrcf.sidekick"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    androidResources {
        localeFilters += listOf("zh", "en")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.version",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "org/bouncycastle/pqc/**",
                "META-INF/versions/9/OSGI-INF/**",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:protocol"))
    implementation(project(":core:noise"))
    implementation(project(":core:ansi"))
    implementation(project(":core:store"))
    implementation(project(":core:relay"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.browser)
    implementation(libs.work.runtime.ktx)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
}
