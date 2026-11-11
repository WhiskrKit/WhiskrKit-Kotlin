package eu.whiskrkit.core.review

import android.app.Activity
import eu.whiskrkit.ReviewRequester
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.ConfigurationService
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.MultipleChoiceOption
import eu.whiskrkit.core.model.MultipleChoiceTemplate
import eu.whiskrkit.core.model.QuestionTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.core.model.TextSurveyTemplate
import eu.whiskrkit.core.model.ThumbsRating
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private class RecordingRequester : ReviewRequester {
    val calls = mutableListOf<Activity>()
    var error: Exception? = null

    override suspend fun requestReview(activity: Activity) {
        calls += activity
        error?.let { throw it }
    }
}

private class InMemoryStoreReviewStorage : StoreReviewStorage {
    override var lastRequestedVersion: String? = null
}

private class ResultConfigurationService(
    private val result: Boolean,
    private val latencyMillis: Long = 0,
) : ConfigurationService {
    override suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? = null
    override suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean {
        delay(latencyMillis)
        return result
    }
    override suspend fun recordImpression(surveyId: String, event: SurveyImpressionEvent, trigger: SurveyImpressionTrigger) = Unit
    override suspend fun retryPendingSubmissions() = Unit
    override fun configure(apiKey: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoreReviewCoordinatorTest {

    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val requester = RecordingRequester()
    private val storage = InMemoryStoreReviewStorage()

    private fun star(id: String = "stars", opensStoreReview: Boolean = true) =
        SymbolRatingTemplate(id = id, opensStoreReview = opensStoreReview, isRequired = false)

    private fun sheet(question: QuestionTemplate = star()) = SheetTemplate(id = "survey", survey = question)

    private fun stars(score: Int, id: String = "stars") =
        SurveyResponse(mapOf(id to SurveyAnswer.SymbolRating(score)))

    private fun TestScope.coordinator(
        appVersion: String = "1.0 (1)",
        mockMode: Boolean = false,
        currentActivity: () -> Activity? = { activity },
        scope: CoroutineScope = this,
    ) = StoreReviewCoordinator(
        storage = if (mockMode) null else storage,
        appVersion = appVersion,
        requester = requester,
        currentActivity = currentActivity,
        scope = scope,
        mockMode = mockMode,
        clock = { testScheduler.currentTime },
    )

    /** Submitted right now, as if the backend answered instantly. */
    private fun TestScope.submittedNow() = testScheduler.currentTime

    @After
    fun tearDown() {
        WhiskrKit.configurationService = null
        WhiskrKit.storeReview = null
    }

    @Test
    fun `fires for 4 and 5 stars`() = runTest {
        for (score in listOf(4, 5)) {
            storage.lastRequestedVersion = null
            coordinator().onSubmissionSucceeded(sheet(), stars(score), submittedNow())
            advanceUntilIdle()
        }
        assertEquals(listOf(activity, activity), requester.calls)
    }

    @Test
    fun `does not fire for 1 to 3 stars`() = runTest {
        for (score in 1..3) {
            coordinator().onSubmissionSucceeded(sheet(), stars(score), submittedNow())
        }
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `does not fire when the question has the follow-up off`() = runTest {
        coordinator().onSubmissionSucceeded(sheet(star(opensStoreReview = false)), stars(5), submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
    }

    @Test
    fun `does not fire when the question was skipped`() = runTest {
        coordinator().onSubmissionSucceeded(sheet(), SurveyResponse(), submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
    }

    @Test
    fun `waits for the delay after dismissal before asking`() = runTest {
        coordinator().onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceTimeBy(STORE_REVIEW_DELAY_MILLIS - 1)
        runCurrent()
        assertTrue(requester.calls.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `fires once per app version and again after a version change`() = runTest {
        repeat(3) {
            coordinator(appVersion = "1.0 (1)").onSubmissionSucceeded(sheet(), stars(5), submittedNow())
            advanceUntilIdle()
        }
        assertEquals(1, requester.calls.size)
        assertEquals("1.0 (1)", storage.lastRequestedVersion)

        coordinator(appVersion = "1.1 (2)").onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceUntilIdle()
        assertEquals(2, requester.calls.size)
        assertEquals("1.1 (2)", storage.lastRequestedVersion)
    }

    @Test
    fun `two qualifying submissions during the delay ask once`() = runTest {
        val coordinator = coordinator()
        coordinator.onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        coordinator.onSubmissionSucceeded(sheet(), stars(4), submittedNow())
        advanceUntilIdle()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `mock mode never calls the requester`() = runTest {
        coordinator(mockMode = true).onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
    }

    @Test
    fun `without a requester nothing is asked or stored`() = runTest {
        StoreReviewCoordinator(
            storage = storage,
            appVersion = "1.0 (1)",
            requester = null,
            currentActivity = { activity },
            scope = this,
            mockMode = false,
            clock = { testScheduler.currentTime },
        ).onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceUntilIdle()
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `skips without a resumed activity and tries again next time`() = runTest {
        coordinator(currentActivity = { null }).onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `a failing requester is caught`() = runTest {
        requester.error = IllegalStateException("Play not available")
        coordinator().onSubmissionSucceeded(sheet(), stars(5), submittedNow())
        advanceUntilIdle()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `banner with a qualifying star question fires`() = runTest {
        coordinator().onSubmissionSucceeded(BannerTemplate(id = "banner", survey = star()), stars(4), submittedNow())
        advanceUntilIdle()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `banner without a question does not fire`() = runTest {
        coordinator().onSubmissionSucceeded(BannerTemplate(id = "banner"), SurveyResponse(), submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
    }

    @Test
    fun `full-screen form with several questions fires on its qualifying star question`() = runTest {
        val form = FullScreenFormTemplate(
            id = "form",
            surveys = listOf(
                ThumbsSurveyTemplate(id = "thumbs", isRequired = false),
                star(id = "plain-stars", opensStoreReview = false),
                MultipleChoiceTemplate(
                    id = "choice",
                    isRequired = false,
                    options = listOf(MultipleChoiceOption("a", "A")),
                    allowsMultiSelection = false,
                ),
                star(id = "review-stars"),
                TextSurveyTemplate(id = "text", isRequired = false),
            ),
        )
        val response = SurveyResponse(
            mapOf(
                "thumbs" to SurveyAnswer.Thumbs(ThumbsRating.THUMBS_UP),
                "plain-stars" to SurveyAnswer.SymbolRating(2),
                "choice" to SurveyAnswer.MultipleChoice(listOf("a")),
                "review-stars" to SurveyAnswer.SymbolRating(5),
                "text" to SurveyAnswer.Text("Great"),
            ),
        )

        coordinator().onSubmissionSucceeded(form, response, submittedNow())
        advanceUntilIdle()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `full-screen form fires only when the flagged question qualifies`() = runTest {
        val form = FullScreenFormTemplate(
            id = "form",
            surveys = listOf(star(id = "plain-stars", opensStoreReview = false), star(id = "review-stars")),
        )
        val response = SurveyResponse(
            mapOf(
                "plain-stars" to SurveyAnswer.SymbolRating(5),
                "review-stars" to SurveyAnswer.SymbolRating(3),
            ),
        )

        coordinator().onSubmissionSucceeded(form, response, submittedNow())
        advanceUntilIdle()
        assertTrue(requester.calls.isEmpty())
    }

    @Test
    fun `a successful submission through WhiskrKit fires`() = runTest {
        WhiskrKit.configurationService = ResultConfigurationService(result = true)
        WhiskrKit.storeReview = coordinator()

        WhiskrKit.submitSurveyResponse(sheet(), stars(5))
        advanceUntilIdle()

        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `a failed or queued submission through WhiskrKit does not fire`() = runTest {
        WhiskrKit.configurationService = ResultConfigurationService(result = false)
        WhiskrKit.storeReview = coordinator()

        WhiskrKit.submitSurveyResponse(sheet(), stars(5))
        advanceUntilIdle()

        assertTrue(requester.calls.isEmpty())
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `the delay counts from the submit tap, not from the server answer`() = runTest {
        val tappedAt = testScheduler.currentTime
        advanceTimeBy(500) // The submission took 500 ms.
        runCurrent()

        coordinator().onSubmissionSucceeded(sheet(), stars(5), submitTappedAt = tappedAt)
        advanceTimeBy(STORE_REVIEW_DELAY_MILLIS - 500 - 1)
        runCurrent()
        assertTrue(requester.calls.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `a submission slower than the delay asks right away`() = runTest {
        val tappedAt = testScheduler.currentTime
        advanceTimeBy(3_000)
        runCurrent()

        coordinator().onSubmissionSucceeded(sheet(), stars(5), submitTappedAt = tappedAt)
        runCurrent()
        assertEquals(1, requester.calls.size)
    }

    @Test
    fun `a submission slower than 10 seconds skips and leaves the version unused`() = runTest {
        val tappedAt = testScheduler.currentTime
        advanceTimeBy(STORE_REVIEW_MAX_SUBMIT_MILLIS + 1)
        runCurrent()

        coordinator().onSubmissionSucceeded(sheet(), stars(5), submitTappedAt = tappedAt)
        advanceUntilIdle()

        assertTrue(requester.calls.isEmpty())
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `a slow submission through WhiskrKit skips the review`() = runTest {
        WhiskrKit.configurationService =
            ResultConfigurationService(result = true, latencyMillis = STORE_REVIEW_MAX_SUBMIT_MILLIS + 500)
        WhiskrKit.storeReview = coordinator()

        launch { WhiskrKit.submitSurveyResponse(sheet(), stars(5)) }
        advanceUntilIdle()

        assertTrue(requester.calls.isEmpty())
        assertNull(storage.lastRequestedVersion)
    }

    @Test
    fun `a submission through WhiskrKit asks the delay after the tap`() = runTest {
        WhiskrKit.configurationService = ResultConfigurationService(result = true, latencyMillis = 400)
        WhiskrKit.storeReview = coordinator()

        launch { WhiskrKit.submitSurveyResponse(sheet(), stars(5)) }
        advanceTimeBy(STORE_REVIEW_DELAY_MILLIS - 1)
        runCurrent()
        assertTrue(requester.calls.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, requester.calls.size)
        assertEquals("1.0 (1)", storage.lastRequestedVersion)
    }

    @Test
    fun `storage keeps the requested version in shared preferences`() {
        val context = RuntimeEnvironment.getApplication()
        SharedPrefsStoreReviewStorage(context).lastRequestedVersion = "2.0 (20)"
        assertEquals("2.0 (20)", SharedPrefsStoreReviewStorage(context).lastRequestedVersion)

        SharedPrefsStoreReviewStorage(context).lastRequestedVersion = null
        assertNull(SharedPrefsStoreReviewStorage(context).lastRequestedVersion)
    }
}
