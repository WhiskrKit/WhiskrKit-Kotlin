package eu.whiskrkit

import android.os.Looper
import eu.whiskrkit.SurveyCompletion.Answer
import eu.whiskrkit.SurveyCompletion.QuestionType
import eu.whiskrkit.SurveyCompletion.Value
import eu.whiskrkit.core.ConfigurationService
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.model.ThumbsRating
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private class StubConfigurationService : ConfigurationService {
    var succeeds = true
    var submitCount = 0
    var onSubmit: () -> Unit = {}
    override suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? = null
    override suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean {
        submitCount++
        onSubmit()
        return succeeds
    }
    override suspend fun recordImpression(surveyId: String, event: SurveyImpressionEvent, trigger: SurveyImpressionTrigger) = Unit
    override suspend fun retryPendingSubmissions() = Unit
    override fun configure(apiKey: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SurveyCompletionListenerTest {

    private val service = StubConfigurationService()
    private val received = mutableListOf<SurveyCompletion>()

    @Before
    fun setUp() {
        WhiskrKit.configurationService = service
        WhiskrKit.setOnSurveyCompletedListener { received += it }
    }

    @After
    fun tearDown() {
        WhiskrKit.setOnSurveyCompletedListener(null)
        WhiskrKit.configurationService = null
    }

    @Test
    fun `reports the survey id and every answer with its type and value`() = runTest {
        WhiskrKit.submitSurveyResponse(
            "s1",
            SurveyResponse(
                mapOf(
                    "s1-q10" to SurveyAnswer.MultipleChoice(listOf("a", "c")),
                    "s1-q2" to SurveyAnswer.SymbolRating(4),
                    "s1-q1" to SurveyAnswer.NpsRating(9),
                    "s1-q3" to SurveyAnswer.Thumbs(ThumbsRating.THUMBS_DOWN),
                    "s1-q4" to SurveyAnswer.Text("Great"),
                ),
            ),
        )

        assertEquals(
            listOf(
                SurveyCompletion(
                    "s1",
                    listOf(
                        Answer("s1-q1", QuestionType.SCALE_RATING, Value.Score(9)),
                        Answer("s1-q2", QuestionType.SYMBOL_RATING, Value.Score(4)),
                        Answer("s1-q3", QuestionType.THUMBS_RATING, Value.Thumbs(isUp = false)),
                        Answer("s1-q4", QuestionType.TEXTUAL_SURVEY, Value.Text("Great")),
                        Answer("s1-q10", QuestionType.MULTIPLE_CHOICE, Value.Choices(listOf("a", "c"))),
                    ),
                ),
            ),
            received,
        )
    }

    @Test
    fun `fires when the submission fails`() = runTest {
        service.succeeds = false
        WhiskrKit.submitSurveyResponse("s", SurveyResponse(mapOf("q" to SurveyAnswer.SymbolRating(2))))
        assertEquals(1, received.size)
    }

    @Test
    fun `fires before the network submission`() = runTest {
        val events = mutableListOf<String>()
        WhiskrKit.setOnSurveyCompletedListener { events += "listener" }
        service.onSubmit = { events += "network" }
        WhiskrKit.submitSurveyResponse("s", SurveyResponse(mapOf("q" to SurveyAnswer.NpsRating(5))))
        assertEquals(listOf("listener", "network"), events)
    }

    @Test
    fun `fires once per submission`() = runTest {
        WhiskrKit.submitSurveyResponse("a", SurveyResponse(emptyMap()))
        WhiskrKit.submitSurveyResponse("b", SurveyResponse(emptyMap()))
        assertEquals(listOf("a", "b"), received.map { it.surveyId })
    }

    @Test
    fun `does not fire before WhiskrKit is initialized`() = runTest {
        WhiskrKit.configurationService = null
        WhiskrKit.submitSurveyResponse("s", SurveyResponse(mapOf("q" to SurveyAnswer.NpsRating(5))))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `a thumbs question without a choice is left out`() {
        val completion = SurveyCompletion.from("s", SurveyResponse(mapOf("q" to SurveyAnswer.Thumbs(ThumbsRating.NONE))))
        assertTrue(completion.answers.isEmpty())
    }

    @Test
    fun `a submission off the main thread is delivered on the main thread`() {
        var deliveredOn: Thread? = null
        WhiskrKit.setOnSurveyCompletedListener { deliveredOn = Thread.currentThread() }
        val worker = Thread {
            runBlocking { WhiskrKit.submitSurveyResponse("s", SurveyResponse(emptyMap())) }
        }
        worker.start()
        worker.join()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(Looper.getMainLooper().thread, deliveredOn)
    }
}
