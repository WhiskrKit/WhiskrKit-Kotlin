package eu.whiskrkit.core.network

import java.io.IOException

/** Internal SDK errors. They never escape the SDK to the host app. */
internal sealed class WhiskrKitException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class NotInitialized :
        WhiskrKitException("WhiskrKit is not initialized with an API key. Call initialize() first.")

    class BadRequest : WhiskrKitException("Bad request (400)")
    class Unauthorized : WhiskrKitException("Unauthorized (401)")
    class Forbidden : WhiskrKitException("Forbidden (403)")
    class NotFound : WhiskrKitException("Not found (404)")
    class RateLimited : WhiskrKitException("Rate limited (429)")
    class ServerError : WhiskrKitException("Server error (5xx)")
    class InvalidResponse : WhiskrKitException("Invalid response")
    class DecodingFailed(cause: Throwable) : WhiskrKitException("Decoding failed", cause)
    class HttpError(val statusCode: Int) : WhiskrKitException("HTTP error $statusCode")
    class NetworkError(cause: IOException) : WhiskrKitException("Network error", cause)

    /** Retryable: 429, 5xx, and transport errors. */
    val isRetryable: Boolean
        get() = when (this) {
            is RateLimited, is ServerError, is NetworkError -> true
            else -> false
        }
}
