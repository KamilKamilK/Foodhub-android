package pl.foodhub.pos.core.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.foodhub.pos.core.common.ApiResult
import pl.foodhub.pos.core.network.api.AuthApi
import pl.foodhub.pos.core.network.apiCall
import pl.foodhub.pos.core.network.model.DeviceDto
import pl.foodhub.pos.core.network.model.PosLoginRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val authApi: AuthApi,
        private val tokenStore: TokenStore,
        private val deviceIdentity: DeviceIdentityProvider,
    ) {
        val sessionState: Flow<SessionState> =
            tokenStore.tokens.map { tokens ->
                if (tokens == null) SessionState.LoggedOut else SessionState.Authenticated
            }

        /**
         * PIN-only login for the terminal (POST /v1/auth/pos-login). The device is
         * trusted through pairing on the backend; [posSerialNo] is only needed the
         * first time a terminal is provisioned into a place with more than one free POS.
         */
        suspend fun loginWithPin(
            pin: String,
            posSerialNo: String? = null,
        ): ApiResult<Unit> {
            val request =
                PosLoginRequestDto(
                    pin = pin,
                    device =
                        DeviceDto(
                            macAddress = deviceIdentity.deviceId(),
                            name = deviceIdentity.model(),
                            model = deviceIdentity.model(),
                            version = deviceIdentity.osVersion(),
                        ),
                    posId = posSerialNo,
                )

            return when (val result = apiCall { authApi.posLogin(request) }) {
                is ApiResult.Success -> {
                    tokenStore.save(result.value.token, result.value.refreshToken)
                    ApiResult.Success(Unit)
                }
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
            }
        }

        fun logout() = tokenStore.clear()
    }
