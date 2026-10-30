package eu.whiskrkit.sample

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.whiskrkit.WhiskrKit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Forces the app's appearance, presented surveys included. Not persisted. */
internal enum class PreviewAppearance(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

@Composable
internal fun PlaygroundScreen(
    settings: ThemeSettings,
    appearance: PreviewAppearance,
    onAppearanceChange: (PreviewAppearance) -> Unit,
) {
    var editingRole by remember { mutableStateOf<ColorRole?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = WindowInsets.systemBars.asPaddingValues()) {
            item {
                Text(
                    text = "WhiskrKit Playground",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
            }

            surveySections()
            presetSection(settings)
            colorSection(settings, appearance, onAppearanceChange, onEdit = { editingRole = it })
            codeSection(themeSnippet(settings.customColors))
        }
    }

    editingRole?.let { role ->
        val initialHex = settings.customColors[role.id]
            ?: role.systemColor(MaterialTheme.colorScheme).toHex()
        ColorPickerDialog(
            title = role.name,
            initialHex = initialHex,
            onDismiss = { editingRole = null },
            onConfirm = { hex ->
                settings.setColor(role, hex)
                editingRole = null
            },
        )
    }
}

private fun LazyListScope.surveySections() {
    SurveyDescriptor.Group.entries.forEach { group ->
        item { SectionHeader(group.title) }
        items(SurveyDescriptor.all.filter { it.group == group }) { survey ->
            ListItem(
                headlineContent = { Text(survey.name) },
                supportingContent = { Text(survey.summary) },
                leadingContent = { Icon(survey.icon, contentDescription = null) },
                modifier = Modifier.clickable { WhiskrKit.present(survey.id) },
            )
        }
    }
    item { SectionFooter("Surveys are mocked: responses and events are never sent to WhiskrKit.") }
}

private fun LazyListScope.presetSection(settings: ThemeSettings) {
    item { SectionHeader("Presets") }
    items(ThemePreset.all) { preset ->
        val scheme = MaterialTheme.colorScheme
        ListItem(
            headlineContent = { Text(preset.name) },
            supportingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    ColorRole.all.forEach { role ->
                        val color = preset.colors[role.id]?.let(::hexToColor) ?: role.systemColor(scheme)
                        Swatch(color, size = 16)
                    }
                }
            },
            trailingContent = {
                if (settings.matchingPreset == preset) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected")
                }
            },
            modifier = Modifier.clickable { settings.apply(preset) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.colorSection(
    settings: ThemeSettings,
    appearance: PreviewAppearance,
    onAppearanceChange: (PreviewAppearance) -> Unit,
    onEdit: (ColorRole) -> Unit,
) {
    item { SectionHeader("Colors") }
    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Preview in")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PreviewAppearance.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = appearance == option,
                        onClick = { onAppearanceChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, PreviewAppearance.entries.size),
                        icon = {},
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }
    items(ColorRole.all) { role ->
        val custom = settings.customColors[role.id]
        val color = custom?.let(::hexToColor) ?: role.systemColor(MaterialTheme.colorScheme)
        ListItem(
            headlineContent = { Text(role.name) },
            supportingContent = {
                Column {
                    Text(role.description)
                    Text(custom ?: "System", fontFamily = FontFamily.Monospace)
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (custom != null) {
                        IconButton(onClick = { settings.reset(role) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset ${role.name} to System")
                        }
                    }
                    IconButton(
                        onClick = { onEdit(role) },
                        modifier = Modifier.semantics { contentDescription = "Pick ${role.name} color" },
                    ) {
                        Swatch(color, size = 28)
                    }
                }
            },
        )
    }
    item {
        SectionFooter(
            "A picked color is fixed: it looks the same in light and dark mode. " +
                "Reset a color to go back to its adaptive system color.",
        )
    }
}

private fun LazyListScope.codeSection(snippet: String) {
    item { SectionHeader("Code") }
    item {
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = snippet,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                softWrap = false,
            )
        }
    }
    item { CopyButton(snippet) }
    item {
        SectionFooter(
            "Paste inside a @Composable, such as setContent. Imports: " +
                "androidx.compose.ui.graphics.Color, eu.whiskrkit.theme.WhiskrKitTheme, " +
                "eu.whiskrkit.ui.WhiskrKitHost.",
        )
    }
}

@Composable
private fun CopyButton(snippet: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    Button(
        onClick = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("WhiskrKit theme", snippet)))
                copied = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (copied) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Copied")
        } else {
            Text("Copy code")
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
internal fun Swatch(color: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}
