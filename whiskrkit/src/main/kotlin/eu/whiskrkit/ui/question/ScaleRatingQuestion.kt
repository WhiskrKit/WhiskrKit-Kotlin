package eu.whiskrkit.ui.question

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.theme.LocalWhiskrKitTheme

/**
 * NPS-style numeric scale laid out as an adaptive FlowRow. Tapping the
 * selected score deselects it and removes the answer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScaleRatingQuestion(
    template: ScaleRatingTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedScore = (answer as? SurveyAnswer.NpsRating)?.score
    val haptics = LocalHapticFeedback.current
    val theme = LocalWhiskrKitTheme.current
    val selectedLabel = stringResource(R.string.whiskrkit_a11y_selected)
    val notSelectedLabel = stringResource(R.string.whiskrkit_a11y_not_selected)

    QuestionContainer(
        title = template.title,
        subtitle = template.subtitle,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (score in template.ratingRange.values) {
                    val isSelected = selectedScore == score
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        label = "scoreScale",
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    scaleColor(score, template.ratingRange)
                                } else {
                                    theme.selectionBackground
                                },
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAnswer(if (isSelected) null else SurveyAnswer.NpsRating(score))
                            }
                            .semantics {
                                selected = isSelected
                                stateDescription = if (isSelected) selectedLabel else notSelectedLabel
                            },
                    ) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            // Selected chips have a bright fixed-color background;
                            // black stays legible on all of them in both modes.
                            color = if (isSelected) Color.Black else theme.body.color,
                        )
                    }
                }
            }
            if (showError) {
                RequiredErrorMessage(
                    stringResource(
                        R.string.whiskrkit_required_scale,
                        template.ratingRange.min,
                        template.ratingRange.max,
                    ),
                )
            }
        }
    }
}

/** Red → yellow → green interpolation across the rating range. */
private fun scaleColor(score: Int, range: ScaleRatingTemplate.RatingRange): Color {
    val span = (range.max - range.min).coerceAtLeast(1)
    val progress = (score - range.min).toFloat() / span
    return if (progress < 0.5f) {
        Color(red = 1f, green = progress / 0.5f, blue = 0f)
    } else {
        Color(red = 1f - (progress - 0.5f) / 0.5f, green = 1f, blue = 0f)
    }
}
