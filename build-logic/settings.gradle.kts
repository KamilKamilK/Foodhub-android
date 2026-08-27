dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // `libs` is auto-created from build-logic/gradle/libs.versions.toml (kept separate
    // from the root catalog on purpose — see that file's header).
}

rootProject.name = "build-logic"
include(":convention")
