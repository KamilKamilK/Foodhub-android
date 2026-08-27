import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Adds Jetpack Compose to a module that already has the android.application or
 * android.library convention applied. Each branch fetches the extension by the
 * concrete type AGP actually registers (`getByType<CommonExtension<*, …>>()` does
 * not resolve to it), then shares one config through a `CommonExtension` parameter.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            configureCompose(extensions.getByType<ApplicationExtension>())
        }
        pluginManager.withPlugin("com.android.library") {
            configureCompose(extensions.getByType<LibraryExtension>())
        }
    }

    private fun Project.configureCompose(commonExtension: CommonExtension<*, *, *, *, *, *>) {
        commonExtension.buildFeatures.compose = true

        dependencies {
            val bom = platform(libs.findLibrary("androidx-compose-bom").get())
            add("implementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
