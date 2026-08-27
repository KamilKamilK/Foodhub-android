plugins {
    alias(libs.plugins.foodhub.android.library)
    alias(libs.plugins.foodhub.android.compose)
    alias(libs.plugins.foodhub.android.hilt)
}

android {
    namespace = "pl.foodhub.pos.feature.sales"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.database)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
}
