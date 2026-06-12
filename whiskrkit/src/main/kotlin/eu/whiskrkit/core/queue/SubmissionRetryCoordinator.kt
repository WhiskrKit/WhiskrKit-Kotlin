package eu.whiskrkit.core.queue

import eu.whiskrkit.core.network.SurveyApi
import eu.whiskrkit.core.network.WhiskrKitException
import eu.whiskrkit.internal.WhiskrLog

/**
 * Retries queued submissions. Lifecycle triggers (app foreground, connectivity
 * restored) are registered by [eu.whiskrkit.WhiskrKit] at initialize time —
 * the coordinator itself stays free of Android plumbing.
 */
internal class SubmissionRetryCoordinator(
    private val queue: SubmissionQueue,
    private val api: SurveyApi,
) {
    private var isRetrying = false

    suspend fun retryPendingSubmissions() {
        if (isRetrying) {
            WhiskrLog.i(WhiskrLog.CACHE, "Retry already in progress, skipping")
            return
        }
        isRetrying = true
        try {
            val submissions = queue.retryableSubmissions()
            if (submissions.isEmpty()) {
                return
            }
            WhiskrLog.i(WhiskrLog.CACHE, "Retrying ${submissions.size} pending submissions")
            for (submission in submissions) {
                retrySubmission(submission)
            }
        } finally {
            isRetrying = false
        }
    }

    private suspend fun retrySubmission(submission: PendingSubmission) {
        try {
            api.submitResponse(
                surveyId = submission.surveyId,
                response = submission.response,
                idempotencyKey = submission.idempotencyKey,
            )
            WhiskrLog.i(WhiskrLog.CACHE, "Retry succeeded: ${submission.surveyId}")
            queue.dequeue(submission.id)
        } catch (e: WhiskrKitException) {
            WhiskrLog.w(WhiskrLog.CACHE, "Retry failed: ${submission.surveyId}", e)
            queue.recordRetryAttempt(submission.id)
            val updated = queue.getSubmission(submission.id) ?: return
            if (updated.retryCount >= SubmissionQueueConfig.MAX_RETRIES) {
                WhiskrLog.w(WhiskrLog.CACHE, "Max retries exceeded, removing: ${submission.surveyId}")
                queue.dequeue(submission.id)
            }
        }
    }
}
