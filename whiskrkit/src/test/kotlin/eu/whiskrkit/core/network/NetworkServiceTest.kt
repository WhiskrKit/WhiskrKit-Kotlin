package eu.whiskrkit.core.network

import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.internal.DeviceInfo
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NetworkServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: NetworkService

    private val sheetJson = """
        {"template":"sheet","id":"s1","title":"T",
         "survey":{"template":"thumbsRating","id":"q1","isRequired":true}}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val deviceInfo = DeviceInfo(RuntimeEnvironment.getApplication()) { "device-123" }
        service = NetworkService(server.url("/").toString().toHttpUrl(), deviceInfo)
        service.apiKey = "test-key"
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchSurvey sends auth and platform headers to the right path`() = runTest {
        server.enqueue(MockResponse().setBody(sheetJson))

        val template = service.fetchSurvey("survey-1")

        assertTrue(template is SheetTemplate)
        val request = server.takeRequest()
        assertEquals("/api/v1/survey/survey-1", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        assertEquals("Android", request.getHeader("X-OS-Name"))
        assertEquals("device-123", request.getHeader("X-Device-ID"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("WhiskrKit-android/"))
    }

    @Test
    fun `throws NotInitialized without api key`() = runTest {
        service.apiKey = null
        assertThrows(WhiskrKitException.NotInitialized::class.java) {
            kotlinx.coroutines.runBlocking { service.fetchSurvey("survey-1") }
        }
    }

    @Test
    fun `retries twice on server error then fails`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

        var thrown: WhiskrKitException? = null
        try {
            service.fetchSurvey("survey-1")
        } catch (e: WhiskrKitException) {
            thrown = e
        }

        assertTrue(thrown is WhiskrKitException.ServerError)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `recovers when retry succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody(sheetJson))

        val template = service.fetchSurvey("survey-1")

        assertTrue(template is SheetTemplate)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `does not retry client errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        var thrown: WhiskrKitException? = null
        try {
            service.fetchSurvey("survey-1")
        } catch (e: WhiskrKitException) {
            thrown = e
        }

        assertTrue(thrown is WhiskrKitException.NotFound)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `malformed body maps to DecodingFailed without retry`() = runTest {
        server.enqueue(MockResponse().setBody("{broken"))

        var thrown: WhiskrKitException? = null
        try {
            service.fetchSurvey("survey-1")
        } catch (e: WhiskrKitException) {
            thrown = e
        }

        assertTrue(thrown is WhiskrKitException.DecodingFailed)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `submit posts wire-format body and idempotency key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        service.submitResponse(
            surveyId = "survey-1",
            response = SurveyResponse(mapOf("q1" to SurveyAnswer.NpsRating(7))),
            idempotencyKey = "idem-1",
        )

        val request = server.takeRequest()
        assertEquals("/api/v1/survey/survey-1/submit", request.path)
        assertEquals("idem-1", request.getHeader("X-Idempotency-Key"))
        assertEquals("""{"results":{"q1":{"npsRating":7}}}""", request.body.readUtf8())
    }
}
