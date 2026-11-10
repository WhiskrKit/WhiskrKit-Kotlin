package eu.whiskrkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import eu.whiskrkit.core.model.BannerTemplate
import eu.whiskrkit.core.model.Branding
import eu.whiskrkit.core.model.SheetTemplate
import eu.whiskrkit.core.model.ThumbsSurveyTemplate
import eu.whiskrkit.theme.LocalWhiskrKitTheme
import eu.whiskrkit.theme.resolveWhiskrTheme
import eu.whiskrkit.ui.container.BannerContent
import eu.whiskrkit.ui.container.SheetContent
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PoweredByBadgeTest {

    @get:Rule
    val rule = createComposeRule()

    private val question = ThumbsSurveyTemplate(id = "q1", isRequired = false)

    private fun sheet(branding: Branding?) = SheetTemplate(id = "s", title = "Hi", survey = question, branding = branding)
    private fun banner(branding: Branding?, followUpOnly: Boolean = false) = BannerTemplate(
        id = "t",
        title = "Hi",
        survey = question.takeUnless { followUpOnly },
        followUpIdentifier = "next".takeIf { followUpOnly },
        branding = branding,
    )

    private fun show(language: String? = null, content: @Composable () -> Unit) = rule.setContent {
        MaterialTheme(colorScheme = lightColorScheme()) {
            CompositionLocalProvider(LocalWhiskrKitTheme provides resolveWhiskrTheme(null)) {
                SurveyLanguage(language, content)
            }
        }
    }

    private fun sheetContent(template: SheetTemplate): @Composable () -> Unit =
        { SheetContent(template, onClose = {}, onSubmitted = {}) }

    private fun bannerContent(template: BannerTemplate): @Composable () -> Unit =
        { BannerContent(template, onDismissRequested = {}, onSubmitted = {}, onOpenFollowUp = {}) }

    private val badge = "Powered by WhiskrKit"

    @Test
    fun `sheet shows the badge when branding is visible`() {
        show(content = sheetContent(sheet(Branding(visible = true))))
        rule.onNodeWithContentDescription(badge).assertIsDisplayed()
    }

    @Test
    fun `banner shows the badge when branding is visible`() {
        show(content = bannerContent(banner(Branding(visible = true))))
        rule.onNodeWithContentDescription(badge).assertIsDisplayed()
    }

    @Test
    fun `no badge without branding`() {
        show(content = sheetContent(sheet(null)))
        rule.onNodeWithContentDescription(badge).assertDoesNotExist()
    }

    @Test
    fun `no badge when visible is false`() {
        show(content = bannerContent(banner(Branding(visible = false))))
        rule.onNodeWithContentDescription(badge).assertDoesNotExist()
    }

    @Test
    fun `banner without a submit button has no badge`() {
        show(content = bannerContent(banner(Branding(visible = true), followUpOnly = true)))
        rule.onNodeWithContentDescription(badge).assertDoesNotExist()
    }

    @Test
    fun `badge is one element that is not a button and has no click action`() {
        show(content = sheetContent(sheet(Branding(visible = true))))
        val node = rule.onNodeWithContentDescription(badge).fetchSemanticsNode()
        assertNull(node.config.getOrNull(SemanticsProperties.Role))
        assertNull(node.config.getOrNull(SemanticsActions.OnClick))
        // Logo and text are merged away: nothing inside is exposed on its own.
        assertTrue(node.children.isEmpty())
        // Read after the submit button.
        val submit = rule.onNodeWithText("Submit").fetchSemanticsNode()
        assertTrue(node.positionInRoot.y > submit.positionInRoot.y)
    }

    @Test
    fun `badge follows the survey language`() {
        show(language = "nl", content = sheetContent(sheet(Branding(visible = true))))
        rule.onNodeWithContentDescription("Mogelijk gemaakt door WhiskrKit").assertContentDescriptionEquals(
            "Mogelijk gemaakt door WhiskrKit",
        )
    }
}
