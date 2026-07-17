package eu.whiskrkit.core

import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.network.NetworkService
import eu.whiskrkit.core.network.WhiskrKitException
import eu.whiskrkit.core.queue.PendingSubmission
import eu.whiskrkit.core.queue.SubmissionQueue
import eu.whiskrkit.core.queue.SubmissionRetryCoordinator
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface ConfigurationService {
    suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate?
    suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean
    suspend fun recordImpression(surveyId: String, event: SurveyImpressionEvent, trigger: SurveyImpressionTrigger)
    suspend fun retryPendingSubmissions()
    fun configure(apiKey: String)
}

internal class WhiskrKitConfigurationService(
    private val networkService: NetworkService,
    private val submissionQueue: SubmissionQueue,
    private val scope: CoroutineScope,
) : ConfigurationService {

    private val retryCoordinator = SubmissionRetryCoordinator(submissionQueue, networkService)

    override fun configure(apiKey: String) {
        networkService.apiKey = apiKey
        scope.launch { retryPendingSubmissions() }
    }

    override suspend fun retryPendingSubmissions() {
        retryCoordinator.retryPendingSubmissions()
    }

    override suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? = try {
        networkService.fetchSurvey(identifier)
    } catch (e: WhiskrKitException) {
        WhiskrLog.w(WhiskrLog.NETWORKING, "Fetching survey failed", e)
        null
    }

    /** Fire-and-forget analytics: failures are logged and dropped, never queued. */
    override suspend fun recordImpression(surveyId: String, event: SurveyImpressionEvent, trigger: SurveyImpressionTrigger) {
        try {
            networkService.recordImpression(surveyId, event, trigger)
            WhiskrLog.i(WhiskrLog.NETWORKING, "Reported '${event.name.lowercase()}' (${trigger.name.lowercase()}) impression for survey '$surveyId'")
        } catch (e: WhiskrKitException) {
            WhiskrLog.w(WhiskrLog.NETWORKING, "Impression report failed for '$surveyId'. Ignoring.", e)
        }
    }

    override suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean {
        retryPendingSubmissions()
        return try {
            networkService.submitResponse(surveyId, response)
            WhiskrLog.i(WhiskrLog.NETWORKING, "Survey response submitted successfully")
            true
        } catch (e: WhiskrKitException) {
            WhiskrLog.w(WhiskrLog.NETWORKING, "Submission failed, adding to queue", e)
            submissionQueue.enqueue(PendingSubmission(surveyId = surveyId, response = response))
            false
        }
    }
}
