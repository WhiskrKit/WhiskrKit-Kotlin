package eu.whiskrkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.MultipleChoiceOption
import eu.whiskrkit.core.model.MultipleChoiceTemplate
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyAnswer
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.core.model.TextSurveyTemplate
import eu.whiskrkit.core.model.ThumbsRating
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.resolveWhiskrTheme
import eu.whiskrkit.ui.container.BannerContent
import eu.whiskrkit.ui.container.FullScreenContent
import eu.whiskrkit.ui.container.SheetContent
import eu.whiskrkit.ui.question.ScaleRatingQuestion
import eu.whiskrkit.ui.question.SymbolRatingQuestion
import eu.whiskrkit.ui.question.TextFeedbackQuestion
import eu.whiskrkit.ui.question.ThumbsRatingQuestion
import eu.whiskrkit.ui.question.MultipleChoiceQuestion
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi reference renders covering every question type and presentation
 * container, light and dark. Run `./gradlew recordRoborazziDebug` to update
 * golden images, `verifyRoborazziDebug` in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-normal-long-notround-any-440dpi-keyshidden-nonav")
class SnapshotTest {

    private fun snap(dark: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalWhiskrKitTheme provides resolveWhiskrTheme(null)) {
                    Surface {
                        Box(modifier = Modifier.padding(16.dp)) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun scaleRating_selected() = snap {
        ScaleRatingQuestion(
            template = ScaleRatingTemplate(
                id = "q1",
                title = "How likely are you to recommend us?",
                subtitle = "Your feedback helps us improve",
                ratingRange = ScaleRatingTemplate.RatingRange(0, 10),
                isRequired = true,
            ),
            answer = SurveyAnswer.NpsRating(7),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun scaleRating_error() = snap {
        ScaleRatingQuestion(
            template = ScaleRatingTemplate(
                id = "q1",
                title = "Rate our cookies",
                ratingRange = ScaleRatingTemplate.RatingRange(1, 5),
                isRequired = true,
            ),
            answer = null,
            onAnswer = {},
            showError = true,
        )
    }

    @Test
    fun symbolRating() = snap {
        SymbolRatingQuestion(
            template = SymbolRatingTemplate(
                id = "q1",
                title = "Ticket purchase",
                description = "How well did buying your ticket go?",
                isRequired = false,
            ),
            answer = SurveyAnswer.SymbolRating(3),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun thumbsRating_selected() = snap {
        ThumbsRatingQuestion(
            template = ThumbsSurveyTemplate(
                id = "q1",
                title = "Rollercoaster",
                subtitle = "Did you enjoy the ride?",
                isRequired = false,
            ),
            answer = SurveyAnswer.Thumbs(ThumbsRating.THUMBS_UP),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun textFeedback() = snap {
        TextFeedbackQuestion(
            template = TextSurveyTemplate(
                id = "q1",
                title = "Give feedback",
                description = "What do you want to say?",
                isRequired = true,
            ),
            answer = SurveyAnswer.Text("The new planner is really handy on the go."),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun multipleChoice_single() = snap {
        MultipleChoiceQuestion(
            template = MultipleChoiceTemplate(
                id = "q1",
                title = "How long have you been using the app?",
                isRequired = true,
                options = listOf(
                    MultipleChoiceOption("1", "Less than a year"),
                    MultipleChoiceOption("2", "1–3 years"),
                    MultipleChoiceOption("3", "3+ years"),
                ),
                allowsMultiSelection = false,
            ),
            answer = SurveyAnswer.MultipleChoice(listOf("2")),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun multipleChoice_multi() = snap {
        MultipleChoiceQuestion(
            template = MultipleChoiceTemplate(
                id = "q1",
                title = "Which features do you use?",
                subtitle = "Select all that apply",
                isRequired = false,
                options = listOf(
                    MultipleChoiceOption("1", "Surveys"),
                    MultipleChoiceOption("2", "Analytics"),
                    MultipleChoiceOption("3", "Feedback forms"),
                ),
                allowsMultiSelection = true,
            ),
            answer = SurveyAnswer.MultipleChoice(listOf("1", "3")),
            onAnswer = {},
            showError = false,
        )
    }

    @Test
    fun sheetContent() = snap {
        SheetContent(
            template = SheetTemplate(
                id = "s1",
                title = "Quick feedback",
                description = "We'd love to hear from you",
                followUpQuestion = "Tell us more about your experience",
                survey = ScaleRatingTemplate(
                    id = "q1",
                    title = "How likely are you to recommend us?",
                    ratingRange = ScaleRatingTemplate.RatingRange(1, 7),
                    isRequired = true,
                ),
            ),
            onClose = {},
            onSubmitted = {},
        )
    }

    @Test
    fun sheetContent_dark() = snap(dark = true) {
        SheetContent(
            template = SheetTemplate(
                id = "s1",
                title = "Quick feedback",
                description = "We'd love to hear from you",
                survey = ThumbsSurveyTemplate(id = "q1", isRequired = false),
            ),
            onClose = {},
            onSubmitted = {},
        )
    }

    @Test
    fun fullScreenContent() = snap {
        FullScreenContent(
            template = FullScreenFormTemplate(
                id = "f1",
                title = "Ticket purchase",
                subtitle = "How did buying your ticket go?",
                description = "Thank you for taking some time to help us improve.",
                surveys = listOf(
                    ScaleRatingTemplate(
                        id = "q1",
                        title = "How likely are you to recommend us?",
                        ratingRange = ScaleRatingTemplate.RatingRange(1, 7),
                        isRequired = true,
                    ),
                    TextSurveyTemplate(id = "q2", title = "What went well?", isRequired = false),
                ),
            ),
            onClose = {},
            onSubmitted = {},
        )
    }

    @Test
    fun banner_inlineQuestion() = snap {
        BannerContent(
            template = BannerTemplate(
                id = "t1",
                title = "Planning your trip",
                description = "How satisfied are you with our new planner?",
                survey = ThumbsSurveyTemplate(id = "q1", isRequired = false),
            ),
            onDismissRequested = {},
            onOpenFollowUp = {},
        )
    }

    @Test
    fun banner_followUpButtons() = snap {
        BannerContent(
            template = BannerTemplate(
                id = "t1",
                title = "Got a minute?",
                description = "We'd love to hear how onboarding went.",
                followUpIdentifier = "next",
            ),
            onDismissRequested = {},
            onOpenFollowUp = {},
        )
    }

    @Test
    fun banner_dark() = snap(dark = true) {
        BannerContent(
            template = BannerTemplate(
                id = "t1",
                title = "Got a minute?",
                description = "We'd love to hear how onboarding went.",
                followUpIdentifier = "next",
            ),
            onDismissRequested = {},
            onOpenFollowUp = {},
        )
    }

    @Test
    fun sheetContent_fillWidth() = snap {
        Box(modifier = Modifier.fillMaxWidth()) {
            SheetContent(
                template = SheetTemplate(
                    id = "s1",
                    title = "About you",
                    survey = MultipleChoiceTemplate(
                        id = "q1",
                        title = "How long have you been using the app?",
                        isRequired = true,
                        options = listOf(
                            MultipleChoiceOption("1", "Less than a year"),
                            MultipleChoiceOption("2", "1–3 years"),
                        ),
                        allowsMultiSelection = false,
                    ),
                ),
                onClose = {},
                onSubmitted = {},
            )
        }
    }
}
