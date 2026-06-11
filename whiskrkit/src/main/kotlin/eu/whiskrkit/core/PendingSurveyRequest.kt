package eu.whiskrkit.core

import eu.whiskrkit.core.model.SurveyTemplate

/**
 * The imperative trigger signal observed by the host composable. Unlike the iOS
 * `pendingSurveyId: String?`, this can carry an already-fetched template so the
 * eligibility response's payload is not fetched twice (decision P1).
 */
internal sealed interface PendingSurveyRequest {
    /** `present(surveyId)` — template must still be fetched. */
    data class Fetch(val surveyId: String) : PendingSurveyRequest

    /** Eligibility already granted — template carried through. */
    data class Present(val template: SurveyTemplate) : PendingSurveyRequest
}
