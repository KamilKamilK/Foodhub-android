package pl.foodhub.pos.core.realtime.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealtimeModule {
    private const val CONNECT_TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    @RealtimeNetwork
    fun realtimeOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // The Mercure stream is held open indefinitely; a fixed read timeout would
            // tear it down as soon as a quiet period exceeded it.
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
}
