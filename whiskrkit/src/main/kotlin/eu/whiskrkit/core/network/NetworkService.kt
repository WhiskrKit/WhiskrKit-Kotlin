package eu.whiskrkit.core.network

import eu.whiskrkit.BuildConfig
import eu.whiskrkit.core.eligibility.SurveyEligibilityContext
import eu.whiskrkit.core.eligibility.SurveyEligibilityResponse
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.serialization.WireJson
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import eu.whiskrkit.internal.DeviceInfo

/**
 * The three backend endpoints, abstracted for testability (the iOS coordinator
 * depends on the concrete NetworkService; the seam here replaces that).
 */
internal interface SurveyApi {
    suspend fun fetchSurvey(identifier: String): SurveyTemplate

    suspend fun checkEligibility(
        surveyId: String,
        context: SurveyEligibilityContext,
    ): SurveyEligibilityResponse

    suspend fun submitResponse(
        surveyId: String,
        response: SurveyResponse,
        idempotencyKey: String? = null,
    )
}

/**
 * OkHttp port of the iOS `NetworkService`: 30 s timeouts, up to [MAX_RETRIES]
 * retries with exponential backoff (1 s, 2 s) on 429/5xx/transport errors,
 * 4xx mapped to typed errors and never retried.
 */
internal class NetworkService(
    private val baseUrl: HttpUrl,
    private val deviceInfo: DeviceInfo,
    private val client: OkHttpClient = defaultClient(),
) : SurveyApi {

    var apiKey: String? = null

    override suspend fun fetchSurvey(identifier: String): SurveyTemplate {
        val request = requestBuilder("api/v1/survey/$identifier").get().build()
        return withRetry { execute(request).decode(SurveyTemplate.serializer()) }
    }

    override suspend fun checkEligibility(
        surveyId: String,
        context: SurveyEligibilityContext,
    ): SurveyEligibilityResponse {
        val body = WireJson.encodeToString(SurveyEligibilityContext.serializer(), context)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = requestBuilder("api/v1/survey/$surveyId/eligible").post(body).build()
        return withRetry { execute(request).decode(SurveyEligibilityResponse.serializer()) }
    }

    override suspend fun submitResponse(
        surveyId: String,
        response: SurveyResponse,
        idempotencyKey: String?,
    ) {
        val body = WireJson.encodeToString(SurveyResponse.serializer(), response)
            .toRequestBody(JSON_MEDIA_TYPE)
        val builder = requestBuilder("api/v1/survey/$surveyId/submit").post(body)
        if (idempotencyKey != null) {
            builder.header("X-Idempotency-Key", idempotencyKey)
        }
        val request = builder.build()
        withRetry { execute(request) }
    }

    // region Request plumbing

    private fun requestBuilder(path: String): Request.Builder {
        val key = apiKey ?: throw WhiskrKitException.NotInitialized()
        val url = baseUrl.newBuilder().addPathSegments(path).build()
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("Accept-Language", deviceInfo.localeTag.ascii())
            .header("X-Device-ID", deviceInfo.deviceId.ascii())
            .header("X-App-Bundle-ID", deviceInfo.packageName.ascii())
            .header("X-App-Version", deviceInfo.appVersion.ascii())
            .header("X-App-Build", deviceInfo.appBuild.ascii())
            .header("X-OS-Name", "Android")
            .header("X-OS-Version", deviceInfo.osVersion.ascii())
            .header("X-Device-Model", deviceInfo.deviceModel.ascii())
            .header("X-Device-Identifier", deviceInfo.deviceIdentifier.ascii())
            .header("X-Language", deviceInfo.language.ascii())
            .header("X-Region", deviceInfo.region.ascii())
            .header("X-Timezone", deviceInfo.timezone.ascii())
            .header("User-Agent", "WhiskrKit-android/${BuildConfig.SDK_VERSION}")
            .header("Cache-Control", "no-cache")
    }

    private suspend fun execute(request: Request): String {
        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            throw WhiskrKitException.NetworkError(e)
        }
        response.use {
            mapStatusCode(it.code)
            return it.body?.string().orEmpty()
        }
    }

    private fun <T> String.decode(strategy: DeserializationStrategy<T>): T = try {
        WireJson.decodeFromString(strategy, this)
    } catch (e: IllegalArgumentException) {
        throw WhiskrKitException.DecodingFailed(e)
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: WhiskrKitException) {
                if (e.isRetryable && attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS shl attempt)
                    attempt++
                } else {
                    throw e
                }
            }
        }
    }

    private fun mapStatusCode(code: Int) {
        when (code) {
            in 200..299 -> Unit
            400 -> throw WhiskrKitException.BadRequest()
            401 -> throw WhiskrKitException.Unauthorized()
            403 -> throw WhiskrKitException.Forbidden()
            404 -> throw WhiskrKitException.NotFound()
            429 -> throw WhiskrKitException.RateLimited()
            in 500..599 -> throw WhiskrKitException.ServerError()
            else -> throw WhiskrKitException.HttpError(code)
        }
    }

    // endregion

    private companion object {
        const val MAX_RETRIES = 2
        const val RETRY_DELAY_MS = 1000L
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        /** OkHttp rejects non-ASCII header values; Build.MODEL can contain them. */
        fun String.ascii(): String = filter { it.code in 32..126 }.ifEmpty { "unknown" }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { runCatching { cancel() } }
}
