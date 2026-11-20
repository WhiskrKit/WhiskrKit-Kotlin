package eu.whiskrkit.ui.question

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.whiskrkit.core.model.RatingMetric
import eu.whiskrkit.core.model.ScaleRatingTemplate
import eu.whiskrkit.core.model.ScaleRatingTemplate.RatingRange
import eu.whiskrkit.ui.question.ScaleRatingPalette.Swatch
import eu.whiskrkit.ui.withLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.pow

/** Colors, row layout and end labels of scale ratings ([ScaleRatingStyle.kt]). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScaleRatingStyleTest {

    private val nps = RatingRange(0, 10)
    private val csat = RatingRange(1, 5)
    private val ces = RatingRange(1, 7)

    // MARK: - Colors

    @Test
    fun `NPS is red to 6, gold for 7 and 8, green for 9 and 10`() {
        val swatches = (0..10).map { ScaleRatingPalette.swatch(it, RatingMetric.NPS, nps) }
        assertEquals(List(7) { Swatch.RED } + listOf(Swatch.GOLD, Swatch.GOLD, Swatch.GREEN, Swatch.GREEN), swatches)
        assertEquals(Swatch.RED, ScaleRatingPalette.swatch(6, RatingMetric.NPS, nps))
        assertEquals(Swatch.GOLD, ScaleRatingPalette.swatch(7, RatingMetric.NPS, nps))
        assertEquals(Swatch.GOLD, ScaleRatingPalette.swatch(8, RatingMetric.NPS, nps))
        assertEquals(Swatch.GREEN, ScaleRatingPalette.swatch(9, RatingMetric.NPS, nps))
    }

    @Test
    fun `CSAT is neutral to 3, green for 4 and 5`() {
        val swatches = (1..5).map { ScaleRatingPalette.swatch(it, RatingMetric.CSAT, csat) }
        assertEquals(listOf(Swatch.NEUTRAL, Swatch.NEUTRAL, Swatch.NEUTRAL, Swatch.GREEN, Swatch.GREEN), swatches)
    }

    @Test
    fun `CES and custom are neutral for every score`() {
        for (metric in listOf(RatingMetric.CES, RatingMetric.CUSTOM)) {
            for (range in listOf(ces, nps, csat)) {
                for (score in range.values) {
                    assertEquals("$metric $score", Swatch.NEUTRAL, ScaleRatingPalette.swatch(score, metric, range))
                }
            }
        }
    }

    @Test
    fun `a preset with a non-standard range falls back to neutral`() {
        assertEquals(Swatch.NEUTRAL, ScaleRatingPalette.swatch(10, RatingMetric.NPS, RatingRange(1, 10)))
        assertEquals(Swatch.NEUTRAL, ScaleRatingPalette.swatch(0, RatingMetric.NPS, RatingRange(0, 5)))
        assertEquals(Swatch.NEUTRAL, ScaleRatingPalette.swatch(5, RatingMetric.CSAT, RatingRange(1, 7)))
    }

    @Test
    fun `every swatch reaches WCAG contrast with black text and against white and black`() {
        for (swatch in Swatch.entries) {
            val fill = luminance(swatch.rgb)
            assertTrue("$swatch with black text", contrast(fill, 0.0) >= 4.5)
            assertTrue("$swatch on white", contrast(fill, 1.0) >= 3.0)
            assertTrue("$swatch on black", contrast(fill, 0.0) >= 3.0)
        }
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((rgb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun contrast(a: Double, b: Double) = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)

    // MARK: - Layout

    @Test
    fun `rows depend on the number of options, not on the upper bound`() {
        assertEquals(listOf(5), ScaleRatingLayout.rows(RatingRange(1, 5)).map { it.size })
        assertEquals(listOf(6), ScaleRatingLayout.rows(RatingRange(0, 5)).map { it.size })
        assertEquals(listOf(4, 3), ScaleRatingLayout.rows(RatingRange(1, 7)).map { it.size })
        assertEquals(listOf(6, 5), ScaleRatingLayout.rows(RatingRange(0, 10)).map { it.size })
        assertEquals(listOf((0..5).toList(), (6..10).toList()), ScaleRatingLayout.rows(RatingRange(0, 10)))
    }

    @Test
    fun `large text wraps to more, evenly filled rows`() {
        assertEquals(listOf(4, 4, 3), ScaleRatingLayout.rowSizes(11, maxPerRow = 4))
        assertEquals(listOf(3, 3, 3, 2), ScaleRatingLayout.rowSizes(11, maxPerRow = 3))
        assertEquals(listOf(2, 2, 1), ScaleRatingLayout.rowSizes(5, maxPerRow = 2))
        assertEquals(emptyList<Int>(), ScaleRatingLayout.rowSizes(0))
    }

    @Test
    fun `buttons per row follow the available width`() {
        // A 360dp phone with 16dp padding on both sides: six 48dp buttons fit.
        assertEquals(6, ScaleRatingLayout.buttonsPerRow(328.dp, 48.dp))
        // A 320dp phone: five fit.
        assertEquals(5, ScaleRatingLayout.buttonsPerRow(288.dp, 48.dp))
        // Font scale 2: 96dp buttons.
        assertEquals(3, ScaleRatingLayout.buttonsPerRow(328.dp, 96.dp))
        assertEquals(1, ScaleRatingLayout.buttonsPerRow(50.dp, 96.dp))
    }

    // MARK: - Labels

    private fun resources(language: String = "en") =
        RuntimeEnvironment.getApplication().withLanguage(language).resources

    private fun template(
        metric: RatingMetric,
        range: RatingRange = nps,
        minLabel: String? = null,
        maxLabel: String? = null,
    ) = ScaleRatingTemplate(
        id = "q", ratingRange = range, isRequired = false, metric = metric, minLabel = minLabel, maxLabel = maxLabel,
    )

    @Test
    fun `preset metrics use WhiskrKit's English labels`() {
        assertEquals("Not at all likely" to "Extremely likely", ScaleRatingLabels.endLabels(template(RatingMetric.NPS), resources()))
        assertEquals("Very dissatisfied" to "Very satisfied", ScaleRatingLabels.endLabels(template(RatingMetric.CSAT), resources()))
        assertEquals("Very difficult" to "Very easy", ScaleRatingLabels.endLabels(template(RatingMetric.CES), resources()))
    }

    @Test
    fun `preset labels follow the survey language`() {
        assertEquals("Zeer ontevreden" to "Zeer tevreden", ScaleRatingLabels.endLabels(template(RatingMetric.CSAT), resources("nl")))
        assertEquals("Sehr schwierig" to "Sehr einfach", ScaleRatingLabels.endLabels(template(RatingMetric.CES), resources("de")))
        assertEquals("Nada provável" to "Extremamente provável", ScaleRatingLabels.endLabels(template(RatingMetric.NPS), resources("pt")))
    }

    @Test
    fun `preset metrics ignore labels from the server`() {
        val labels = ScaleRatingLabels.endLabels(template(RatingMetric.NPS, minLabel = "Nope", maxLabel = "Yes!"), resources())
        assertEquals("Not at all likely" to "Extremely likely", labels)
    }

    @Test
    fun `custom uses the server's labels`() {
        val labels = ScaleRatingLabels.endLabels(template(RatingMetric.CUSTOM, minLabel = "Too easy", maxLabel = "Too hard"), resources())
        assertEquals("Too easy" to "Too hard", labels)
    }

    @Test
    fun `custom without labels shows nothing`() {
        assertEquals(null to null, ScaleRatingLabels.endLabels(template(RatingMetric.CUSTOM), resources()))
        assertEquals(null to null, ScaleRatingLabels.endLabels(template(RatingMetric.CUSTOM, minLabel = "  ", maxLabel = ""), resources()))
        assertNull(ScaleRatingLabels.endsDescription(template(RatingMetric.CUSTOM), resources()))
    }

    @Test
    fun `TalkBack hears the position and what the ends mean`() {
        assertEquals(
            "8 out of 10. 0 is Not at all likely, 10 is Extremely likely",
            ScaleRatingLabels.buttonDescription(8, template(RatingMetric.NPS), resources()),
        )
        assertEquals(
            "3 out of 5. 5 is Packed",
            ScaleRatingLabels.buttonDescription(3, template(RatingMetric.CUSTOM, csat, maxLabel = "Packed"), resources()),
        )
        assertEquals("3 out of 5", ScaleRatingLabels.buttonDescription(3, template(RatingMetric.CUSTOM, csat), resources()))
    }

    @Test
    fun `a description built from resolved ends matches the one-call form`() {
        val nps = template(RatingMetric.NPS)
        val ends = ScaleRatingLabels.endsDescription(nps, resources())
        for (score in 0..10) {
            assertEquals(
                ScaleRatingLabels.buttonDescription(score, nps, resources()),
                ScaleRatingLabels.buttonDescription(score, nps, resources(), ends),
            )
        }
    }

    @Test
    fun `an unlimited width allows the maximum per row`() {
        assertEquals(
            ScaleRatingLayout.MAX_BUTTONS_PER_ROW,
            ScaleRatingLayout.buttonsPerRow(Dp.Infinity, ScaleRatingLayout.MinimumButtonSize),
        )
    }
}
