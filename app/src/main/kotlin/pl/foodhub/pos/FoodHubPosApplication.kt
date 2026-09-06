package pl.foodhub.pos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import pl.foodhub.pos.core.realtime.RealtimeSessionController
import javax.inject.Inject

@HiltAndroidApp
class FoodHubPosApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var realtimeSessionController: RealtimeSessionController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        realtimeSessionController.start()
    }
}
