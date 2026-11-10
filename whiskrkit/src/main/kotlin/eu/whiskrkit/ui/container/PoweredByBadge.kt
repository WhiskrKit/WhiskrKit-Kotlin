package eu.whiskrkit.ui.container

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.theme.readableColor

/** The product name in the badge. A brand name: passed to the string, never translated. */
private const val BRAND_NAME = "WhiskrKit"

/**
 * "Powered by WhiskrKit", centered below a survey's submit button. Shown only
 * when the server asks for it (see `Branding`).
 *
 * Static on purpose: no animation, not tappable, no link. Logo and text are one
 * accessibility element, read after the submit button.
 *
 * @param textColor the theme's secondary text color; swapped for black or white
 *   when it wouldn't reach 4.5:1 on [background].
 * @param background what the badge sits on, for the contrast check.
 * @param fontFamily the theme's font, so the badge matches the text around it; the
 *   size stays caption-sized.
 */
@Composable
internal fun PoweredByBadge(
    textColor: Color,
    background: Color,
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.whiskrkit_powered_by, BRAND_NAME)
    val style = MaterialTheme.typography.bodySmall.let { if (fontFamily != null) it.copy(fontFamily = fontFamily) else it }
    // Compose's default text backdrop: what shows through a translucent background.
    val surface = MaterialTheme.colorScheme.surface
    val color = remember(textColor, background, surface) { readableColor(textColor, background, surface) }
    // Sized in sp so the logo grows with the user's font size, like the text. A host
    // typography without an sp size must not crash the survey: use the default.
    val logoSize = with(LocalDensity.current) { (style.fontSize.takeIf { it.isSp } ?: 12.sp).toDp() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        Image(
            painter = painterResource(R.drawable.whiskrkit_logo_small),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(logoSize),
        )
        Text(
            text = label,
            style = style,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
