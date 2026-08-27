import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Android defaults shared by the application and every library module: SDK levels,
 * Java 17, Kotlin compiler options, and a JUnit test task. Feature/core modules only
 * differ in whether they add Compose or Hilt on top.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = libs.findVersion("androidCompileSdk").get().toString().toInt()

        defaultConfig {
            minSdk = libs.findVersion("androidMinSdk").get().toString().toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    tasks.withType(KotlinCompile::class.java).configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(
                providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false),
            )
        }
    }

    tasks.withType(Test::class.java).configureEach {
        useJUnit()
    }
}
