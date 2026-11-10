package eu.whiskrkit.core

import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.FullScreenFormTemplate
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.isVisible
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockBrandingTest {

    private val service = MockConfigurationService()

    @Test
    fun `mocked sheets and banners follow the branding switch`() = runTest {
        for (visible in listOf(true, false)) {
            service.brandingVisible = visible
            assertEquals(visible, (service.fetchSurveyTemplate("onboarding-survey") as SheetTemplate).branding.isVisible)
            assertEquals(visible, (service.fetchSurveyTemplate("welcome-toast") as BannerTemplate).branding.isVisible)
        }
    }

    @Test
    fun `branding is off by default and never added to the full screen form`() = runTest {
        assertFalse((service.fetchSurveyTemplate("onboarding-survey") as SheetTemplate).branding.isVisible)
        service.brandingVisible = true
        assertTrue(service.fetchSurveyTemplate("super-survey") is FullScreenFormTemplate)
        assertNull(service.fetchSurveyTemplate("nope"))
    }
}
