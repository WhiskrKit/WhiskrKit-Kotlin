package eu.whiskrkit.core.eligibility

import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.network.SurveyApi
import eu.whiskrkit.internal.DeviceInfo
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.coroutines.CancellationException
import java.time.Instant

internal interface EligibilityService {
    suspend fun checkEligibility(surveyId: String): SurveyTemplate?
}

/**
 * Manages per-survey eligibility checks, ported from iOS: dedupes in-flight
 * checks, honours the server's `nextCheckAfter` cache hint and
 * `removeFromHistory`, and fails silently — the SDK never crashes the host app.
 */
internal class WhiskrKitEligibilityService(
    private val api: SurveyApi,
    private val storage: EligibilityStorage,
    private val deviceInfo: DeviceInfo,
    private val now: () -> Instant = Instant::now,
) : EligibilityService {

    /** Survey IDs for which an eligibility network call is currently in-flight. */
    private val inFlightSurveyIds = mutableSetOf<String>()

    override suspend fun checkEligibility(surveyId: String): SurveyTemplate? {
        if (surveyId in inFlightSurveyIds) {
            WhiskrLog.i(WhiskrLog.NETWORKING, "Eligibility check already in-flight for '$surveyId', skipping.")
            return null
        }

        val nextCheck = storage.nextCheckAfter(surveyId)
        if (nextCheck != null && nextCheck.isAfter(now())) {
            WhiskrLog.i(WhiskrLog.NETWORKING, "Skipping eligibility check for '$surveyId' until $nextCheck.")
            return null
        }

        inFlightSurveyIds += surveyId
        try {
            val response = api.checkEligibility(surveyId, buildContext())

            storage.setNextCheckAfter(response.nextCheckAfter, surveyId)

            if (response.removeFromHistory == true) {
                storage.removeCompletedSurvey(surveyId)
                storage.setNextCheckAfter(null, surveyId)
            }

            if (response.shouldShow && response.survey != null) {
                storage.lastSurveyDate = now()
                WhiskrLog.i(WhiskrLog.NETWORKING, "Eligibility granted for '$surveyId'.")
                return response.survey
            }

            WhiskrLog.i(WhiskrLog.NETWORKING, "Server declined eligibility for '$surveyId'.")
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WhiskrLog.w(WhiskrLog.NETWORKING, "Eligibility check failed for '$surveyId'. Showing nothing.", e)
            return null
        } finally {
            inFlightSurveyIds -= surveyId
        }
    }

    private fun buildContext() = SurveyEligibilityContext(
        deviceId = storage.deviceId,
        appVersion = deviceInfo.appVersion,
        locale = deviceInfo.localeTag,
        sessionCount = storage.sessionCount,
        installDate = storage.installDate,
        lastSurveyDate = storage.lastSurveyDate,
        completedSurveys = storage.completedSurveys,
    )
}
