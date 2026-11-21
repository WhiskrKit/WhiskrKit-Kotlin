package eu.whiskrkit.ui.question

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.core.model.RatingMetric
import eu.whiskrkit.core.model.ScaleRatingTemplate
import kotlin.math.ceil

// Pure helpers behind ScaleRatingQuestion, kept out of the composable so they can
// be unit tested. The iOS SDK mirrors them in ScaleRatingStyle.swift.

/**
 * Colors for a selected score button.
 *
 * Answer controls are deliberately not themeable, so customers can't pick colors
 * that fail contrast. Every swatch has a relative luminance between 0.18 and 0.30:
 * dark text on it stays above 4.5:1, and the fill stays above 3:1 against both a
 * white and a black background. The same colors are used in light and dark mode.
 */
internal object ScaleRatingPalette {
    enum class Swatch(val rgb: Int) {
        RED(0xE5534B),
        GOLD(0xB8860B),
        GREEN(0x2DA44E),
        NEUTRAL(0x3B82F6),
        ;

        val color: Color get() = Color(0xFF000000.toInt() or rgb)
    }

    /** Text on a selected button. Dark on every swatch, in light and dark mode. */
    val SelectedText: Color = Color.Black

    /** Outline around a selected button, so selection never relies on color alone. */
    val SelectedOutlineWidth: Dp = 2.dp

    /**
     * The swatch for a selected score.
     *
     * NPS (0 to 10): 0–6 red, 7–8 gold, 9–10 green. CSAT (1 to 5): 4–5 green,
     * 1–3 neutral. CES and custom: neutral. A preset that arrives with a range
     * other than its standard one is shown neutral too.
     */
    fun swatch(score: Int, metric: RatingMetric, range: ScaleRatingTemplate.RatingRange): Swatch = when {
        metric == RatingMetric.NPS && range == ScaleRatingTemplate.RatingRange(0, 10) -> when {
            score <= 6 -> Swatch.RED
            score <= 8 -> Swatch.GOLD
            else -> Swatch.GREEN
        }
        metric == RatingMetric.CSAT && range == ScaleRatingTemplate.RatingRange(1, 5) ->
            if (score >= 4) Swatch.GREEN else Swatch.NEUTRAL
        else -> Swatch.NEUTRAL
    }
}

internal object ScaleRatingLayout {
    /** Most buttons in one row; 6 keeps 0 to 10 at two rows on a 360dp phone. */
    const val MAX_BUTTONS_PER_ROW: Int = 6
    val ButtonSpacing: Dp = 8.dp
    val MinimumButtonSize: Dp = 48.dp

    /** How many buttons fit next to each other in [width], capped at [MAX_BUTTONS_PER_ROW]. */
    fun buttonsPerRow(width: Dp, buttonSize: Dp, spacing: Dp = ButtonSpacing): Int {
        if (width <= 0.dp) return MAX_BUTTONS_PER_ROW
        val fitting = ((width + spacing) / (buttonSize + spacing)).toInt()
        return fitting.coerceIn(1, MAX_BUTTONS_PER_ROW)
    }

    /**
     * The number of buttons in each row, based only on the number of options.
     * Rows are as even as possible, longer rows first: 11 options in rows of at
     * most 6 become `[6, 5]`.
     */
    fun rowSizes(optionCount: Int, maxPerRow: Int = MAX_BUTTONS_PER_ROW): List<Int> {
        if (optionCount <= 0) return emptyList()
        val perRow = maxPerRow.coerceAtLeast(1)
        val rowCount = ceil(optionCount.toDouble() / perRow).toInt()
        val base = optionCount / rowCount
        val longer = optionCount % rowCount
        return List(rowCount) { if (it < longer) base + 1 else base }
    }

    /** The scores split into rows, see [rowSizes]. */
    fun rows(range: ScaleRatingTemplate.RatingRange, maxPerRow: Int = MAX_BUTTONS_PER_ROW): List<List<Int>> {
        var remaining = range.values
        return rowSizes(remaining.size, maxPerRow).map { size ->
            remaining.take(size).also { remaining = remaining.drop(size) }
        }
    }
}

internal object ScaleRatingLabels {
    /**
     * The labels under the first and last option. The presets use WhiskrKit's own
     * translated labels; `custom` uses the server's, and shows nothing without them.
     * Pass the survey-language [Resources] (`LocalResources.current`).
     */
    fun endLabels(template: ScaleRatingTemplate, resources: Resources): Pair<String?, String?> {
        val presets = when (template.metric) {
            RatingMetric.NPS -> R.string.whiskrkit_scale_nps_min to R.string.whiskrkit_scale_nps_max
            RatingMetric.CSAT -> R.string.whiskrkit_scale_csat_min to R.string.whiskrkit_scale_csat_max
            RatingMetric.CES -> R.string.whiskrkit_scale_ces_min to R.string.whiskrkit_scale_ces_max
            RatingMetric.CUSTOM -> null
        }
        if (presets != null) return resources.getString(presets.first) to resources.getString(presets.second)
        return template.minLabel.nonBlank() to template.maxLabel.nonBlank()
    }

    /** What the end labels mean, for TalkBack: "0 is Not at all likely, 10 is Extremely likely". */
    fun endsDescription(template: ScaleRatingTemplate, resources: Resources): String? {
        val (min, max) = endLabels(template, resources)
        val parts = listOfNotNull(
            min?.let { resources.getString(R.string.whiskrkit_a11y_scale_end, template.ratingRange.min, it) },
            max?.let { resources.getString(R.string.whiskrkit_a11y_scale_end, template.ratingRange.max, it) },
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    /** What TalkBack reads for one score button: "8 out of 10", then what the end labels mean. */
    fun buttonDescription(score: Int, template: ScaleRatingTemplate, resources: Resources): String =
        buttonDescription(score, template, resources, endsDescription(template, resources))

    /**
     * [buttonDescription] with the [endsDescription] already resolved, so a grid of
     * buttons resolves the end labels only once.
     */
    fun buttonDescription(
        score: Int,
        template: ScaleRatingTemplate,
        resources: Resources,
        endsDescription: String?,
    ): String {
        val position = resources.getString(R.string.whiskrkit_a11y_scale_position, score, template.ratingRange.max)
        return if (endsDescription == null) position else "$position. $endsDescription"
    }

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
