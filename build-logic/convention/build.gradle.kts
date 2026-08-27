plugins {
    `kotlin-dsl`
}

group = "pl.foodhub.pos.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

// build-logic has no version catalog on purpose: an included build that generates
// org.gradle.accessors.dm.LibrariesForLibs shadows the root build's one on the
// module buildscript classpath, breaking libs.* in every module that applies a
// convention plugin. These few coordinates are compile-only; keep the versions in
// sync with ../gradle/libs.versions.toml.
dependencies {
    compileOnly("com.android.tools.build:gradle:8.7.3")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.1.0")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.0-1.0.29")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.54")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.2")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "foodhub.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "foodhub.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "foodhub.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "foodhub.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "foodhub.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
