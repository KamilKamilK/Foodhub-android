package pl.foodhub.pos.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.foodhub.pos.core.common.ApiResult
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs a Retrofit suspend call and folds the outcome into [ApiResult]: a completed
 * round trip with a non-2xx status becomes [ApiResult.HttpError] (with the API's
 * `code`/`message` parsed from the body when present), a failed round trip becomes
 * [ApiResult.NetworkError].
 */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        ApiResult.HttpError(
            status = e.code(),
            errorCode = body?.let { readField(it, "code") },
            message = body?.let { readField(it, "message") },
        )
    } catch (e: IOException) {
        ApiResult.NetworkError(e)
    }

suspend fun <T> apiCall(response: Response<T>): ApiResult<T> =
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) ApiResult.Success(body) else ApiResult.HttpError(response.code(), null, "Empty body")
    } else {
        val raw = response.errorBody()?.string()
        ApiResult.HttpError(response.code(), raw?.let { readField(it, "code") }, raw?.let { readField(it, "message") })
    }

private fun readField(
    rawBody: String,
    field: String,
): String? =
    runCatching { errorJson.parseToJsonElement(rawBody).jsonObject[field]?.jsonPrimitive?.content }.getOrNull()
