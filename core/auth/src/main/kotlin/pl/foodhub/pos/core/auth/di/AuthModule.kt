package pl.foodhub.pos.core.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.foodhub.pos.core.auth.AndroidDeviceIdentityProvider
import pl.foodhub.pos.core.auth.AuthTokenProviderImpl
import pl.foodhub.pos.core.auth.DeviceIdentityProvider
import pl.foodhub.pos.core.network.auth.AuthTokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    @Singleton
    fun bindAuthTokenProvider(impl: AuthTokenProviderImpl): AuthTokenProvider

    @Binds
    @Singleton
    fun bindDeviceIdentityProvider(impl: AndroidDeviceIdentityProvider): DeviceIdentityProvider
}
