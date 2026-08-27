plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// foodhub-api HTTP contract version this build targets (gradle.properties),
// surfaced as BuildConfig.API_CONTRACT_VERSION and sent as X-Api-Contract-Version.
val apiContractVersion: String = providers.gradleProperty("foodhub.apiContractVersion").get().trim()

android {
    namespace = "pl.foodhub.pos.core.network"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        buildConfigField("int", "API_CONTRACT_VERSION", apiContractVersion)
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.okhttp.mockwebserver)
}
