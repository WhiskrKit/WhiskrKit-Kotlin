package eu.whiskrkit.ui.question

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.TextSurveyTemplate
import androidx.compose.material3.Text

/**
 * Free-text question. The character cap is enforced by truncation; the live
 * counter appears when focused or past 80% of the cap, turning to the warning
 * color past 90%.
 */
@Composable
internal fun TextFeedbackQuestion(
    template: TextSurveyTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = (answer as? SurveyAnswer.Text)?.feedback ?: ""
    val limit = template.characterLimit
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fieldDescription = stringResource(R.string.whiskrkit_a11y_text_field)

    QuestionContainer(
        title = template.title,
        subtitle = template.description,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column {
            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    val capped = newValue.take(limit)
                    onAnswer(
                        if (capped.isBlank()) null else SurveyAnswer.Text(capped),
                    )
                },
                minLines = 3,
                maxLines = 6,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = fieldDescription },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                if (showError) {
                    RequiredErrorMessage(stringResource(R.string.whiskrkit_required_text))
                }
                if (isFocused || text.length > limit * 8 / 10) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.whiskrkit_char_count, text.length, limit),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (text.length > limit * 9 / 10) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
