import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Adds Jetpack Compose to a module that already has the android.application or
 * android.library convention applied. The concrete extension is fetched via
 * `withPlugin` because the Android DSL extension is registered under its concrete
 * type, not `CommonExtension`, so a generic `getByType<CommonExtension<*, …>>()`
 * fails to resolve it.
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
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
