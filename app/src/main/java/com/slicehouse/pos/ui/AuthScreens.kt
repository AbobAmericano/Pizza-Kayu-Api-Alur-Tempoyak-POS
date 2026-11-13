package com.slicehouse.pos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.auth.AuthState
import com.slicehouse.pos.data.auth.PinHasher
import com.slicehouse.pos.data.auth.PinResult
import kotlinx.coroutines.delay

/** First launch (or first launch after upgrading): create the owner account. */
@Composable
fun SetupOwnerScreen(shopName: String, viewModel: AuthViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Keeps the fields and button above the on-screen keyboard.
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 480.dp).padding(24.dp)
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🍕", fontSize = 40.sp)
                Text("Set up the owner account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$shopName now uses staff PINs. The owner PIN approves refunds, price changes " +
                        "and settings. You can add cashiers afterwards in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Owner name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PinTextField(pin, { pin = it; error = null }, "Owner PIN (4–6 digits)")
                PinTextField(confirm, { confirm = it; error = null }, "Type the PIN again")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        error = when {
                            name.isBlank() -> "Enter the owner's name."
                            !PinHasher.isValidPin(pin) -> "PIN must be 4 to 6 digits."
                            pin != confirm -> "The two PINs don't match."
                            else -> null
                        }
                        if (error == null) {
                            busy = true
                            viewModel.setUpOwner(name, pin) { problem ->
                                busy = false
                                error = problem
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Create owner account") }
            }
        }
    }
}

/** Shown whenever nobody is signed in: pick your name, type your PIN. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockScreen(state: AuthState, shopName: String, viewModel: AuthViewModel) {
    val people = state.activeStaff
    var selectedId by rememberSaveable { mutableStateOf(people.singleOrNull()?.id) }
    val selected = people.firstOrNull { it.id == selectedId }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }

    // Counts down while PIN entry is blocked after too many wrong tries.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.lockedUntil) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= state.lockedUntil) break
            delay(1_000)
        }
    }
    val secondsLeft = ((state.lockedUntil - now + 999) / 1_000).coerceAtLeast(0)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 36.sp)
        Text(shopName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Who's working?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 640.dp)
        ) {
            people.forEach { person ->
                FilterChip(
                    selected = person.id == selectedId,
                    onClick = {
                        selectedId = person.id
                        pin = ""
                        message = null
                    },
                    label = {
                        Text(
                            if (person.isOwner) "${person.name} · Owner" else person.name,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        if (selected == null) {
            Text("Tap your name to sign in.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Enter ${selected.name}'s PIN", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            PinPad(
                pin = pin,
                onPinChange = {
                    pin = it
                    message = null
                },
                onSubmit = {
                    val attempt = pin
                    pin = ""
                    busy = true
                    viewModel.signIn(selected.id, attempt) { result ->
                        busy = false
                        message = pinMessage(result)
                    }
                },
                enabled = !busy && secondsLeft == 0L
            )
            val shown = if (secondsLeft > 0) "Too many wrong tries. Try again in ${secondsLeft}s." else message
            shown?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            if (selected.isOwner) {
                TextButton(onClick = { showRecovery = true }) { Text("Forgot owner PIN?") }
            }
        }
    }

    if (showRecovery) {
        RecoveryDialog(viewModel = viewModel, onDismiss = { showRecovery = false })
    }
}

/** Forgotten owner PIN: recovery code plus a new PIN. */
@Composable
private fun RecoveryDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset the owner PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the 12-character recovery code you wrote down at setup, then choose a new PIN.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text("Recovery code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                PinTextField(pin, { pin = it; error = null }, "New PIN")
                PinTextField(confirm, { confirm = it; error = null }, "Type the new PIN again")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = when {
                        !PinHasher.isValidPin(pin) -> "PIN must be 4 to 6 digits."
                        pin != confirm -> "The two PINs don't match."
                        else -> null
                    }
                    if (error == null) {
                        busy = true
                        viewModel.recoverOwner(code, pin) { result ->
                            busy = false
                            when (result) {
                                is PinResult.Success -> onDismiss()
                                is PinResult.Wrong -> error = "That code doesn't match. ${result.attemptsLeft} tries left."
                                is PinResult.LockedOut -> error = "Too many wrong tries. Wait 30 seconds and try again."
                            }
                        }
                    }
                }
            ) { Text("Reset PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Shown once after setup or recovery. Can't be dismissed by tapping outside. */
@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Write down your recovery code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "If the owner PIN is ever forgotten, this code sets a new one. It's shown only " +
                        "once, so keep it somewhere safe, away from the tablet."
                )
                Text(
                    PinHasher.formatRecoveryCode(code),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("I've written it down") } }
    )
}
