plugins {
    alias(libs.plugins.foodhub.android.library)
    alias(libs.plugins.foodhub.android.hilt)
}

android {
    namespace = "pl.foodhub.pos.core.auth"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
}
