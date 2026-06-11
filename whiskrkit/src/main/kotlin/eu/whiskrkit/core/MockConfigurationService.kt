package eu.whiskrkit.core

import eu.whiskrkit.core.eligibility.EligibilityService
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.MultipleChoiceOption
import eu.whiskrkit.core.model.MultipleChoiceTemplate
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.SurveyResponse
import eu.whiskrkit.core.model.SurveyTemplate
import eu.whiskrkit.core.model.SymbolRatingTemplate
import eu.whiskrkit.core.model.TextSurveyTemplate
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import eu.whiskrkit.internal.WhiskrLog
import kotlinx.coroutines.delay

/**
 * Mock configuration service for testing and development, mirroring the iOS
 * `MockConfigurationService`: predefined templates behind well-known
 * identifiers, a simulated network delay, and log-only submissions.
 */
internal class MockConfigurationService : ConfigurationService {

    private val templates: Map<String, SurveyTemplate> = buildTemplates()

    override fun configure(apiKey: String) {
        // Mock service doesn't need real API configuration.
    }

    override suspend fun retryPendingSubmissions() {
        // Not applicable for the mock.
    }

    override suspend fun fetchSurveyTemplate(identifier: String): SurveyTemplate? {
        delay(500) // Simulate network delay.
        return templates[identifier]
    }

    override suspend fun submitSurveyResponse(surveyId: String, response: SurveyResponse): Boolean {
        WhiskrLog.i(
            WhiskrLog.NETWORKING,
            "Mock: saved surveyId=$surveyId response=${response.results}",
        )
        return true
    }

    private fun buildTemplates(): Map<String, SurveyTemplate> = mapOf(
        // Banner ("toast" on the wire) templates
        "welcome-toast" to BannerTemplate(
            id = "toast-1",
            title = "Welcome to our app!",
            description = "How are you liking it so far?",
            survey = ThumbsSurveyTemplate(id = "toast-1-thumbs", isRequired = false),
        ),
        "quick-feedback" to BannerTemplate(
            id = "toast-2",
            title = "Quick feedback",
            description = "Rate your experience",
            survey = SymbolRatingTemplate(id = "toast-2-symbol", isRequired = false),
        ),
        "simple-toast" to BannerTemplate(
            id = "toast-3",
            title = "Thanks for using our app!",
        ),
        "feedback-toast" to BannerTemplate(
            id = "toast-4",
            title = "Got a minute?",
            description = "We'd love to hear how onboarding went.",
            followUpIdentifier = "onboarding-survey",
        ),

        // Sheet templates
        "onboarding-survey" to SheetTemplate(
            id = "sheet-1",
            title = "Welcome aboard!",
            description = "Help us understand your needs better",
            followUpQuestion = "What could we do better?",
            survey = ScaleRatingTemplate(
                id = "sheet-1-scale",
                title = "How was your onboarding experience?",
                ratingRange = ScaleRatingTemplate.RatingRange(min = 1, max = 7),
                isRequired = true,
            ),
        ),
        "nps-survey" to SheetTemplate(
            id = "sheet-2",
            title = "Quick question",
            survey = ScaleRatingTemplate(
                id = "sheet-2-nps",
                title = "How likely are you to recommend us?",
                subtitle = "Your feedback helps us improve",
                ratingRange = ScaleRatingTemplate.RatingRange(min = 0, max = 10),
                isRequired = true,
            ),
        ),
        "text-feedback" to SheetTemplate(
            id = "sheet-3",
            title = "Give feedback",
            survey = TextSurveyTemplate(
                id = "sheet-3-text",
                title = "What's on your mind?",
                description = "Anything you'd like to tell us.",
                isRequired = true,
            ),
        ),
        "choice-survey" to SheetTemplate(
            id = "sheet-4",
            title = "About you",
            survey = MultipleChoiceTemplate(
                id = "sheet-4-choice",
                title = "How long have you been using the app?",
                isRequired = true,
                options = listOf(
                    MultipleChoiceOption("opt-1", "Less than a year"),
                    MultipleChoiceOption("opt-2", "1–3 years"),
                    MultipleChoiceOption("opt-3", "3+ years"),
                ),
                allowsMultiSelection = false,
            ),
        ),
        "multi-choice-survey" to SheetTemplate(
            id = "sheet-5",
            title = "Your interests",
            survey = MultipleChoiceTemplate(
                id = "sheet-5-choice",
                title = "Which features do you use?",
                subtitle = "Select all that apply",
                isRequired = false,
                options = listOf(
                    MultipleChoiceOption("opt-1", "Surveys"),
                    MultipleChoiceOption("opt-2", "Analytics"),
                    MultipleChoiceOption("opt-3", "Feedback forms"),
                ),
                allowsMultiSelection = true,
            ),
        ),

        // Full-screen form
        "full-survey" to FullScreenFormTemplate(
            id = "form-1",
            title = "Ticket purchase",
            subtitle = "How did buying your ticket go?",
            description = "Thank you for taking some time to help us improve our product.",
            surveys = listOf(
                ScaleRatingTemplate(
                    id = "form-1-scale",
                    title = "How likely are you to recommend us?",
                    subtitle = "Your feedback helps us improve",
                    ratingRange = ScaleRatingTemplate.RatingRange(min = 1, max = 7),
                    isRequired = true,
                ),
                TextSurveyTemplate(
                    id = "form-1-good",
                    title = "What went well?",
                    isRequired = false,
                ),
                MultipleChoiceTemplate(
                    id = "form-1-choice",
                    title = "What did you buy?",
                    isRequired = true,
                    options = listOf(
                        MultipleChoiceOption("opt-1", "Single ticket"),
                        MultipleChoiceOption("opt-2", "Day pass"),
                        MultipleChoiceOption("opt-3", "Season pass"),
                    ),
                    allowsMultiSelection = false,
                ),
                ThumbsSurveyTemplate(
                    id = "form-1-thumbs",
                    title = "Would you do it again?",
                    isRequired = false,
                ),
            ),
        ),
    )
}

/** Mock eligibility: always eligible, returns the template directly (parity, P6). */
internal class MockEligibilityService(
    private val configurationService: ConfigurationService,
) : EligibilityService {
    override suspend fun checkEligibility(surveyId: String): SurveyTemplate? =
        configurationService.fetchSurveyTemplate(surveyId)
}
