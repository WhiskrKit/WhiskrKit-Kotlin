package eu.whiskrkit.core.review

import android.app.Activity
import eu.whiskrkit.ReviewRequester
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lowest star rating that leads to the store review prompt. */
internal const val STORE_REVIEW_MIN_RATING = 4

/** Pause after the survey is dismissed, so the prompt doesn't collide with its exit animation. */
internal const val STORE_REVIEW_DELAY_MILLIS = 750L

/**
 * True when the response rates a star question that has `opensStoreReview`
 * switched on with at least [STORE_REVIEW_MIN_RATING] stars.
 */
internal fun SurveyTemplate.qualifiesForStoreReview(response: SurveyResponse): Boolean {
    val questions = when (this) {
        is SheetTemplate -> listOf(survey)
        is BannerTemplate -> listOfNotNull(survey)
        is FullScreenFormTemplate -> surveys
    }
    return questions.any { question ->
        question is SymbolRatingTemplate &&
            question.opensStoreReview &&
            ((response.results[question.id] as? SurveyAnswer.SymbolRating)?.score ?: 0) >= STORE_REVIEW_MIN_RATING
    }
}

/**
 * Asks for the store review after a survey was submitted successfully, at most
 * once per app version, after a short delay on the activity that is resumed
 * by then. In mock mode it only logs.
 *
 * @param storage null in mock mode; nothing is persisted there.
 * @param requester null when the app didn't pass one; the request is then logged only.
 */
internal class StoreReviewCoordinator(
    private val storage: StoreReviewStorage?,
    private val appVersion: String,
    private val requester: ReviewRequester?,
    private val currentActivity: () -> Activity?,
    private val scope: CoroutineScope,
    private val mockMode: Boolean,
    private val delayMillis: Long = STORE_REVIEW_DELAY_MILLIS,
) {

    /** Call only after the backend accepted the response. */
    fun onSubmissionSucceeded(template: SurveyTemplate, response: SurveyResponse) {
        if (!template.qualifiesForStoreReview(response)) return

        if (mockMode || storage == null) {
            WhiskrLog.i(
                WhiskrLog.CORE,
                "Mock: would request the store review for survey '${template.id}' (not calling the store in mock mode)",
            )
            return
        }
        if (requester == null) {
            WhiskrLog.i(
                WhiskrLog.CORE,
                "Survey '${template.id}' qualifies for a store review, but no ReviewRequester was passed to " +
                    "WhiskrKit.initialize. Add eu.whiskrkit:whiskrkit-android-review to ask Google Play.",
            )
            return
        }
        if (alreadyRequested(storage)) return

        scope.launch {
            delay(delayMillis)
            // Checked again: another survey may have asked during the delay.
            if (alreadyRequested(storage)) return@launch
            val activity = currentActivity() ?: run {
                WhiskrLog.i(WhiskrLog.CORE, "No resumed activity; skipping the store review request")
                return@launch
            }
            storage.lastRequestedVersion = appVersion
            WhiskrLog.i(WhiskrLog.CORE, "Requesting the store review for app version $appVersion")
            try {
                requester.requestReview(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WhiskrLog.w(WhiskrLog.CORE, "Store review request failed", e)
            }
        }
    }

    private fun alreadyRequested(storage: StoreReviewStorage): Boolean {
        val done = storage.lastRequestedVersion == appVersion
        if (done) {
            WhiskrLog.i(WhiskrLog.CORE, "Store review already requested for app version $appVersion; skipping")
        }
        return done
    }
}
