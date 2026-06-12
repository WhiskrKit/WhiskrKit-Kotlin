package eu.whiskrkit.core.eligibility

import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.serialization.IsoInstant
import kotlinx.serialization.Serializable

/**
 * Context sent with every eligibility check. Dates are ISO-8601, as the
 * backend expects.
 */
@Serializable
internal data class SurveyEligibilityContext(
    val deviceId: String,
    val appVersion: String,
    val locale: String,
    val sessionCount: Int,
    val installDate: IsoInstant,
    val lastSurveyDate: IsoInstant? = null,
    val completedSurveys: Map<String, IsoInstant> = emptyMap(),
)

@Serializable
internal data class SurveyEligibilityResponse(
    val shouldShow: Boolean,
    val survey: SurveyTemplate? = null,
    val nextCheckAfter: IsoInstant? = null,
    val removeFromHistory: Boolean? = null,
)
