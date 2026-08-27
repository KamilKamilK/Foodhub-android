plugins {
    alias(libs.plugins.foodhub.android.library)
    alias(libs.plugins.foodhub.android.compose)
}

android {
    namespace = "pl.foodhub.pos.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
