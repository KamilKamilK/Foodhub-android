package pl.foodhub.pos

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that boots Hilt's [HiltTestApplication] instead of the
 * production `FoodHubPosApplication`, so `@HiltAndroidTest` classes can install
 * test-only modules (`@TestInstallIn`) such as `core:network`'s replacement in
 * `pl.foodhub.pos.testing.di.TestNetworkModule`. `FoodHubPosApplication`'s own
 * `Configuration.Provider` setup for WorkManager does not run under this Application, so
 * tests that exercise `core:sync` initialize WorkManager themselves with an injected
 * `HiltWorkerFactory` instead (see `TableToCartFlowTest`).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
