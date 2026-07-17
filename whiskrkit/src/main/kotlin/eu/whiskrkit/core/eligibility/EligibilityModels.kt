package eu.whiskrkit.core.eligibility

import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.serialization.IsoInstant
import kotlinx.serialization.Serializable

/**
 * Context sent with every eligibility check. Dates are ISO-8601, as the
 * backend expects.
 *
 * The maps deliberately have no defaults: `WireJson` skips default-valued
 * fields, and empty maps must still be encoded as `{}` for wire parity with
 * the iOS SDK.
 */
@Serializable
internal data class SurveyEligibilityContext(
    val deviceId: String,
    val appVersion: String,
    val locale: String,
    val sessionCount: Int,
    val installDate: IsoInstant,
    val lastSurveyDate: IsoInstant? = null,
    val completedSurveys: Map<String, IsoInstant>,
    /** Surveys put on screen, whatever the outcome. */
    val seenSurveys: Map<String, IsoInstant>,
)

@Serializable
internal data class SurveyEligibilityResponse(
    val shouldShow: Boolean,
    val survey: SurveyTemplate? = null,
    val nextCheckAfter: IsoInstant? = null,
    val removeFromHistory: Boolean? = null,
)
