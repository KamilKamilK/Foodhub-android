package pl.foodhub.pos.core.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.AuthApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.auth.AuthTokenProvider
import pl.foodhub.pos.core.network.model.RefreshTokenRequestDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges core:auth's token store to the network layer's [AuthTokenProvider] contract.
 * Serialises refreshes with a mutex so a burst of parallel 401s triggers one exchange.
 */
@Singleton
class AuthTokenProviderImpl
    @Inject
    constructor(
        private val tokenStore: TokenStore,
        private val authApi: AuthApi,
    ) : AuthTokenProvider {
        private val refreshMutex = Mutex()

        override suspend fun accessToken(): String? = tokenStore.tokens.value?.accessToken

        override suspend fun refresh(): Boolean =
            refreshMutex.withLock {
                val refreshToken = tokenStore.tokens.value?.refreshToken ?: return false
                when (val result = apiCall { authApi.refresh(RefreshTokenRequestDto(refreshToken)) }) {
                    is ApiResult.Success -> {
                        tokenStore.save(result.value.token, result.value.refreshToken ?: refreshToken)
                        true
                    }
                    else -> false
                }
            }

        override suspend fun onAuthenticationLost() = tokenStore.clear()
    }
