package eu.whiskrkit.e2e

import android.content.Context
import eu.whiskrkit.core.eligibility.SharedPrefsEligibilityStorage
import eu.whiskrkit.core.eligibility.WhiskrKitEligibilityService
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.network.NetworkService
import eu.whiskrkit.core.network.WhiskrKitException
import eu.whiskrkit.internal.DeviceInfo
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * End-to-end round trip against a locally running whiskrkit-server (branch
 * `survey-dismissal-tracking`), exercising the real SDK stack — [NetworkService],
 * [WhiskrKitEligibilityService], [SharedPrefsEligibilityStorage] — over real
 * HTTP. Skipped automatically when no server is listening.
 *
 * Server prerequisites (see whiskrkit-server repo):
 * - Postgres + Redis up, `.build/debug/WhiskrkitServer serve` on :8080
 * - survey `test-survey`: repeatPolicy `once`, active, sampleRate 1.0
 * - API key `wk_test_e2e-verification-key`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalServerE2ETest {

    private val baseUrl = "http://127.0.0.1:8080".toHttpUrl()
    private val apiKey = "wk_test_e2e-verification-key"
    private val surveyId = "test-survey"

    private lateinit var storage: SharedPrefsEligibilityStorage
    private lateinit var network: NetworkService
    private lateinit var service: WhiskrKitEligibilityService

    @Before
    fun setUpFreshDevice() {
        assumeTrue("local whiskrkit-server not running; skipping E2E", serverReachable())

        // A fresh device per run: unique prefs file, unique deviceId.
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("e2e-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        storage = SharedPrefsEligibilityStorage(prefs)
        storage.initializeIfNeeded()
        storage.incrementSessionCount()

        val deviceInfo = DeviceInfo(context) { storage.deviceId }
        network = NetworkService(baseUrl, deviceInfo)
        network.apiKey = apiKey
        service = WhiskrKitEligibilityService(network, storage, deviceInfo)
    }

    private fun serverReachable(): Boolean = runCatching {
        val client = OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(2))
            .build()
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("api/v1/survey/$surveyId").build())
            .header("Authorization", "Bearer $apiKey")
            .build()
        client.newCall(request).execute().use { it.code == 200 }
    }.getOrDefault(false)

    /**
     * The bug this whole change exists to fix, end to end: a dismissed survey
     * must not be granted again.
     */
    @Test
    fun `dismissal round trip - grant, dismiss, then declined for ~30 days`() = runBlocking {
        // 1. Fresh device: server grants.
        val granted = service.checkEligibility(surveyId)
        assertNotNull("fresh device should be granted the 100%-targeted survey", granted)

        // 2. Survey goes on screen: seen record + shown impression
        //    (what the impression reporter does on display).
        storage.seenSurveys = storage.seenSurveys + (surveyId to Instant.now())
        network.recordImpression(surveyId, SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.TARGETED)

        // 3. User dismisses without submitting: dismissed impression, nothing else.
        network.recordImpression(surveyId, SurveyImpressionEvent.DISMISSED, SurveyImpressionTrigger.TARGETED)

        // 4. Next check: the context now carries seenSurveys, so the `.once`
        //    policy must decline and hand back a finite ~30 day nextCheckAfter.
        val afterDismissal = service.checkEligibility(surveyId)
        assertNull("dismissed .once survey must not be granted again", afterDismissal)

        val nextCheck = storage.nextCheckAfter(surveyId)
        assertNotNull("server must return a nextCheckAfter for the seen survey", nextCheck)
        val days = ChronoUnit.HOURS.between(Instant.now(), nextCheck).toDouble() / 24.0
        assertTrue("expected ~30 day cooldown, got $days days", days > 29 && days < 31)

        // 5. Third check is suppressed client-side by the cached nextCheckAfter —
        //    no network call at all until the window passes.
        val suppressed = service.checkEligibility(surveyId)
        assertNull(suppressed)
    }

    @Test
    fun `completion still declines via completedSurveys alone`() = runBlocking {
        val granted = service.checkEligibility(surveyId)
        assertNotNull(granted)

        // Simulate what a successful submit records.
        storage.completedSurveys = storage.completedSurveys + (surveyId to Instant.now())

        val afterCompletion = service.checkEligibility(surveyId)
        assertNull("completed .once survey must not be granted again", afterCompletion)
    }

    @Test
    fun `impression endpoint accepts both events and rejects an unknown survey`() = runBlocking {
        network.recordImpression(surveyId, SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL)
        network.recordImpression(surveyId, SurveyImpressionEvent.DISMISSED, SurveyImpressionTrigger.MANUAL)

        var thrown: WhiskrKitException? = null
        try {
            network.recordImpression("no-such-survey", SurveyImpressionEvent.SHOWN, SurveyImpressionTrigger.MANUAL)
        } catch (e: WhiskrKitException) {
            thrown = e
        }
        assertTrue("unknown survey should 404", thrown is WhiskrKitException.NotFound)
    }
}
