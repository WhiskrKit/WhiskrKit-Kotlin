package eu.whiskrkit.core.queue

import eu.whiskrkit.FakeSubmissionStorage
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SubmissionQueueTest {

    private val baseTime: Instant = Instant.parse("2026-06-11T12:00:00Z")
    private var currentTime: Instant = baseTime
    private val storage = FakeSubmissionStorage()
    private val queue = SubmissionQueue(storage) { currentTime }

    private fun submission(surveyId: String = "survey-1") = PendingSubmission(
        surveyId = surveyId,
        response = SurveyResponse(mapOf("q" to SurveyAnswer.NpsRating(5))),
        timestamp = currentTime,
        expiresAt = currentTime.plus(7, ChronoUnit.DAYS),
    )

    @Test
    fun `enqueue persists and counts`() {
        queue.enqueue(submission())
        assertEquals(1, queue.count)
        assertEquals(1, storage.stored.size)
    }

    @Test
    fun `enqueue replaces older submission for the same survey`() {
        val first = submission("survey-1")
        val second = submission("survey-1")
        queue.enqueue(first)
        queue.enqueue(second)
        assertEquals(1, queue.count)
        assertEquals(second.id, queue.getSubmission(second.id)?.id)
        assertNull(queue.getSubmission(first.id))
    }

    @Test
    fun `queue evicts oldest beyond max size`() {
        repeat(SubmissionQueueConfig.MAX_QUEUE_SIZE + 1) { index ->
            queue.enqueue(submission("survey-$index"))
        }
        assertEquals(SubmissionQueueConfig.MAX_QUEUE_SIZE, queue.count)
        // survey-0 was the oldest and is gone
        assertTrue(storage.stored.none { it.surveyId == "survey-0" })
    }

    @Test
    fun `dequeue removes by id`() {
        val item = submission()
        queue.enqueue(item)
        queue.dequeue(item.id)
        assertEquals(0, queue.count)
    }

    @Test
    fun `expired submissions are cleaned up on read`() {
        queue.enqueue(submission())
        currentTime = baseTime.plus(8, ChronoUnit.DAYS)
        assertTrue(queue.retryableSubmissions().isEmpty())
        assertEquals(0, queue.count)
    }

    @Test
    fun `recently attempted submissions are throttled`() {
        val item = submission()
        queue.enqueue(item)
        queue.recordRetryAttempt(item.id)

        currentTime = baseTime.plusSeconds(60)
        assertTrue(queue.retryableSubmissions().isEmpty())

        currentTime = baseTime.plusSeconds(SubmissionQueueConfig.RETRY_THROTTLE_SECONDS + 1)
        assertEquals(1, queue.retryableSubmissions().size)
    }

    @Test
    fun `recordRetryAttempt increments count and timestamps`() {
        val item = submission()
        queue.enqueue(item)
        queue.recordRetryAttempt(item.id)
        val updated = queue.getSubmission(item.id)!!
        assertEquals(1, updated.retryCount)
        assertEquals(currentTime, updated.lastRetryAttempt)
    }

    @Test
    fun `queue loads persisted submissions on construction`() {
        storage.stored = listOf(submission("persisted"))
        val fresh = SubmissionQueue(storage) { currentTime }
        assertEquals(1, fresh.count)
    }

    @Test
    fun `queue drops expired submissions on construction`() {
        storage.stored = listOf(submission("old"))
        currentTime = baseTime.plus(8, ChronoUnit.DAYS)
        val fresh = SubmissionQueue(storage) { currentTime }
        assertEquals(0, fresh.count)
    }

    @Test
    fun `clear empties queue and storage`() {
        queue.enqueue(submission())
        queue.clear()
        assertEquals(0, queue.count)
        assertTrue(storage.stored.isEmpty())
    }

    @Test
    fun `pending submission retry gating`() {
        val item = submission()
        assertTrue(item.canRetryNow(baseTime))
        assertFalse(item.isExpired(baseTime))

        val maxedOut = item.copy(retryCount = SubmissionQueueConfig.MAX_RETRIES)
        assertFalse(maxedOut.canRetryNow(baseTime))

        val expired = item.copy(expiresAt = baseTime.minusSeconds(1))
        assertTrue(expired.isExpired(baseTime))
        assertFalse(expired.canRetryNow(baseTime))
    }
}
