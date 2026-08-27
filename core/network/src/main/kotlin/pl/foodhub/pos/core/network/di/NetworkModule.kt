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
import pl.foodhub.pos.core.network.ApiContractVersionInterceptor
import pl.foodhub.pos.core.network.BuildConfig
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
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L

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
    fun loggingInterceptor(
        @ApplicationContext context: Context,
    ): HttpLoggingInterceptor {
        val debuggable = 0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        return HttpLoggingInterceptor().apply {
            level = if (debuggable) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    @AuthNetwork
    fun authOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(ApiContractVersionInterceptor(BuildConfig.API_CONTRACT_VERSION))
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    fun okHttpClient(
        logging: HttpLoggingInterceptor,
        tokenProvider: AuthTokenProvider,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // TODO(security, ANDROID_POS_ARCHITECTURE.md section 12): add CertificatePinner
            // for the client's API host once deployments exist — the terminal is a
            // dedicated device, so pinning one cert is justified.
            .addInterceptor(ApiContractVersionInterceptor(BuildConfig.API_CONTRACT_VERSION))
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(logging)
            .authenticator(TokenRefreshAuthenticator(tokenProvider))
            .build()

    private fun retrofit(
        baseUrl: String,
        client: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @AuthNetwork
    fun authRetrofit(
        @ApplicationContext context: Context,
        @AuthNetwork client: OkHttpClient,
    ): Retrofit = retrofit(context.getString(R.string.foodhub_api_base_url), client)

    @Provides
    @Singleton
    fun retrofit(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): Retrofit = retrofit(context.getString(R.string.foodhub_api_base_url), client)

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
