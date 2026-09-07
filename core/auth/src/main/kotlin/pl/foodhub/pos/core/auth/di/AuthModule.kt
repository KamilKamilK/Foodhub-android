package pl.foodhub.pos.core.auth.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.foodhub.pos.core.auth.AndroidDeviceBuildInfo
import pl.foodhub.pos.core.auth.AuthTokenProviderImpl
import pl.foodhub.pos.core.auth.DeviceBuildInfo
import pl.foodhub.pos.core.auth.DeviceIdentityProvider
import pl.foodhub.pos.core.auth.DeviceIdentityProviderSelector
import pl.foodhub.pos.core.auth.GenericDeviceIdentityProvider
import pl.foodhub.pos.core.auth.PaxDeviceIdentityProvider
import pl.foodhub.pos.core.auth.SunmiDeviceIdentityProvider
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
    fun bindDeviceBuildInfo(impl: AndroidDeviceBuildInfo): DeviceBuildInfo

    companion object {
        @Provides
        @Singleton
        fun provideDeviceIdentityProvider(
            deviceBuildInfo: DeviceBuildInfo,
            generic: GenericDeviceIdentityProvider,
            sunmi: SunmiDeviceIdentityProvider,
            pax: PaxDeviceIdentityProvider,
        ): DeviceIdentityProvider = DeviceIdentityProviderSelector.select(deviceBuildInfo, generic, sunmi, pax)
    }
}
