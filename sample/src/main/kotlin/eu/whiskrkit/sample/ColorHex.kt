package eu.whiskrkit.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private val HexPattern = Regex("^#[0-9A-Fa-f]{6}$")

/** Returns [value] as an uppercase `#RRGGBB` string, or null if it isn't one. */
internal fun normalizedHex(value: String): String? = value.takeIf { HexPattern.matches(it) }?.uppercase()

/** `#RRGGBB` to an opaque color. */
internal fun hexToColor(hex: String): Color = Color(0xFF000000 or hex.substring(1).toLong(16))

/** The color as `#RRGGBB` in sRGB; alpha is dropped. */
internal fun Color.toHex(): String = rgbToHex(toArgb() and 0xFFFFFF)

/** A 24-bit `0xRRGGBB` value as `#RRGGBB`. */
internal fun rgbToHex(rgb: Int): String = "#" + rgb.toString(16).padStart(6, '0').uppercase()
