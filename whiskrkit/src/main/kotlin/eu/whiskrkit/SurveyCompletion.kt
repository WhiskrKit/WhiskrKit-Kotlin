package eu.whiskrkit

import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.ThumbsRating

/**
 * A survey the user just submitted, passed to the listener set with
 * [WhiskrKit.setOnSurveyCompletedListener].
 *
 * @property surveyId the id of the survey that was submitted.
 * @property answers the answers the user gave, ordered by question id. Questions
 *   the user left unanswered are not included.
 */
public data class SurveyCompletion(
    val surveyId: String,
    val answers: List<Answer>,
) {
    /** One answered question. */
    public data class Answer(
        val questionId: String,
        val questionType: QuestionType,
        val value: Value,
    )

    /** The kind of question that was answered. */
    public enum class QuestionType {
        SCALE_RATING,
        SYMBOL_RATING,
        THUMBS_RATING,
        TEXTUAL_SURVEY,
        MULTIPLE_CHOICE,
    }

    /** The submitted value. */
    public sealed interface Value {
        /** A scale or star rating. */
        public data class Score(val score: Int) : Value

        /** A thumbs rating: `true` for thumbs up, `false` for thumbs down. */
        public data class Thumbs(val isUp: Boolean) : Value

        /** Free text. */
        public data class Text(val text: String) : Value

        /** The ids of the selected multiple choice options. */
        public data class Choices(val optionIds: List<String>) : Value
    }

    internal companion object {
        private val questionOrder = Comparator<String> { a, b -> naturalCompare(a, b) }

        fun from(surveyId: String, response: SurveyResponse): SurveyCompletion = SurveyCompletion(
            surveyId = surveyId,
            answers = response.results.entries
                .sortedWith(compareBy(questionOrder) { it.key })
                .mapNotNull { (questionId, answer) -> answer.toAnswer(questionId) },
        )

        private fun SurveyAnswer.toAnswer(questionId: String): Answer? = when (this) {
            is SurveyAnswer.NpsRating -> Answer(questionId, QuestionType.SCALE_RATING, Value.Score(score))
            is SurveyAnswer.SymbolRating -> Answer(questionId, QuestionType.SYMBOL_RATING, Value.Score(score))
            is SurveyAnswer.Thumbs -> when (rating) {
                ThumbsRating.THUMBS_UP -> Answer(questionId, QuestionType.THUMBS_RATING, Value.Thumbs(isUp = true))
                ThumbsRating.THUMBS_DOWN -> Answer(questionId, QuestionType.THUMBS_RATING, Value.Thumbs(isUp = false))
                ThumbsRating.NONE -> null
            }
            is SurveyAnswer.Text -> Answer(questionId, QuestionType.TEXTUAL_SURVEY, Value.Text(feedback))
            is SurveyAnswer.MultipleChoice -> Answer(questionId, QuestionType.MULTIPLE_CHOICE, Value.Choices(optionIds))
        }

        /** Compares runs of digits by number, so `q2` sorts before `q10`, like the iOS SDK. */
        private fun naturalCompare(a: String, b: String): Int {
            var i = 0
            var j = 0
            while (i < a.length && j < b.length) {
                if (a[i].isDigit() && b[j].isDigit()) {
                    val startA = i
                    val startB = j
                    while (i < a.length && a[i].isDigit()) i++
                    while (j < b.length && b[j].isDigit()) j++
                    val numA = a.substring(startA, i).trimStart('0')
                    val numB = b.substring(startB, j).trimStart('0')
                    if (numA.length != numB.length) return numA.length - numB.length
                    val cmp = numA.compareTo(numB)
                    if (cmp != 0) return cmp
                } else {
                    val cmp = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
                    if (cmp != 0) return cmp
                    i++
                    j++
                }
            }
            return (a.length - i) - (b.length - j)
        }
    }
}

/** Receives [SurveyCompletion]s. See [WhiskrKit.setOnSurveyCompletedListener]. */
public fun interface SurveyCompletionListener {
    public fun onSurveyCompleted(completion: SurveyCompletion)
}
