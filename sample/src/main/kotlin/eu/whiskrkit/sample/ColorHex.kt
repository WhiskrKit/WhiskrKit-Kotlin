package eu.whiskrkit.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

private val HexPattern = Regex("^#[0-9A-Fa-f]{6}$")

/** Returns [value] as an uppercase `#RRGGBB` string, or null if it isn't one. */
internal fun normalizedHex(value: String): String? = value.takeIf { HexPattern.matches(it) }?.uppercase()

/** `#RRGGBB` to an opaque color. */
internal fun hexToColor(hex: String): Color = Color(0xFF000000 or hex.substring(1).toLong(16))

/** The color as `#RRGGBB` in sRGB; alpha is dropped. */
internal fun Color.toHex(): String = rgbToHex(toArgb() and 0xFFFFFF)

/**
 * A slightly raised surface on top of [hex]: mixed 10% toward white when the
 * color is dark, or toward black when it's light.
 */
internal fun raisedHex(hex: String): String {
    val rgb = hex.substring(1).toInt(16)
    val channels = listOf(16, 8, 0).map { shift -> (rgb shr shift) and 0xFF }
    val (red, green, blue) = channels
    val isDark = 0.2126 * red + 0.7152 * green + 0.0722 * blue < 128
    val target = if (isDark) 0xFF else 0x00
    return rgbToHex(
        channels.fold(0) { acc, channel -> (acc shl 8) or (channel + (target - channel) * 0.1).roundToInt() },
    )
}

/** A 24-bit `0xRRGGBB` value as `#RRGGBB`. */
internal fun rgbToHex(rgb: Int): String = "#" + rgb.toString(16).padStart(6, '0').uppercase()
