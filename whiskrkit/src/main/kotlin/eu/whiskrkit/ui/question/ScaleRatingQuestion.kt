package eu.whiskrkit.ui.question

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import eu.whiskrkit.R
import eu.whiskrkit.core.model.RatingDisplayStyle
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.theme.LocalWhiskrKitTheme

/**
 * A numeric scale (or five stars, for `displayStyle` stars with a 1 to 5 range).
 * Selected colors depend on the metric, see [ScaleRatingPalette]. Tapping the
 * selected score deselects it and removes the answer.
 */
@Composable
internal fun ScaleRatingQuestion(
    template: ScaleRatingTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedScore = (answer as? SurveyAnswer.ScaleRating)?.score
    val resources = LocalResources.current
    val (minLabel, maxLabel) = ScaleRatingLabels.endLabels(template, resources)

    QuestionContainer(
        title = template.title,
        subtitle = template.subtitle,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            when (template.resolvedDisplayStyle) {
                RatingDisplayStyle.NUMBERS -> ScoreButtonGrid(template, selectedScore, onAnswer)
                RatingDisplayStyle.STARS -> StarRatingRow(
                    rating = selectedScore ?: 0,
                    onSelect = { star -> onAnswer(if (star == selectedScore) null else SurveyAnswer.ScaleRating(star)) },
                    extraDescription = ScaleRatingLabels.endsDescription(template, resources),
                    fillWidth = true,
                )
            }
            if (minLabel != null || maxLabel != null) {
                EndLabelRow(minLabel, maxLabel)
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

/**
 * The score buttons in rows: as many per row as fit (at most six), rows as even as
 * possible, each row spanning the full width. Buttons grow with the font scale and
 * never get smaller than 48dp, so large text wraps to more rows.
 */
@Composable
private fun ScoreButtonGrid(
    template: ScaleRatingTemplate,
    selectedScore: Int?,
    onAnswer: (SurveyAnswer?) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val buttonSize = max(ScaleRatingLayout.MinimumButtonSize, ScaleRatingLayout.MinimumButtonSize * fontScale)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val perRow = ScaleRatingLayout.buttonsPerRow(maxWidth, buttonSize)
        Column(verticalArrangement = Arrangement.spacedBy(ScaleRatingLayout.ButtonSpacing)) {
            for (row in ScaleRatingLayout.rows(template.ratingRange, perRow)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (row.size > 1) Arrangement.SpaceBetween else Arrangement.Start,
                ) {
                    for (score in row) {
                        ScoreButton(
                            score = score,
                            template = template,
                            isSelected = selectedScore == score,
                            size = buttonSize,
                            onClick = { isSelected ->
                                onAnswer(if (isSelected) null else SurveyAnswer.ScaleRating(score))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreButton(
    score: Int,
    template: ScaleRatingTemplate,
    isSelected: Boolean,
    size: Dp,
    onClick: (isSelected: Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val theme = LocalWhiskrKitTheme.current
    val resources = LocalResources.current
    val selectedLabel = stringResource(R.string.whiskrkit_a11y_selected)
    val notSelectedLabel = stringResource(R.string.whiskrkit_a11y_not_selected)
    val description = ScaleRatingLabels.buttonDescription(score, template, resources)
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f, label = "scoreScale")
    val shape = RoundedCornerShape(8.dp)
    // Black or white, whichever the survey's text color is closest to, so the outline
    // stands out from the survey background in light and dark mode.
    val outline = if (theme.body.color.luminance() > 0.5f) Color.White else Color.Black

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(shape)
            .background(
                if (isSelected) {
                    ScaleRatingPalette.swatch(score, template.metric, template.ratingRange).color
                } else {
                    theme.selectionBackground
                },
            )
            // Selection never relies on color alone: an outline, the scale and the selected state.
            .then(if (isSelected) Modifier.border(ScaleRatingPalette.SelectedOutlineWidth, outline, shape) else Modifier)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick(isSelected)
            }
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) selectedLabel else notSelectedLabel
                contentDescription = description
            },
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) ScaleRatingPalette.SelectedText else theme.body.color,
            maxLines = 1,
            // The description already says the score.
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * One line under the grid: the first option's label on the left, the last one's on
 * the right, each wrapping within its half. TalkBack reads them as part of each
 * button's description instead.
 */
@Composable
private fun EndLabelRow(minLabel: String?, maxLabel: String?) {
    val theme = LocalWhiskrKitTheme.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clearAndSetSemantics {},
    ) {
        Text(
            text = minLabel.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = theme.subheadline.color,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = maxLabel.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = theme.subheadline.color,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
