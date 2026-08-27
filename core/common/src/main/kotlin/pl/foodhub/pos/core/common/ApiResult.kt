package pl.foodhub.pos.core.common

/**
 * Outcome of a single call to `foodhub-api`. Keeps the transport failure modes the
 * terminal must react to differently (offline vs. rejected vs. server fault) apart,
 * instead of collapsing them into one exception type.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** The request reached the server and came back with a 4xx/5xx status. */
    data class HttpError(
        val status: Int,
        val errorCode: String?,
        val message: String?,
    ) : ApiResult<Nothing>

    /** The request never completed a round trip (no connectivity, timeout, TLS). */
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(value))
        is ApiResult.HttpError -> this
        is ApiResult.NetworkError -> this
    }

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> =
    also { if (it is ApiResult.Success) block(it.value) }
