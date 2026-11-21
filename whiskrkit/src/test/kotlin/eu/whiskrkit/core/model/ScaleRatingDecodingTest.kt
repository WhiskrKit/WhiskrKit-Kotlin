package eu.whiskrkit.core.model

import eu.whiskrkit.core.serialization.WireJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScaleRatingDecodingTest {

    private fun decode(fields: String): ScaleRatingTemplate {
        val json = """
            {"template":"sheet","id":"s1",
             "survey":{"template":"scaleRating","id":"q_123",
                       "title":"How likely are you to recommend Stride to a friend?",
                       "subtitle":null,"isRequired":true,$fields}}
        """.trimIndent()
        val sheet = WireJson.decodeFromString(SurveyTemplate.serializer(), json) as SheetTemplate
        return sheet.survey as ScaleRatingTemplate
    }

    @Test
    fun `a payload from an older server decodes with defaults`() {
        val template = decode(""""ratingRange":{"min":1,"max":7}""")
        assertEquals(ScaleRatingTemplate.RatingRange(1, 7), template.ratingRange)
        assertEquals(RatingMetric.CUSTOM, template.metric)
        assertEquals(RatingDisplayStyle.NUMBERS, template.displayStyle)
        assertNull(template.minLabel)
        assertNull(template.maxLabel)
    }

    @Test
    fun `the wire contract example decodes`() {
        val template = decode(
            """"ratingRange":{"min":0,"max":10},"metric":"nps","displayStyle":"numbers","minLabel":null,"maxLabel":null""",
        )
        assertEquals(RatingMetric.NPS, template.metric)
        assertEquals(RatingDisplayStyle.NUMBERS, template.resolvedDisplayStyle)
    }

    @Test
    fun `a full new payload decodes every field`() {
        val template = decode(
            """"ratingRange":{"min":1,"max":5},"metric":"csat","displayStyle":"stars","minLabel":"Meh","maxLabel":"Wow"""",
        )
        assertEquals(RatingMetric.CSAT, template.metric)
        assertEquals(RatingDisplayStyle.STARS, template.displayStyle)
        assertEquals(RatingDisplayStyle.STARS, template.resolvedDisplayStyle)
        assertEquals("Meh", template.minLabel)
        assertEquals("Wow", template.maxLabel)
    }

    @Test
    fun `every metric decodes`() {
        for (metric in RatingMetric.entries) {
            assertEquals(metric, decode(""""ratingRange":{"min":0,"max":10},"metric":"${metric.wireName}"""").metric)
        }
    }

    @Test
    fun `an unknown metric falls back to custom`() {
        assertEquals(RatingMetric.CUSTOM, decode(""""ratingRange":{"min":0,"max":10},"metric":"ltr"""").metric)
    }

    @Test
    fun `an unknown display style falls back to numbers`() {
        val template = decode(""""ratingRange":{"min":1,"max":5},"displayStyle":"emoji"""")
        assertEquals(RatingDisplayStyle.NUMBERS, template.displayStyle)
        assertEquals(RatingDisplayStyle.NUMBERS, template.resolvedDisplayStyle)
    }

    @Test
    fun `null or wrongly typed values fall back instead of failing the survey`() {
        val nulls = decode(""""ratingRange":{"min":1,"max":5},"metric":null,"displayStyle":null""")
        assertEquals(RatingMetric.CUSTOM, nulls.metric)
        assertEquals(RatingDisplayStyle.NUMBERS, nulls.displayStyle)
        val wrongTypes = decode(""""ratingRange":{"min":1,"max":5},"metric":3,"displayStyle":true""")
        assertEquals(RatingMetric.CUSTOM, wrongTypes.metric)
        assertEquals(RatingDisplayStyle.NUMBERS, wrongTypes.displayStyle)
    }

    @Test
    fun `stars with a range other than 1 to 5 render as numbers`() {
        for ((min, max) in listOf(0 to 5, 1 to 7, 0 to 10, 2 to 5)) {
            val template = decode(""""ratingRange":{"min":$min,"max":$max},"displayStyle":"stars"""")
            assertEquals(RatingDisplayStyle.STARS, template.displayStyle)
            assertEquals("$min to $max", RatingDisplayStyle.NUMBERS, template.resolvedDisplayStyle)
        }
    }

    @Test
    fun `a template round-trips through the wire format`() {
        val original = ScaleRatingTemplate(
            id = "q", ratingRange = ScaleRatingTemplate.RatingRange(1, 5), isRequired = true,
            metric = RatingMetric.CES, displayStyle = RatingDisplayStyle.STARS, minLabel = "a", maxLabel = "b",
        )
        val json = WireJson.encodeToString(ScaleRatingTemplate.serializer(), original)
        assertEquals(original, WireJson.decodeFromString(ScaleRatingTemplate.serializer(), json))
    }
}
