package eu.whiskrkit.core.queue

import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.serialization.IsoInstant
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
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
    val timestamp: IsoInstant = Instant.now(),
    val expiresAt: IsoInstant = Instant.now().plus(SubmissionQueueConfig.EXPIRATION_DAYS, ChronoUnit.DAYS),
    val retryCount: Int = 0,
    val lastRetryAttempt: IsoInstant? = null,
) {
    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)

    fun shouldRetry(now: Instant): Boolean =
        !isExpired(now) && retryCount < SubmissionQueueConfig.MAX_RETRIES

    fun canRetryNow(now: Instant): Boolean {
        if (!shouldRetry(now)) return false
        val lastRetry = lastRetryAttempt ?: return true
        return Duration.between(lastRetry, now).seconds >= SubmissionQueueConfig.RETRY_THROTTLE_SECONDS
    }

    fun withRetryAttempt(now: Instant): PendingSubmission =
        copy(retryCount = retryCount + 1, lastRetryAttempt = now)
}
