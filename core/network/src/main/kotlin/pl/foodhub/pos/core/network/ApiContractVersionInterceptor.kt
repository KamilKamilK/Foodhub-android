package pl.foodhub.pos.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Declares the foodhub-api HTTP contract version this build targets on every
 * request, via the `X-Api-Contract-Version` header. The API hard-blocks builds
 * below its minimum supported contract (406) and flags merely-behind ones with
 * `X-Client-Outdated`.
 *
 * The value comes from `foodhub.apiContractVersion` in `gradle.properties` and
 * is bumped only when the app adopts a breaking API contract — it is unrelated
 * to the app's `versionName`.
 */
class ApiContractVersionInterceptor(
    private val contractVersion: Int,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request()
                .newBuilder()
                .header("X-Api-Contract-Version", contractVersion.toString())
                .build(),
        )
}
