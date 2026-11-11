package eu.whiskrkit.core.model

import eu.whiskrkit.core.eligibility.SurveyEligibilityResponse
import eu.whiskrkit.core.serialization.WireJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandingDecodingTest {

    private val question = """{"template":"thumbsRating","id":"q1","isRequired":false}"""

    private fun sheet(branding: String?) = """
        {"template":"sheet","id":"s1","title":"Hi","survey":$question
         ${branding?.let { ""","branding":$it""" } ?: ""}}
    """.trimIndent()

    private fun toast(branding: String?) = """
        {"template":"toast","id":"t1","title":"Hi","survey":$question
         ${branding?.let { ""","branding":$it""" } ?: ""}}
    """.trimIndent()

    private fun branding(template: SurveyTemplate): Branding? = when (template) {
        is SheetTemplate -> template.branding
        is BannerTemplate -> template.branding
        is FullScreenFormTemplate -> null
    }

    private fun decode(json: String) = WireJson.decodeFromString(SurveyTemplate.serializer(), json)

    private fun eligibility(survey: String) = WireJson.decodeFromString(
        SurveyEligibilityResponse.serializer(),
        """{"shouldShow":true,"survey":$survey}""",
    ).survey!!

    private val shapes: Map<String, (String?) -> String> = mapOf("sheet" to ::sheet, "toast" to ::toast)

    private fun assertVisible(expected: Boolean, branding: String?) {
        for ((name, shape) in shapes) {
            val json = shape(branding)
            val direct = decode(json)
            val viaEligibility = eligibility(json)
            assertEquals("$name direct $branding", expected, branding(direct).isVisible)
            assertEquals("$name eligibility $branding", expected, branding(viaEligibility).isVisible)
            // The rest of the survey always survives.
            assertEquals("$name id", direct.id, viaEligibility.id)
            assertEquals("Hi", (direct as? SheetTemplate)?.title ?: (direct as BannerTemplate).title)
            assertTrue(((direct as? SheetTemplate)?.survey ?: (direct as BannerTemplate).survey) is ThumbsSurveyTemplate)
        }
    }

    @Test
    fun `visible true shows the badge`() = assertVisible(true, """{"visible":true}""")

    @Test
    fun `visible false hides the badge`() = assertVisible(false, """{"visible":false}""")

    @Test
    fun `missing branding hides the badge`() = assertVisible(false, null)

    @Test
    fun `null branding hides the badge`() = assertVisible(false, "null")

    @Test
    fun `branding as a string hides the badge`() = assertVisible(false, "\"yes\"")

    @Test
    fun `branding as a number hides the badge`() = assertVisible(false, "1")

    @Test
    fun `branding as an array hides the badge`() = assertVisible(false, "[true]")

    @Test
    fun `non-boolean visible hides the badge`() {
        assertVisible(false, """{"visible":"yes"}""")
        assertVisible(false, """{"visible":"true"}""")
        assertVisible(false, """{"visible":1}""")
        assertVisible(false, """{"visible":null}""")
        assertVisible(false, """{"visible":{"a":true}}""")
        assertVisible(false, "{}")
    }

    @Test
    fun `unknown branding fields are ignored`() = assertVisible(true, """{"visible":true,"variant":"mono"}""")

    @Test
    fun `full screen form ignores branding`() {
        val form = decode(
            """{"template":"fullScreenForm","id":"f1","branding":{"visible":true},"surveys":[$question]}""",
        )
        assertTrue(form is FullScreenFormTemplate)
    }

    @Test
    fun `an absent branding round trips as absent`() {
        val template = decode(sheet(null)) as SheetTemplate
        assertFalse(WireJson.encodeToString(SurveyTemplate.serializer(), template).contains("branding"))
        val shown = decode(sheet("""{"visible":true}""")) as SheetTemplate
        val again = decode(WireJson.encodeToString(SurveyTemplate.serializer(), shown)) as SheetTemplate
        assertEquals(Branding(visible = true), again.branding)
    }
}
