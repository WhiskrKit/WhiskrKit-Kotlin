package eu.whiskrkit.ui.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.ui.WhiskrIcons

internal const val STAR_COUNT = 5
private val StarYellow = Color(0xFFFFC107)

/** Five-star rating. */
@Composable
internal fun SymbolRatingQuestion(
    template: SymbolRatingTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val rating = (answer as? SurveyAnswer.SymbolRating)?.score ?: 0

    QuestionContainer(
        title = template.title,
        subtitle = template.description,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            StarRatingRow(
                rating = rating,
                onSelect = { star -> onAnswer(SurveyAnswer.SymbolRating(star)) },
            )
            if (showError) {
                RequiredErrorMessage(
                    pluralStringResource(R.plurals.whiskrkit_required_symbol, STAR_COUNT, 1, STAR_COUNT),
                )
            }
        }
    }
}

/**
 * Five tappable stars, used by symbol rating questions and by scale ratings shown
 * as stars. [rating] is 0 while nothing is chosen. [extraDescription] is appended
 * to every star's TalkBack description, e.g. what the end labels mean. With
 * [fillWidth], the stars spread over the full width, so end labels below line up
 * with the first and last star.
 */
@Composable
internal fun StarRatingRow(
    rating: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    extraDescription: String? = null,
    fillWidth: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        horizontalArrangement = if (fillWidth) Arrangement.SpaceBetween else Arrangement.spacedBy(4.dp),
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
    ) {
        for (star in 1..STAR_COUNT) {
            val isFilled = star <= rating
            val starDescription = pluralStringResource(R.plurals.whiskrkit_a11y_star, star, star)
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(star)
                },
                modifier = Modifier.semantics { selected = isFilled },
            ) {
                Icon(
                    imageVector = if (isFilled) WhiskrIcons.Star else WhiskrIcons.StarBorder,
                    contentDescription = listOfNotNull(starDescription, extraDescription).joinToString(". "),
                    tint = if (isFilled) StarYellow else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
