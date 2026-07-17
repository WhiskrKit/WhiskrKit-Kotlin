package eu.whiskrkit.core.eligibility

import eu.whiskrkit.core.serialization.WireJson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Pins the `/eligible` request body byte-for-byte against the iOS SDK. The two
 * SDKs must stay wire-compatible: same keys, same date format, and empty maps
 * sent as `{}` rather than omitted.
 */
class EligibilityContextSerializationTest {

    @Test
    fun `eligibility body matches the cross-platform wire contract exactly`() {
        val context = SurveyEligibilityContext(
            deviceId = "test-device-id",
            appVersion = "1.0.0",
            locale = "en-US",
            sessionCount = 3,
            installDate = Instant.ofEpochSecond(1_700_000_000),
            lastSurveyDate = Instant.ofEpochSecond(1_705_000_000),
            completedSurveys = mapOf("checkout-feedback" to Instant.ofEpochSecond(1_704_000_000)),
            seenSurveys = mapOf("your-journey-stats" to Instant.ofEpochSecond(1_706_000_000)),
        )

        val json = WireJson.encodeToString(SurveyEligibilityContext.serializer(), context)

        assertEquals(
            """{"deviceId":"test-device-id","appVersion":"1.0.0","locale":"en-US",""" +
                """"sessionCount":3,"installDate":"2023-11-14T22:13:20Z",""" +
                """"lastSurveyDate":"2024-01-11T19:06:40Z",""" +
                """"completedSurveys":{"checkout-feedback":"2023-12-31T05:20:00Z"},""" +
                """"seenSurveys":{"your-journey-stats":"2024-01-23T08:53:20Z"}}""",
            json,
        )
    }

    @Test
    fun `fresh install sends empty maps rather than omitting them`() {
        val context = SurveyEligibilityContext(
            deviceId = "test-device-id",
            appVersion = "1.0.0",
            locale = "en-US",
            sessionCount = 1,
            installDate = Instant.ofEpochSecond(1_700_000_000),
            lastSurveyDate = null,
            completedSurveys = emptyMap(),
            seenSurveys = emptyMap(),
        )

        val json = WireJson.encodeToString(SurveyEligibilityContext.serializer(), context)

        assertEquals(
            """{"deviceId":"test-device-id","appVersion":"1.0.0","locale":"en-US",""" +
                """"sessionCount":1,"installDate":"2023-11-14T22:13:20Z",""" +
                """"completedSurveys":{},"seenSurveys":{}}""",
            json,
        )
    }
}
