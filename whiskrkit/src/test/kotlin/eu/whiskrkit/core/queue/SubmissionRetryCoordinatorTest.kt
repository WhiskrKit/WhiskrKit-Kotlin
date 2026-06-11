package eu.whiskrkit.core.queue

import eu.whiskrkit.FakeSubmissionStorage
import eu.whiskrkit.FakeSurveyApi
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.network.WhiskrKitException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SubmissionRetryCoordinatorTest {

    private var currentTime: Instant = Instant.parse("2026-06-11T12:00:00Z")
    private val api = FakeSurveyApi()
    private val queue = SubmissionQueue(FakeSubmissionStorage()) { currentTime }
    private val coordinator = SubmissionRetryCoordinator(queue, api)

    private fun enqueue(surveyId: String = "survey-1"): PendingSubmission {
        val submission = PendingSubmission(
            surveyId = surveyId,
            response = SurveyResponse(mapOf("q" to SurveyAnswer.NpsRating(5))),
            timestamp = currentTime,
            expiresAt = currentTime.plus(7, ChronoUnit.DAYS),
        )
        queue.enqueue(submission)
        return submission
    }

    @Test
    fun `successful retry dequeues and passes idempotency key`() = runTest {
        val submission = enqueue()
        coordinator.retryPendingSubmissions()

        assertEquals(0, queue.count)
        assertEquals(1, api.submissions.size)
        val (surveyId, _, idempotencyKey) = api.submissions.single()
        assertEquals(submission.surveyId, surveyId)
        assertEquals(submission.idempotencyKey, idempotencyKey)
    }

    @Test
    fun `failed retry increments retry count and keeps submission`() = runTest {
        api.submitError = WhiskrKitException.ServerError()
        val submission = enqueue()

        coordinator.retryPendingSubmissions()

        assertEquals(1, queue.count)
        assertEquals(1, queue.getSubmission(submission.id)?.retryCount)
    }

    @Test
    fun `submission is dropped after max retries`() = runTest {
        api.submitError = WhiskrKitException.ServerError()
        enqueue()

        repeat(SubmissionQueueConfig.MAX_RETRIES) {
            coordinator.retryPendingSubmissions()
            currentTime = currentTime.plusSeconds(SubmissionQueueConfig.RETRY_THROTTLE_SECONDS + 1)
        }

        assertEquals(0, queue.count)
    }

    @Test
    fun `retry does nothing for an empty queue`() = runTest {
        coordinator.retryPendingSubmissions()
        assertTrue(api.submissions.isEmpty())
    }

    @Test
    fun `multiple submissions retry independently`() = runTest {
        enqueue("survey-1")
        enqueue("survey-2")

        coordinator.retryPendingSubmissions()

        assertEquals(0, queue.count)
        assertEquals(listOf("survey-1", "survey-2"), api.submissions.map { it.first })
    }
}
