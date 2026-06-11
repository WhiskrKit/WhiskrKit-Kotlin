package eu.whiskrkit.core.eligibility

import android.content.Context
import eu.whiskrkit.FakeEligibilityStorage
import eu.whiskrkit.FakeSurveyApi
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.internal.DeviceInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EligibilityServiceTest {

    private val now: Instant = Instant.parse("2026-06-11T12:00:00Z")
    private val api = FakeSurveyApi()
    private val storage = FakeEligibilityStorage()
    private val context: Context = RuntimeEnvironment.getApplication()
    private val deviceInfo = DeviceInfo(context) { storage.deviceId }
    private val service = WhiskrKitEligibilityService(api, storage, deviceInfo) { now }

    private val template = BannerTemplate(id = "t1", title = "Hello")

    @Test
    fun `returns template and records last survey date when eligible`() = runTest {
        api.eligibilityResult = SurveyEligibilityResponse(shouldShow = true, survey = template)

        val result = service.checkEligibility("survey-1")

        assertNotNull(result)
        assertEquals(now, storage.lastSurveyDate)
    }

    @Test
    fun `returns null when server declines`() = runTest {
        api.eligibilityResult = SurveyEligibilityResponse(shouldShow = false)

        assertNull(service.checkEligibility("survey-1"))
        assertNull(storage.lastSurveyDate)
    }

    @Test
    fun `persists and honours nextCheckAfter cache hint`() = runTest {
        api.eligibilityResult = SurveyEligibilityResponse(
            shouldShow = false,
            nextCheckAfter = now.plusSeconds(3600),
        )

        service.checkEligibility("survey-1")
        service.checkEligibility("survey-1") // within the cache window

        assertEquals(1, api.eligibilityCalls)
    }

    @Test
    fun `removeFromHistory clears completion and cache`() = runTest {
        storage.completedSurveys = mapOf("survey-1" to now.minusSeconds(86_400))
        api.eligibilityResult = SurveyEligibilityResponse(
            shouldShow = false,
            nextCheckAfter = now.plusSeconds(3600),
            removeFromHistory = true,
        )

        service.checkEligibility("survey-1")

        assertTrue(storage.completedSurveys.isEmpty())
        assertNull(storage.nextCheckAfter("survey-1"))
    }

    @Test
    fun `network failure returns null without crashing`() = runTest {
        api.eligibilityResult = null // FakeSurveyApi throws ServerError

        assertNull(service.checkEligibility("survey-1"))
    }

    @Test
    fun `sends storage-derived context to the backend`() = runTest {
        storage.completedSurveys = mapOf("done-1" to now.minusSeconds(100))
        storage.lastSurveyDate = now.minusSeconds(50)
        api.eligibilityResult = SurveyEligibilityResponse(shouldShow = false)

        service.checkEligibility("survey-1")

        val sentContext = api.eligibilityContexts.single()
        assertEquals("test-device", sentContext.deviceId)
        assertEquals(3, sentContext.sessionCount)
        assertEquals(storage.installDate, sentContext.installDate)
        assertEquals(storage.lastSurveyDate, sentContext.lastSurveyDate)
        assertEquals(setOf("done-1"), sentContext.completedSurveys.keys)
    }
}
