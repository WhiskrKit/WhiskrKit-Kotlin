package eu.whiskrkit

import eu.whiskrkit.core.AttributeValue
import eu.whiskrkit.core.ConfigurationService
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class CapturingConfigurationService : ConfigurationService {
    val responses = mutableListOf<SurveyResponse>()
    override suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? = null
    override suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean {
        responses += response
        return true
    }
    override suspend fun recordImpression(surveyId: String, event: SurveyImpressionEvent, trigger: SurveyImpressionTrigger) = Unit
    override suspend fun retryPendingSubmissions() = Unit
    override fun configure(apiKey: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhiskrKitAttributesTest {

    private val service = CapturingConfigurationService()
    private val answers = mapOf("q1" to SurveyAnswer.NpsRating(9))

    @Before
    fun setUp() {
        WhiskrKit.configurationService = service
        WhiskrKit.clearAttributes()
    }

    @After
    fun tearDown() {
        WhiskrKit.clearAttributes()
        WhiskrKit.configurationService = null
    }

    private suspend fun submit() = WhiskrKit.submitSurveyResponse("s1", SurveyResponse(answers))

    @Test
    fun `submission carries the global attributes of every type`() = runTest {
        WhiskrKit.setAttribute("plan", "pro")
        WhiskrKit.setAttribute("sessions", 3)
        WhiskrKit.setAttribute("bytes", 5_000_000_000L)
        WhiskrKit.setAttribute("ratio", 0.5)
        WhiskrKit.setAttribute("beta", true)

        submit()

        assertEquals(
            mapOf(
                "plan" to AttributeValue.Text("pro"),
                "sessions" to AttributeValue.Integer(3),
                "bytes" to AttributeValue.Integer(5_000_000_000L),
                "ratio" to AttributeValue.Decimal(0.5),
                "beta" to AttributeValue.Bool(true),
            ),
            service.responses.single().attributes,
        )
        assertEquals(answers, service.responses.single().results)
    }

    @Test
    fun `no attributes means null on the response`() = runTest {
        submit()
        assertNull(service.responses.single().attributes)
    }

    @Test
    fun `clearing all attributes yields null, not an empty map`() = runTest {
        WhiskrKit.setAttribute("a", "b")
        WhiskrKit.clearAttributes()
        submit()
        assertNull(service.responses.single().attributes)
    }

    @Test
    fun `value changed after presentation but before submit is sent as set at submit time`() = runTest {
        WhiskrKit.setAttribute("plan", "free")
        // The survey is "presented" here. The state the container holds is a plain SurveyResponse
        // without attributes, so nothing is frozen at presentation time.
        val inFlight = SurveyResponse(answers)
        WhiskrKit.setAttribute("plan", "pro")
        WhiskrKit.removeAttribute("legacy")

        WhiskrKit.submitSurveyResponse("s1", inFlight)

        assertEquals(mapOf("plan" to AttributeValue.Text("pro")), service.responses.single().attributes)
    }

    @Test
    fun `changes after submit do not touch an already sent response`() = runTest {
        WhiskrKit.setAttribute("plan", "free")
        submit()
        WhiskrKit.setAttribute("plan", "pro")
        assertEquals(mapOf("plan" to AttributeValue.Text("free")), service.responses.single().attributes)
    }

    @Test
    fun `setAttributes merges and ignores unsupported types`() = runTest {
        WhiskrKit.setAttribute("a", 1)
        WhiskrKit.setAttributes(mapOf("b" to "x", "c" to listOf(1, 2), "d" to Double.NaN))
        submit()
        assertEquals(setOf("a", "b"), service.responses.single().attributes!!.keys)
    }

    @Test
    fun `removeAttribute removes only that key`() = runTest {
        WhiskrKit.setAttributes(mapOf("a" to 1, "b" to 2))
        WhiskrKit.removeAttribute("a")
        submit()
        assertEquals(setOf("b"), service.responses.single().attributes!!.keys)
    }

    @Test
    fun `submit time sanitizing applies the limits`() = runTest {
        WhiskrKit.setAttribute("a-note", "x".repeat(300))
        (1..25).forEach { WhiskrKit.setAttribute("k%02d".format(it), it) }
        submit()
        val sent = service.responses.single().attributes!!
        assertEquals(20, sent.size)
        assertEquals(256, (sent.getValue("a-note") as AttributeValue.Text).value.length)
    }

    @Test
    fun `invalid non-finite number is ignored`() = runTest {
        WhiskrKit.setAttribute("x", Double.POSITIVE_INFINITY)
        submit()
        assertNull(service.responses.single().attributes)
    }
}
