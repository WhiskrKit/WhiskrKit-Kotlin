package eu.whiskrkit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG 2.x minimum contrast for normal-size text (level AA). */
internal const val MIN_TEXT_CONTRAST = 4.5f

/**
 * WCAG contrast ratio between [foreground] and [background], from 1 to 21.
 * Translucent colors are blended over [backdrop] first: the foreground over the
 * background, the background over the backdrop.
 */
internal fun contrastRatio(
    foreground: Color,
    background: Color,
    backdrop: Color = Color.White,
): Float {
    val opaqueBackground = background.compositeOver(backdrop)
    val opaqueForeground = foreground.compositeOver(opaqueBackground)
    val a = opaqueForeground.luminance()
    val b = opaqueBackground.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

/** Blend step for [readableColor], as a fraction of the way to black or white. */
internal const val BLEND_STEP = 0.01f

/**
 * [preferred] when it reads at [MIN_TEXT_CONTRAST] or better on [background].
 * Otherwise [preferred] blended toward black (on a light background) or white (on
 * a dark one), in [BLEND_STEP] steps, until it does: the smallest change, so it
 * stays as close to the theme's look as possible. Pure black or white (whichever
 * contrasts more) is the last resort, when no blend gets there.
 */
internal fun readableColor(
    preferred: Color,
    background: Color,
    backdrop: Color = Color.White,
): Color {
    if (preferred.isSpecified && contrastRatio(preferred, background, backdrop) >= MIN_TEXT_CONTRAST) {
        return preferred
    }
    val black = contrastRatio(Color.Black, background, backdrop)
    val white = contrastRatio(Color.White, background, backdrop)
    val target = if (black >= white) Color.Black else Color.White
    if (preferred.isSpecified) {
        val steps = (1f / BLEND_STEP).toInt()
        for (step in 1 until steps) {
            val blended = lerp(preferred, target, step * BLEND_STEP)
            if (contrastRatio(blended, background, backdrop) >= MIN_TEXT_CONTRAST) return blended
        }
    }
    return target
}
