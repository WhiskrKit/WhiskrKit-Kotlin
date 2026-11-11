package eu.whiskrkit.core.queue

import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.internal.IsoTimestamp
import eu.whiskrkit.internal.Timestamp
import kotlinx.serialization.Serializable
import java.util.UUID

internal object SubmissionQueueConfig {
    /** Maximum number of submissions to keep in queue. */
    const val MAX_QUEUE_SIZE: Int = 5

    /** Maximum retry attempts per submission. */
    const val MAX_RETRIES: Int = 5

    /** Minimum time between retry attempts (5 minutes). */
    const val RETRY_THROTTLE_SECONDS: Long = 300

    /** Number of days before submissions expire. */
    const val EXPIRATION_DAYS: Long = 7

    /** SharedPreferences key for persistence. */
    const val STORAGE_KEY: String = "pendingSubmissions"
}

/**
 * A failed submission waiting for retry. Immutable; retry bookkeeping uses
 * [withRetryAttempt].
 */
@Serializable
internal data class PendingSubmission(
    val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val surveyId: String,
    val response: SurveyResponse,
    val timestamp: IsoTimestamp = Timestamp.now(),
    val expiresAt: IsoTimestamp = Timestamp.now().plusDays(SubmissionQueueConfig.EXPIRATION_DAYS),
    val retryCount: Int = 0,
    val lastRetryAttempt: IsoTimestamp? = null,
) {
    fun isExpired(now: Timestamp): Boolean = now.isAfter(expiresAt)

    fun shouldRetry(now: Timestamp): Boolean =
        !isExpired(now) && retryCount < SubmissionQueueConfig.MAX_RETRIES

    fun canRetryNow(now: Timestamp): Boolean {
        if (!shouldRetry(now)) return false
        val lastRetry = lastRetryAttempt ?: return true
        return now.secondsSince(lastRetry) >= SubmissionQueueConfig.RETRY_THROTTLE_SECONDS
    }

    fun withRetryAttempt(now: Timestamp): PendingSubmission =
        copy(retryCount = retryCount + 1, lastRetryAttempt = now)
}
