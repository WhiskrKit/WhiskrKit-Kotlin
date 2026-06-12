package eu.whiskrkit.core.model

import eu.whiskrkit.core.eligibility.SurveyEligibilityResponse
import eu.whiskrkit.core.serialization.WireJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SurveyTemplateDecodingTest {

    private fun decode(json: String): SurveyTemplate =
        WireJson.decodeFromString(SurveyTemplate.serializer(), json)

    @Test
    fun `decodes sheet with scale rating and exact a11y key casing`() {
        val template = decode(
            """
            {"template":"sheet","id":"s1","title":"Quick feedback",
             "description":"We'd love to hear from you","followUpQuestion":"Tell us more",
             "survey":{"template":"scaleRating","id":"q1","title":"Recommend us?",
                       "subtitle":"Helps us improve","ratingRange":{"min":1,"max":7},
                       "isRequired":true,"A11yLabel":"Rating","A11yHint":"1 to 7"}}
            """.trimIndent(),
        )
        val sheet = template as SheetTemplate
        assertEquals("s1", sheet.id)
        assertEquals("Tell us more", sheet.followUpQuestion)
        val question = sheet.survey as ScaleRatingTemplate
        assertEquals(1, question.ratingRange.min)
        assertEquals(7, question.ratingRange.max)
        assertEquals("Rating", question.a11yLabel)
        assertEquals("1 to 7", question.a11yHint)
        assertTrue(question.isRequired)
    }

    @Test
    fun `decodes text question with lowercase a11yHint wire key`() {
        val template = decode(
            """
            {"template":"sheet","id":"s1",
             "survey":{"template":"textualSurvey","id":"q1","maxLength":300,
                       "isRequired":false,"A11yLabel":"Label","a11yHint":"Hint"}}
            """.trimIndent(),
        )
        val question = (template as SheetTemplate).survey as TextSurveyTemplate
        assertEquals(300, question.characterLimit)
        assertEquals("Hint", question.a11yHint)
    }

    @Test
    fun `decodes full screen form with multiple question types`() {
        val template = decode(
            """
            {"template":"fullScreenForm","id":"f1","title":"Survey","subtitle":"Sub",
             "description":"Desc","surveys":[
               {"template":"thumbsRating","id":"q1","isRequired":true},
               {"template":"symbolRating","id":"q2","opensStoreReview":false,"isRequired":false},
               {"template":"multipleChoice","id":"q3","isRequired":true,
                "options":[{"id":"a","label":"A"},{"id":"b","label":"B"}],
                "allowsMultiSelection":true}]}
            """.trimIndent(),
        )
        val form = template as FullScreenFormTemplate
        assertEquals(3, form.surveys.size)
        assertTrue(form.surveys[0] is ThumbsSurveyTemplate)
        assertTrue(form.surveys[1] is SymbolRatingTemplate)
        val choice = form.surveys[2] as MultipleChoiceTemplate
        assertTrue(choice.allowsMultiSelection)
        assertEquals(listOf("a", "b"), choice.options.map { it.id })
    }

    @Test
    fun `decodes banner with toast wire value`() {
        val template = decode(
            """{"template":"toast","id":"t1","title":"Hi","followUpIdentifier":"next"}""",
        )
        val banner = template as BannerTemplate
        assertEquals("next", banner.followUpIdentifier)
        assertNull(banner.survey)
    }

    @Test
    fun `unknown presentation template fails decoding`() {
        assertThrows(SerializationException::class.java) {
            decode("""{"template":"hologram","id":"x1"}""")
        }
    }

    @Test
    fun `template round-trips through saver wire form`() {
        val original = decode(
            """
            {"template":"sheet","id":"s1","title":"T",
             "survey":{"template":"thumbsRating","id":"q1","isRequired":true}}
            """.trimIndent(),
        )
        val encoded = WireJson.encodeToString(SheetTemplate.serializer(), original as SheetTemplate)
        assertEquals(original, decode(encoded))
    }

    // region Lenient decoding of unknown question types

    @Test
    fun `unknown optional question is dropped from full screen form`() {
        val template = decode(
            """
            {"template":"fullScreenForm","id":"f1","surveys":[
               {"template":"emojiRating","id":"q1","isRequired":false},
               {"template":"thumbsRating","id":"q2","isRequired":true}]}
            """.trimIndent(),
        ).validated()
        val form = template as FullScreenFormTemplate
        assertEquals(listOf("q2"), form.surveys.map { it.id })
    }

    @Test
    fun `unknown required question invalidates full screen form`() {
        val template = decode(
            """
            {"template":"fullScreenForm","id":"f1","surveys":[
               {"template":"emojiRating","id":"q1","isRequired":true},
               {"template":"thumbsRating","id":"q2","isRequired":true}]}
            """.trimIndent(),
        ).validated()
        assertNull(template)
    }

    @Test
    fun `form with only unknown questions is invalid`() {
        val template = decode(
            """
            {"template":"fullScreenForm","id":"f1","surveys":[
               {"template":"emojiRating","id":"q1","isRequired":false}]}
            """.trimIndent(),
        ).validated()
        assertNull(template)
    }

    @Test
    fun `sheet with unknown question is invalid`() {
        val template = decode(
            """
            {"template":"sheet","id":"s1",
             "survey":{"template":"emojiRating","id":"q1","isRequired":false}}
            """.trimIndent(),
        ).validated()
        assertNull(template)
    }

    @Test
    fun `banner without survey is valid`() {
        val template = decode("""{"template":"toast","id":"t1"}""").validated()
        assertTrue(template is BannerTemplate)
    }

    // endregion

    @Test
    fun `eligibility response decodes iso dates and embedded survey`() {
        val response = WireJson.decodeFromString(
            SurveyEligibilityResponse.serializer(),
            """
            {"shouldShow":true,"nextCheckAfter":"2026-07-01T10:00:00Z",
             "survey":{"template":"toast","id":"t1","title":"Hello"}}
            """.trimIndent(),
        )
        assertTrue(response.shouldShow)
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"), response.nextCheckAfter)
        assertTrue(response.survey is BannerTemplate)
        assertNull(response.removeFromHistory)
    }
}
