package eu.whiskrkit.core

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
