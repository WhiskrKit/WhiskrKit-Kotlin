package eu.whiskrkit.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A minimal opaque color picker: red, green and blue sliders plus a hex field.
 * Compose has no built-in color picker, and this keeps the sample free of
 * third-party dependencies.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var hex by remember { mutableStateOf(initialHex) }
    var hexInput by remember { mutableStateOf(initialHex) }
    val rgb = hex.substring(1).toInt(16)

    fun setChannel(shift: Int, value: Int) {
        val current = hex.substring(1).toInt(16)
        hex = rgbToHex((current and (0xFF shl shift).inv()) or (value shl shift))
        hexInput = hex
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(hex) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(hexToColor(hex), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                )
                ChannelSlider("R", (rgb shr 16) and 0xFF) { setChannel(16, it) }
                ChannelSlider("G", (rgb shr 8) and 0xFF) { setChannel(8, it) }
                ChannelSlider("B", rgb and 0xFF) { setChannel(0, it) }
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        normalizedHex(if (input.startsWith("#")) input else "#$input")?.let { hex = it }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun ChannelSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = FontFamily.Monospace, modifier = Modifier.width(20.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toString(),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp),
        )
    }
}
