package eu.whiskrkit.ui.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.ThumbsRating
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import eu.whiskrkit.ui.WhiskrIcons
import eu.whiskrkit.theme.LocalWhiskrKitTheme

private val ThumbsGreen = Color(0xFF2E7D32)

@Composable
internal fun ThumbsRatingQuestion(
    template: ThumbsSurveyTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val selected = (answer as? SurveyAnswer.Thumbs)?.rating
    val haptics = LocalHapticFeedback.current

    QuestionContainer(
        title = template.title,
        subtitle = template.subtitle,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ThumbOption(
                    label = stringResource(R.string.whiskrkit_thumbs_down),
                    icon = WhiskrIcons.ThumbDown,
                    filledIcon = WhiskrIcons.ThumbDownFilled,
                    accent = MaterialTheme.colorScheme.error,
                    isSelected = selected == ThumbsRating.THUMBS_DOWN,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAnswer(
                            if (selected == ThumbsRating.THUMBS_DOWN) {
                                null
                            } else {
                                SurveyAnswer.Thumbs(ThumbsRating.THUMBS_DOWN)
                            },
                        )
                    },
                )
                ThumbOption(
                    label = stringResource(R.string.whiskrkit_thumbs_up),
                    icon = WhiskrIcons.ThumbUp,
                    filledIcon = WhiskrIcons.ThumbUpFilled,
                    accent = ThumbsGreen,
                    isSelected = selected == ThumbsRating.THUMBS_UP,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAnswer(
                            if (selected == ThumbsRating.THUMBS_UP) {
                                null
                            } else {
                                SurveyAnswer.Thumbs(ThumbsRating.THUMBS_UP)
                            },
                        )
                    },
                )
            }
            if (showError) {
                RequiredErrorMessage(stringResource(R.string.whiskrkit_required_thumbs))
            }
        }
    }
}

@Composable
private fun RowScope.ThumbOption(
    label: String,
    icon: ImageVector,
    filledIcon: ImageVector,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val theme = LocalWhiskrKitTheme.current
    val shape = RoundedCornerShape(8.dp)
    Surface(
        shape = shape,
        color = if (isSelected) accent.copy(alpha = 0.15f) else theme.selectionBackground,
        modifier = Modifier
            .weight(1f)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { selected = isSelected },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = if (isSelected) filledIcon else icon,
                contentDescription = null,
                tint = if (isSelected) accent else theme.subheadline.color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = theme.body.textStyle,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) accent else theme.body.color,
            )
        }
    }
}
