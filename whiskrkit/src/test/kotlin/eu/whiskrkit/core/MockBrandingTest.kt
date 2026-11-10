package eu.whiskrkit.core

import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.isVisible
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockBrandingTest {

    private val service = MockConfigurationService()

    @Test
    fun `mocked sheets and banners always show the badge`() = runTest {
        for (id in listOf("onboarding-survey", "choice-survey", "feature-feedback")) {
            assertTrue(id, (service.fetchSurveyTemplate(id) as SheetTemplate).branding.isVisible)
        }
        for (id in listOf("welcome-toast", "quick-feedback")) {
            assertTrue(id, (service.fetchSurveyTemplate(id) as BannerTemplate).branding.isVisible)
        }
    }

    @Test
    fun `the full screen form carries no branding`() = runTest {
        assertTrue(service.fetchSurveyTemplate("super-survey") is FullScreenFormTemplate)
        assertNull(service.fetchSurveyTemplate("nope"))
    }
}
