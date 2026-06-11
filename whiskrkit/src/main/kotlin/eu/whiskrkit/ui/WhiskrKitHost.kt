package eu.whiskrkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.PendingSurveyRequest
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.model.validated
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.WhiskrKitTheme
import eu.whiskrkit.theme.resolveWhiskrTheme
import eu.whiskrkit.ui.container.BannerHost
import eu.whiskrkit.ui.container.FullScreenContainer
import eu.whiskrkit.ui.container.SheetContainer
import eu.whiskrkit.ui.container.SurveyTemplateSaver

/**
 * The attachment point for WhiskrKit surveys — the Compose counterpart of the
 * iOS `.whiskrKit()` view modifier.
 *
 * Wrap your root content once, inside your `MaterialTheme`:
 *
 * ```kotlin
 * setContent {
 *     MyAppTheme {
 *         WhiskrKitHost {
 *             AppNavigation()
 *         }
 *     }
 * }
 * ```
 *
 * All survey presentations (sheet, full-screen, banner) render here. Surveys
 * triggered via [WhiskrKit.present], [WhiskrKit.checkAndPresent] or
 * [WhiskrKitSurvey] appear only while a host is in the composition; a trigger
 * fired before the host exists is delivered to the first host that appears.
 *
 * Place exactly one host in the hierarchy. Theming resolves against the
 * ambient [androidx.compose.material3.MaterialTheme] with optional [theme]
 * overrides — see [WhiskrKitTheme].
 */
@Composable
public fun WhiskrKitHost(
    modifier: Modifier = Modifier,
    theme: WhiskrKitTheme? = null,
    content: @Composable () -> Unit,
) {
    val resolvedTheme = resolveWhiskrTheme(theme)
    CompositionLocalProvider(LocalWhiskrKitTheme provides resolvedTheme) {
        Box(modifier = modifier) {
            content()
            SurveyPresenter()
        }
    }
}

/**
 * Automatically checks eligibility for [identifier] and presents the survey if
 * the user qualifies — the Compose counterpart of the iOS
 * `.whiskrKitSurvey(identifier:)` modifier.
 *
 * Place it on the screen where the survey should potentially appear; the check
 * runs when the composable enters the composition. Checks are deduplicated per
 * process and respect the backend's `nextCheckAfter` hint, so recompositions
 * and configuration changes do not cause repeat network calls.
 *
 * A [WhiskrKitHost] must be present (higher in the hierarchy) for the survey
 * to render.
 */
@Composable
public fun WhiskrKitSurvey(identifier: String) {
    LaunchedEffect(identifier) {
        WhiskrKit.autoCheckAndPresent(identifier)
    }
}

/**
 * Owns the currently presented survey. The active template survives
 * configuration changes and process death by round-tripping through its JSON
 * form (decision #9).
 */
@Composable
private fun BoxScope.SurveyPresenter() {
    var activeTemplate by rememberSaveable(stateSaver = SurveyTemplateSaver) {
        mutableStateOf<SurveyTemplate?>(null)
    }

    LaunchedEffect(Unit) {
        WhiskrKit.pendingSurvey.collect { request ->
            if (request == null) return@collect
            WhiskrKit.pendingSurvey.value = null // consume
            val template = when (request) {
                is PendingSurveyRequest.Present -> request.template
                is PendingSurveyRequest.Fetch -> WhiskrKit.fetchSurveyTemplate(request.surveyId)
            }
            template?.validated()?.let { activeTemplate = it }
        }
    }

    when (val template = activeTemplate) {
        null -> Unit
        is SheetTemplate -> SheetContainer(
            template = template,
            onDismiss = { activeTemplate = null },
        )
        is FullScreenFormTemplate -> FullScreenContainer(
            template = template,
            onDismiss = { activeTemplate = null },
        )
        is BannerTemplate -> BannerHost(
            template = template,
            onDismiss = { activeTemplate = null },
            onOpenFollowUp = { followUpId ->
                activeTemplate = null
                WhiskrKit.present(followUpId)
            },
        )
    }
}
