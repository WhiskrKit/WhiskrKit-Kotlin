package eu.whiskrkit.ui.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.MultipleChoiceTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.theme.LocalWhiskrKitTheme

/**
 * Single- or multi-select choice list. Uses real Checkbox/RadioButton
 * components and `selectable`/`toggleable` semantics so TalkBack announces
 * state changes natively (no manual announcements, improving on iOS).
 * Selections are stored as option *ids* (wire parity).
 */
@Composable
internal fun MultipleChoiceQuestion(
    template: MultipleChoiceTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedIds = (answer as? SurveyAnswer.MultipleChoice)?.optionIds.orEmpty()
    val haptics = LocalHapticFeedback.current

    fun update(newSelection: List<String>) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onAnswer(
            if (newSelection.isEmpty()) null else SurveyAnswer.MultipleChoice(newSelection),
        )
    }

    QuestionContainer(
        title = template.title,
        subtitle = template.subtitle,
        isRequired = template.isRequired,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in template.options) {
                val isSelected = option.id in selectedIds
                ChoiceRow(
                    label = option.label,
                    isSelected = isSelected,
                    isMultiSelect = template.allowsMultiSelection,
                    onToggle = {
                        update(
                            when {
                                template.allowsMultiSelection && isSelected -> selectedIds - option.id
                                template.allowsMultiSelection -> selectedIds + option.id
                                isSelected -> emptyList() // single-select: tap again deselects (iOS parity)
                                else -> listOf(option.id)
                            },
                        )
                    },
                )
            }
            if (showError) {
                RequiredErrorMessage(stringResource(R.string.whiskrkit_required_choice))
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onToggle: () -> Unit,
) {
    val theme = LocalWhiskrKitTheme.current
    val shape = RoundedCornerShape(8.dp)
    val selectionModifier = if (isMultiSelect) {
        Modifier.toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
    } else {
        Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = onToggle)
    }

    Surface(
        shape = shape,
        color = theme.selectionBackground,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) theme.selectionTint else MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = shape,
            )
            .then(selectionModifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // row handles interaction
                    colors = CheckboxDefaults.colors(checkedColor = theme.selectionTint),
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = null, // row handles interaction
                    colors = RadioButtonDefaults.colors(selectedColor = theme.selectionTint),
                )
            }
            Text(
                text = label,
                style = theme.body.textStyle,
                color = theme.body.color,
            )
        }
    }
}
