package eu.whiskrkit.ui.question

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.whiskrkit.core.model.MultipleChoiceTemplate
import eu.whiskrkit.core.model.QuestionTemplate
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.core.model.TextSurveyTemplate
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import eu.whiskrkit.core.model.UnknownQuestionTemplate

/** Dispatches a question template to its composable. */
@Composable
internal fun QuestionView(
    template: QuestionTemplate,
    answer: SurveyAnswer?,
    onAnswer: (SurveyAnswer?) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    when (template) {
        is ScaleRatingTemplate ->
            ScaleRatingQuestion(template, answer, onAnswer, showError, modifier)
        is SymbolRatingTemplate ->
            SymbolRatingQuestion(template, answer, onAnswer, showError, modifier)
        is ThumbsSurveyTemplate ->
            ThumbsRatingQuestion(template, answer, onAnswer, showError, modifier)
        is TextSurveyTemplate ->
            TextFeedbackQuestion(template, answer, onAnswer, showError, modifier)
        is MultipleChoiceTemplate ->
            MultipleChoiceQuestion(template, answer, onAnswer, showError, modifier)
        is UnknownQuestionTemplate -> Unit // filtered out by SurveyTemplate.validated()
    }
}
