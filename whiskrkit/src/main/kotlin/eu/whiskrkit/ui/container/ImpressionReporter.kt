package eu.whiskrkit.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.model.SurveyImpressionEvent
import eu.whiskrkit.core.model.SurveyImpressionTrigger

/**
 * Records a survey's `seen` state and `shown`/`dismissed` impressions.
 *
 * Containers call [markInteracted] before any teardown that is not a dismissal
 * and [surveyClosed] on every teardown path. Dismissals are reported from the
 * explicit callbacks, not from disposal — disposal also happens on every
 * configuration change.
 */
internal class ImpressionReporter(private val surveyId: String) {
    private var interacted = false
    private var terminalReported = false

    /** How this presentation got on screen; assigned from saveable state each composition. */
    var trigger: SurveyImpressionTrigger = SurveyImpressionTrigger.MANUAL

    /** Suppresses the `dismissed` event for the teardown that follows. */
    fun markInteracted() {
        interacted = true
    }

    /** Reports `dismissed` once, unless the teardown was an interaction. */
    fun surveyClosed() {
        if (interacted || terminalReported) return
        terminalReported = true
        WhiskrKit.recordImpression(surveyId, SurveyImpressionEvent.DISMISSED, trigger)
    }
}

/**
 * [surveyId] must be the presentation template's `id` — the same id used to
 * submit. `seen` and `shown` are recorded on display; the saveable guard makes
 * a presentation report `shown` exactly once across recreation.
 */
@Composable
internal fun rememberImpressionReporter(surveyId: String): ImpressionReporter {
    val reporter = remember(surveyId) { ImpressionReporter(surveyId) }
    // Consumed once per presentation; saved across recreation.
    val triggerName = rememberSaveable(surveyId) {
        WhiskrKit.consumeImpressionTrigger(surveyId).name
    }
    reporter.trigger = SurveyImpressionTrigger.valueOf(triggerName)

    var shownReported by rememberSaveable(surveyId) { mutableStateOf(false) }
    LaunchedEffect(surveyId) {
        if (!shownReported) {
            shownReported = true
            WhiskrKit.trackSurveySeen(surveyId)
            WhiskrKit.recordImpression(surveyId, SurveyImpressionEvent.SHOWN, reporter.trigger)
        }
    }
    return reporter
}
