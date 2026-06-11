package eu.whiskrkit.core

import eu.whiskrkit.core.eligibility.SharedPrefsEligibilityStorage
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.queue.PendingSubmission
import eu.whiskrkit.core.queue.SharedPrefsSubmissionStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SharedPrefsStorageTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `eligibility storage initializes device id and install date once`() {
        val storage = SharedPrefsEligibilityStorage(context)
        storage.initializeIfNeeded()

        val deviceId = storage.deviceId
        val installDate = storage.installDate
        assertTrue(deviceId.isNotEmpty())

        storage.initializeIfNeeded()
        assertEquals(deviceId, storage.deviceId)
        assertEquals(installDate, storage.installDate)
    }

    @Test
    fun `session count increments`() {
        val storage = SharedPrefsEligibilityStorage(context)
        assertEquals(0, storage.sessionCount)
        storage.incrementSessionCount()
        storage.incrementSessionCount()
        assertEquals(2, storage.sessionCount)
    }

    @Test
    fun `completed surveys round-trip with second precision`() {
        val storage = SharedPrefsEligibilityStorage(context)
        val date = Instant.parse("2026-06-11T12:30:45Z")
        storage.completedSurveys = mapOf("survey-1" to date)

        assertEquals(mapOf("survey-1" to date), storage.completedSurveys)

        storage.removeCompletedSurvey("survey-1")
        assertTrue(storage.completedSurveys.isEmpty())
    }

    @Test
    fun `nextCheckAfter set get and clear`() {
        val storage = SharedPrefsEligibilityStorage(context)
        val date = Instant.parse("2026-07-01T00:00:00Z")

        assertNull(storage.nextCheckAfter("survey-1"))
        storage.setNextCheckAfter(date, "survey-1")
        assertEquals(date, storage.nextCheckAfter("survey-1"))
        storage.setNextCheckAfter(null, "survey-1")
        assertNull(storage.nextCheckAfter("survey-1"))
    }

    @Test
    fun `last survey date persists and clears`() {
        val storage = SharedPrefsEligibilityStorage(context)
        val date = Instant.parse("2026-06-01T08:00:00Z")

        assertNull(storage.lastSurveyDate)
        storage.lastSurveyDate = date
        assertEquals(date, storage.lastSurveyDate)
        storage.lastSurveyDate = null
        assertNull(storage.lastSurveyDate)
    }

    @Test
    fun `submission storage round-trips pending submissions`() {
        val storage = SharedPrefsSubmissionStorage(context)
        val now = Instant.parse("2026-06-11T12:00:00Z")
        val submission = PendingSubmission(
            surveyId = "survey-1",
            response = SurveyResponse(mapOf("q" to SurveyAnswer.Text("hello"))),
            timestamp = now,
            expiresAt = now.plus(7, ChronoUnit.DAYS),
            retryCount = 2,
            lastRetryAttempt = now.minusSeconds(600),
        )

        storage.save(listOf(submission))
        assertEquals(listOf(submission), storage.load())

        storage.clear()
        assertTrue(storage.load().isEmpty())
    }

    @Test
    fun `submission storage survives corrupted payload`() {
        val prefs = context.getSharedPreferences("eu.whiskrkit", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("pendingSubmissions", "{not json").apply()

        val storage = SharedPrefsSubmissionStorage(context)
        assertTrue(storage.load().isEmpty())
    }

    @Test
    fun `fresh ids are generated per submission`() {
        val a = PendingSubmission(surveyId = "s", response = SurveyResponse())
        val b = PendingSubmission(surveyId = "s", response = SurveyResponse())
        assertNotEquals(a.id, b.id)
        assertNotEquals(a.idempotencyKey, b.idempotencyKey)
    }
}
