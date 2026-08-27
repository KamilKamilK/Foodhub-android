package pl.foodhub.pos.core.network.di

import javax.inject.Qualifier

/**
 * The bare Retrofit/OkHttp stack used only for the unauthenticated auth endpoints
 * (pos-login, refresh-token). It has neither the bearer interceptor nor the 401
 * refresh authenticator, which breaks the DI cycle (the authenticated client needs
 * the token provider, which needs [pl.foodhub.pos.core.network.api.AuthApi]) and
 * stops a refresh call from recursively triggering another refresh.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthNetwork
