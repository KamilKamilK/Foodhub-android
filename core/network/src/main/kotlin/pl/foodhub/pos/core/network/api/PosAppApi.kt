package pl.foodhub.pos.core.network.api

import okhttp3.ResponseBody
import pl.foodhub.pos.core.network.model.PosAppVersionDto
import retrofit2.http.GET
import retrofit2.http.Streaming

interface PosAppApi {
    @GET("v1/pos-app/version")
    suspend fun version(): PosAppVersionDto

    /**
     * [Streaming] keeps Retrofit from buffering the whole APK into memory before
     * handing back the response -- the caller reads [ResponseBody.byteStream] and
     * writes it straight to disk.
     */
    @Streaming
    @GET("v1/pos-app/apk")
    suspend fun apk(): ResponseBody
}
