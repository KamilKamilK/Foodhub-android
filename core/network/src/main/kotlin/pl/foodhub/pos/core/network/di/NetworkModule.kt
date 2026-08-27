package pl.foodhub.pos.core.network.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.foodhub.pos.core.common.DefaultDispatcherProvider
import pl.foodhub.pos.core.common.DispatcherProvider
import pl.foodhub.pos.core.network.R
import pl.foodhub.pos.core.network.api.AuthApi
import pl.foodhub.pos.core.network.api.MenuApi
import pl.foodhub.pos.core.network.api.SalesApi
import pl.foodhub.pos.core.network.api.TablesApi
import pl.foodhub.pos.core.network.auth.AuthInterceptor
import pl.foodhub.pos.core.network.auth.AuthTokenProvider
import pl.foodhub.pos.core.network.auth.TokenRefreshAuthenticator
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Provides
    @Singleton
    fun dispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun okHttpClient(
        @ApplicationContext context: Context,
        tokenProvider: AuthTokenProvider,
    ): OkHttpClient {
        val debuggable = 0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // TODO(security, ANDROID_POS_ARCHITECTURE.md section 12): add CertificatePinner
            // for the client's API host once deployments exist — the terminal is a
            // dedicated device, so pinning one cert is justified.
            .addInterceptor(AuthInterceptor(tokenProvider))
            .apply {
                if (debuggable) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                    )
                }
            }
            .authenticator(TokenRefreshAuthenticator(tokenProvider))
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.foodhub_api_base_url))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

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
