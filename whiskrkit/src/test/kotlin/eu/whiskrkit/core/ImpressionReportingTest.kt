package eu.whiskrkit.core

import eu.whiskrkit.FakeSubmissionStorage
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionRequest
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.network.NetworkService
import eu.whiskrkit.core.network.WhiskrKitException
import eu.whiskrkit.core.queue.SubmissionQueue
import eu.whiskrkit.core.serialization.WireJson
import eu.whiskrkit.internal.DeviceInfo
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImpressionReportingTest {

    private lateinit var server: MockWebServer
    private lateinit var networkService: NetworkService
    private lateinit var submissionStorage: FakeSubmissionStorage

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val deviceInfo = DeviceInfo(RuntimeEnvironment.getApplication()) { "device-123" }
        networkService = NetworkService(server.url("/").toString().toHttpUrl(), deviceInfo)
        networkService.apiKey = "test-key"
        submissionStorage = FakeSubmissionStorage()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun configService() = WhiskrKitConfigurationService(
        networkService = networkService,
        submissionQueue = SubmissionQueue(submissionStorage),
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
    )

    @Test
    fun `impression posts the exact wire body to the impression path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202))

        networkService.recordImpression("your-journey-stats", SurveyImpressionEvent.DISMISSED, SurveyImpressionTrigger.TARGETED)

        val request = server.takeRequest()
        assertEquals("/api/v1/survey/your-journey-stats/impression", request.path)
        assertEquals("""{"event":"dismissed","trigger":"targeted"}""", request.body.readUtf8())
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
    }

    @Test
    fun `shown event encodes to the exact string the endpoint expects`() {
        val json = WireJson.encodeToString(
            SurveyImpressionRequest.serializer(),
            SurveyImpressionRequest(SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL),
        )
        assertEquals("""{"event":"shown","trigger":"manual"}""", json)
    }

    /**
     * Analytics must never break UX: a failed impression is swallowed, so a
     * survey still shows and still submits when the endpoint is down.
     */
    @Test
    fun `a failed impression is swallowed rather than thrown`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

        // Must not throw.
        configService().recordImpression("your-journey-stats", SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL)
    }

    /**
     * Unlike a submission, an impression is never queued: a retry would land in
     * the wrong day's totals.
     */
    @Test
    fun `a failed impression is not queued for retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        configService().recordImpression("your-journey-stats", SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL)

        assertTrue(submissionStorage.stored.isEmpty())
    }

    @Test
    fun `unknown event responses map to typed errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))

        var thrown: WhiskrKitException? = null
        try {
            networkService.recordImpression("your-journey-stats", SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL)
        } catch (e: WhiskrKitException) {
            thrown = e
        }

        assertTrue(thrown is WhiskrKitException.BadRequest)
    }
}
