package com.slicehouse.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.auth.PinResult

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/** Big-button number pad for typing a PIN at the counter. Digits are shown only as dots. */
@Composable
fun PinPad(
    pin: String,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.semantics { contentDescription = "${pin.length} digits entered" }
        ) {
            repeat(MAX_PIN_LENGTH) { index ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    val onClick = {
                        when (key) {
                            "⌫" -> onPinChange(pin.dropLast(1))
                            "OK" -> onSubmit()
                            else -> if (pin.length < MAX_PIN_LENGTH) onPinChange(pin + key)
                        }
                    }
                    val label = @Composable {
                        Text(key, fontSize = if (key == "OK") 18.sp else 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                    val buttonModifier = Modifier.size(72.dp)
                    if (key == "OK") {
                        Button(
                            onClick = onClick,
                            enabled = enabled && pin.length >= MIN_PIN_LENGTH,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = buttonModifier
                        ) { label() }
                    } else {
                        FilledTonalButton(
                            onClick = onClick,
                            enabled = enabled,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = buttonModifier.semantics {
                                if (key == "⌫") contentDescription = "Delete digit"
                            }
                        ) { label() }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** A text field for PINs, for forms where a keyboard is fine (setup, adding staff). */
@Composable
fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(MAX_PIN_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier
    )
}

/** What to tell the person after a PIN check. */
fun pinMessage(result: PinResult): String? = when (result) {
    is PinResult.Success -> null
    is PinResult.Wrong ->
        "Wrong PIN. ${result.attemptsLeft} ${if (result.attemptsLeft == 1) "try" else "tries"} left."
    is PinResult.LockedOut -> "Too many wrong tries. Wait 30 seconds and try again."
}
