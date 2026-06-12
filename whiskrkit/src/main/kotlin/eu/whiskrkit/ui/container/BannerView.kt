package eu.whiskrkit.ui.container

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.WhiskrKit
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.ui.WhiskrIcons
import eu.whiskrkit.internal.WhiskrLog
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.WhiskrButton
import eu.whiskrkit.theme.WhiskrButtonKind
import eu.whiskrkit.ui.question.QuestionView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Banner presentation ("toast" on the wire): a bottom-aligned overlay card
 * inside the host's Box, slide-in with fade, swipe-down to dismiss. Either
 * hosts an inline question with a compact submit, or "Give feedback /
 * No thanks" buttons where the primary opens a follow-up survey by identifier.
 */
@Composable
internal fun BoxScope.BannerHost(
    template: BannerTemplate,
    onDismiss: () -> Unit,
    onOpenFollowUp: (String) -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    // When the exit animation has fully finished, release the template.
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState && !visibleState.targetState) {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        BannerContent(
            template = template,
            onDismissRequested = { visibleState.targetState = false },
            onOpenFollowUp = onOpenFollowUp,
        )
    }
}

@Composable
internal fun BannerContent(
    template: BannerTemplate,
    onDismissRequested: () -> Unit,
    onOpenFollowUp: (String) -> Unit,
) {
    val theme = LocalWhiskrKitTheme.current
    val dragScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    var response by rememberSaveable(stateSaver = SurveyResponseSaver) {
        mutableStateOf(SurveyResponse())
    }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    val survey = template.survey
    val canSubmit = survey == null || !survey.isRequired || survey.id in response.results

    fun submit() {
        if (canSubmit) {
            WhiskrLog.i(WhiskrLog.UI, "User submitted survey in banner.")
            val submitted = response
            WhiskrKit.scope.launch {
                WhiskrKit.submitSurveyResponse(template.id, submitted)
            }
            onDismissRequested()
        } else {
            submitAttempted = true
        }
    }

    Surface(
        shape = RoundedCornerShape(theme.bannerCornerRadius),
        color = theme.bannerBackground,
        shadowElevation = theme.bannerElevation,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        val target = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                        dragScope.launch { dragOffsetY.snapTo(target) }
                    },
                    onDragEnd = {
                        if (dragOffsetY.value > DISMISS_DRAG_THRESHOLD_PX) {
                            onDismissRequested()
                        } else {
                            dragScope.launch { dragOffsetY.animateTo(0f) }
                        }
                    },
                )
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (template.title != null) {
                        Text(
                            text = template.title,
                            style = theme.headline.textStyle,
                            color = theme.headline.color,
                        )
                    }
                    if (template.description != null) {
                        Text(
                            text = template.description,
                            style = theme.subheadline.textStyle,
                            color = theme.subheadline.color,
                        )
                    }
                }
                IconButton(onClick = onDismissRequested) {
                    Icon(
                        imageVector = WhiskrIcons.Close,
                        contentDescription = stringResource(R.string.whiskrkit_close),
                        tint = theme.body.color,
                    )
                }
            }

            if (survey != null) {
                QuestionView(
                    template = survey,
                    answer = response.results[survey.id],
                    onAnswer = { answer ->
                        response = response.withAnswer(survey.id, answer)
                    },
                    showError = submitAttempted && survey.id !in response.results,
                )
                WhiskrButton(
                    text = stringResource(R.string.whiskrkit_submit),
                    onClick = ::submit,
                    kind = WhiskrButtonKind.Primary,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    WhiskrButton(
                        text = stringResource(R.string.whiskrkit_give_feedback),
                        onClick = {
                            val followUpId = template.followUpIdentifier
                            if (followUpId != null) {
                                onOpenFollowUp(followUpId)
                            }
                        },
                        kind = WhiskrButtonKind.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    WhiskrButton(
                        text = stringResource(R.string.whiskrkit_no_thanks),
                        onClick = onDismissRequested,
                        kind = WhiskrButtonKind.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val DISMISS_DRAG_THRESHOLD_PX = 150f
