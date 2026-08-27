plugins {
    alias(libs.plugins.foodhub.android.application)
    alias(libs.plugins.foodhub.android.compose)
    alias(libs.plugins.foodhub.android.hilt)
}

android {
    namespace = "pl.foodhub.pos"

    defaultConfig {
        applicationId = "pl.foodhub.pos"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.auth)
    implementation(projects.core.database)
    implementation(projects.feature.auth)
    implementation(projects.feature.menu)
    implementation(projects.feature.sales)
    implementation(projects.feature.tables)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
