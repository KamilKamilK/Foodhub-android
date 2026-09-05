package pl.foodhub.pos.testing.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import pl.foodhub.pos.core.common.DefaultDispatcherProvider
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.network.api.AuthApi
import pl.foodhub.pos.core.network.api.MenuApi
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.auth.AuthInterceptor
import pl.foodhub.pos.core.network.auth.AuthTokenProvider
import pl.foodhub.pos.core.network.auth.TokenRefreshAuthenticator
import pl.foodhub.pos.core.network.di.AuthNetwork
import pl.foodhub.pos.core.network.di.NetworkModule
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

/**
 * Replaces core:network's [NetworkModule] for instrumented tests: same Retrofit/OkHttp
 * wiring (bearer interceptor, 401 refresh), but both API base URLs point at an in-process
 * [MockWebServer] instead of a real `foodhub-api` deployment, so tests never depend on one
 * being reachable. Hilt owns the [MockWebServer]'s lifecycle like any other singleton --
 * a test starts it (and enqueues responses) before launching the activity, and shuts it
 * down afterwards.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object TestNetworkModule {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Provides
    @Singleton
    fun mockWebServer(): MockWebServer = MockWebServer()

    @Provides
    @Singleton
    fun dispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun okHttpClient(tokenProvider: AuthTokenProvider): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .authenticator(TokenRefreshAuthenticator(tokenProvider))
            .build()

    @Provides
    @Singleton
    @AuthNetwork
    fun authOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    private fun retrofit(
        baseUrl: String,
        client: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    // MockWebServer.url() resolves the socket's canonical hostname, which Android's
    // StrictMode treats as network I/O and refuses on the main thread -- ViewModel
    // creation (and therefore first use of these singletons) always happens there.
    // The loopback address plus the already-bound port is equivalent for a
    // same-process fake server and needs no such lookup.
    private fun MockWebServer.loopbackUrl(): String = "http://127.0.0.1:$port/"

    @Provides
    @Singleton
    fun retrofit(
        mockWebServer: MockWebServer,
        client: OkHttpClient,
    ): Retrofit = retrofit(mockWebServer.loopbackUrl(), client)

    @Provides
    @Singleton
    @AuthNetwork
    fun authRetrofit(
        mockWebServer: MockWebServer,
        @AuthNetwork client: OkHttpClient,
    ): Retrofit = retrofit(mockWebServer.loopbackUrl(), client)

    @Provides
    @Singleton
    fun authApi(
        @AuthNetwork retrofit: Retrofit,
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun menuApi(retrofit: Retrofit): MenuApi = retrofit.create(MenuApi::class.java)

    @Provides
    @Singleton
    fun salesApi(retrofit: Retrofit): SalesApi = retrofit.create(SalesApi::class.java)

    @Provides
    @Singleton
    fun tablesApi(retrofit: Retrofit): TablesApi = retrofit.create(TablesApi::class.java)
}
