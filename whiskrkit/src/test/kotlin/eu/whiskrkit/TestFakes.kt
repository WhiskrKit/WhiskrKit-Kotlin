package eu.whiskrkit

import eu.whiskrkit.core.eligibility.EligibilityStorage
import eu.whiskrkit.core.eligibility.SurveyEligibilityContext
import eu.whiskrkit.core.eligibility.SurveyEligibilityResponse
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.network.SurveyApi
import eu.whiskrkit.core.network.WhiskrKitException
import eu.whiskrkit.core.queue.PendingSubmission
import eu.whiskrkit.core.queue.SubmissionStorage
import java.time.Instant

internal class FakeSurveyApi : SurveyApi {
    var fetchResult: SurveyTemplate? = null
    var eligibilityResult: SurveyEligibilityResponse? = null
    var submitError: WhiskrKitException? = null

    var eligibilityCalls = 0
    val eligibilityContexts = mutableListOf<SurveyEligibilityContext>()
    val submissions = mutableListOf<Triple<String, SurveyResponse, String?>>()

    override suspend fun fetchSurvey(identifier: String): SurveyTemplate =
        fetchResult ?: throw WhiskrKitException.NotFound()

    override suspend fun checkEligibility(
        surveyId: String,
        context: SurveyEligibilityContext,
    ): SurveyEligibilityResponse {
        eligibilityCalls++
        eligibilityContexts += context
        return eligibilityResult ?: throw WhiskrKitException.ServerError()
    }

    override suspend fun submitResponse(
        surveyId: String,
        response: SurveyResponse,
        idempotencyKey: String?,
    ) {
        submitError?.let { throw it }
        submissions += Triple(surveyId, response, idempotencyKey)
    }
}

internal class FakeSubmissionStorage : SubmissionStorage {
    var stored: List<PendingSubmission> = emptyList()
    var saveCount = 0

    override fun save(submissions: List<PendingSubmission>) {
        stored = submissions
        saveCount++
    }

    override fun load(): List<PendingSubmission> = stored

    override fun clear() {
        stored = emptyList()
    }
}

internal class FakeEligibilityStorage : EligibilityStorage {
    override var deviceId: String = "test-device"
    override var sessionCount: Int = 3
    override var installDate: Instant = Instant.parse("2026-01-01T00:00:00Z")
    override var lastSurveyDate: Instant? = null
    override var completedSurveys: Map<String, Instant> = emptyMap()

    private val nextChecks = mutableMapOf<String, Instant>()

    override fun nextCheckAfter(surveyId: String): Instant? = nextChecks[surveyId]

    override fun setNextCheckAfter(date: Instant?, surveyId: String) {
        if (date == null) nextChecks.remove(surveyId) else nextChecks[surveyId] = date
    }

    override fun removeCompletedSurvey(surveyId: String) {
        completedSurveys = completedSurveys - surveyId
    }

    override fun incrementSessionCount() {
        sessionCount++
    }

    override fun initializeIfNeeded() = Unit
}
