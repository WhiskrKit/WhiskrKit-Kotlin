package eu.whiskrkit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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

/**
 * [preferred] when it reads at [MIN_TEXT_CONTRAST] or better on [background],
 * otherwise black or white, whichever contrasts more.
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
    return if (black >= white) Color.Black else Color.White
}

private val Color.isSpecified: Boolean get() = this != Color.Unspecified
