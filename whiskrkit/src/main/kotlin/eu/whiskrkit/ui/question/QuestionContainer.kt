package eu.whiskrkit.ui.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import eu.whiskrkit.R
import eu.whiskrkit.ui.WhiskrIcons
import eu.whiskrkit.theme.LocalWhiskrKitTheme

/**
 * Shared header chrome for every question — the port of the iOS
 * `RatingContainerView`: title, an italic "Required" tag that wraps to its own
 * line when space is tight (FlowRow replaces iOS `ViewThatFits`), and subtitle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuestionContainer(
    title: String?,
    subtitle: String?,
    isRequired: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val theme = LocalWhiskrKitTheme.current
    val hasHeader = title != null || subtitle != null

    Column(modifier = modifier) {
        if (hasHeader) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = theme.headline.textStyle,
                            color = theme.headline.color,
                        )
                    }
                    if (isRequired) {
                        Text(
                            text = stringResource(R.string.whiskrkit_required),
                            style = theme.subheadline.textStyle,
                            color = theme.subheadline.color,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = theme.subheadline.textStyle,
                        color = theme.subheadline.color,
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            content()
        }
    }
}

/** The red italic "answer required" footnote shown after a failed submit attempt. */
@Composable
internal fun RequiredErrorMessage(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = WhiskrIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontStyle = FontStyle.Italic,
        )
    }
}
