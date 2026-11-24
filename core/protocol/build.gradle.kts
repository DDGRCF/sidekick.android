plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.wire)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

wire {
    sourcePath {
        srcDir("../../proto")
    }
    kotlin {
        android = false
    }
}

dependencies {
    api(libs.wire.runtime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
