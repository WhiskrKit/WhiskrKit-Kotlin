package eu.whiskrkit.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class WhiskrButtonKind { Primary, Secondary }

/**
 * Renders the primary/secondary survey button per the resolved theme — either
 * the built-in variant rendering or the host's custom slot (decision #12).
 */
@Composable
internal fun WhiskrButton(
    text: String,
    onClick: () -> Unit,
    kind: WhiskrButtonKind,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val theme = LocalWhiskrKitTheme.current
    val appearance = when (kind) {
        WhiskrButtonKind.Primary -> theme.primaryButton
        WhiskrButtonKind.Secondary -> theme.secondaryButton
    }

    when (appearance) {
        is ResolvedButtonAppearance.Custom -> appearance.content(text, true, onClick)
        is ResolvedButtonAppearance.Variant -> Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(appearance.cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = appearance.backgroundColor,
                contentColor = appearance.textColor,
            ),
            border = appearance.borderColor?.let { BorderStroke(2.dp, it) },
            contentPadding = if (compact) {
                PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            } else {
                ButtonDefaults.ContentPadding
            },
        ) {
            Text(text = text, style = appearance.textStyle)
        }
    }
}
