package pl.foodhub.pos.core.network.api

import pl.foodhub.pos.core.network.model.AuthTokensDto
import pl.foodhub.pos.core.network.model.PosLoginRequestDto
import pl.foodhub.pos.core.network.model.RefreshTokenRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/pos-login")
    suspend fun posLogin(
        @Body body: PosLoginRequestDto,
    ): AuthTokensDto

    @POST("v1/auth/refresh-token")
    suspend fun refresh(
        @Body body: RefreshTokenRequestDto,
    ): AuthTokensDto
}
