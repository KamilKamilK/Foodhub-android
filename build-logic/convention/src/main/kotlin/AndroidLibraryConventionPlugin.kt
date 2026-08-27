import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
            apply("org.jlleitschuh.gradle.ktlint")
            apply("io.gitlab.arturbosch.detekt")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // targetSdk for a library only matters for its instrumented tests; the
            // AAR itself has none. Left at the AGP default until androidTest exists.
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit4").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("mockk").get())
        }
    }
}
