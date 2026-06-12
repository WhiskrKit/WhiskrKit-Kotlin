package eu.whiskrkit.ui.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.ui.WhiskrIcons

private const val STAR_COUNT = 5
private val StarYellow = Color(0xFFFFC107)

/**
 * Five-star rating. `opensStoreReview` is decoded but intentionally ignored;
 * Play In-App Review support is planned as a separate optional artifact.
 */
@Composable
internal fun SymbolRatingQuestion(
    template: SymbolRatingTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val rating = (answer as? SurveyAnswer.SymbolRating)?.score ?: 0
    val haptics = LocalHapticFeedback.current

    QuestionContainer(
        title = template.title,
        subtitle = template.description,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..STAR_COUNT) {
                    val isFilled = star <= rating
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAnswer(SurveyAnswer.SymbolRating(star))
                        },
                        modifier = Modifier.semantics { selected = isFilled },
                    ) {
                        Icon(
                            imageVector = if (isFilled) WhiskrIcons.Star else WhiskrIcons.StarBorder,
                            contentDescription = stringResource(R.string.whiskrkit_a11y_star, star),
                            tint = if (isFilled) StarYellow else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            if (showError) {
                RequiredErrorMessage(
                    stringResource(R.string.whiskrkit_required_symbol, 1, STAR_COUNT),
                )
            }
        }
    }
}
