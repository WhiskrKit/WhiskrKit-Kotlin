package eu.whiskrkit.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A survey's on-screen lifecycle event. [DISMISSED] means the survey left the
 * screen without a submission; a submit reports no second event.
 */
@Serializable
internal enum class SurveyImpressionEvent {
    @SerialName("shown")
    SHOWN,

    @SerialName("dismissed")
    DISMISSED,
}

/** How the survey came to be on screen. */
@Serializable
internal enum class SurveyImpressionTrigger {
    /** Shown because the eligibility check granted it. */
    @SerialName("targeted")
    TARGETED,

    /** The host app asked for it directly, bypassing targeting. */
    @SerialName("manual")
    MANUAL,
}

@Serializable
internal data class SurveyImpressionRequest(
    val event: SurveyImpressionEvent,
    val trigger: SurveyImpressionTrigger,
)
