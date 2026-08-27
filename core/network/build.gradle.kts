plugins {
    alias(libs.plugins.foodhub.android.library)
    alias(libs.plugins.foodhub.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.foodhub.pos.core.network"
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.okhttp.mockwebserver)
}
