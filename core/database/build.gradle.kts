plugins {
    alias(libs.plugins.foodhub.android.library)
    alias(libs.plugins.foodhub.android.hilt)
}

android {
    namespace = "pl.foodhub.pos.core.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(projects.core.common)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
}
