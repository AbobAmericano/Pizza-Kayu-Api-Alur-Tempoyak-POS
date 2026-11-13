package com.slicehouse.pos.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.ui.theme.SliceHouseTheme
import com.slicehouse.pos.ui.theme.isDarkTheme

enum class Destination(val label: String, val icon: ImageVector) {
    REGISTER("Register", Icons.Rounded.PointOfSale),
    ORDERS("Orders", Icons.AutoMirrored.Rounded.ReceiptLong),
    MENU("Menu", Icons.Rounded.RestaurantMenu),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun PosApp(
    viewModel: AppViewModel = viewModel(factory = PosViewModelFactory),
    authViewModel: AuthViewModel = viewModel(factory = PosViewModelFactory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val recoveryCode by authViewModel.newRecoveryCode.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.REGISTER) }
    var checkout by rememberSaveable { mutableStateOf(false) }
    var receiptOrder by remember { mutableStateOf<Order?>(null) }
    val dark = isDarkTheme(state.settings.themeMode)

    SliceHouseTheme(themeMode = state.settings.themeMode) {
        SystemBars(dark = dark)
        BackHandler(enabled = checkout) { checkout = false }

        if (!state.loaded || !auth.loaded) {
            // The database opens in the background; this is usually a split second.
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@SliceHouseTheme
        }

        if (auth.needsSetup) {
            SetupOwnerScreen(shopName = state.settings.shopName, viewModel = authViewModel)
            return@SliceHouseTheme
        }
        val signedIn = auth.signedIn
        if (signedIn == null) {
            LockScreen(state = auth, shopName = state.settings.shopName, viewModel = authViewModel)
            return@SliceHouseTheme
        }
        val lock = {
            checkout = false
            receiptOrder = null
            authViewModel.lock()
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val wide = maxWidth >= 900.dp

            if (checkout) {
                CheckoutScreen(
                    wide = wide,
                    onBack = { checkout = false },
                    onCompleted = { order ->
                        checkout = false
                        receiptOrder = order
                    }
                )
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        if (!wide) {
                            BottomBar(destination, onLock = lock) { destination = it }
                        }
                    }
                ) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        if (wide) {
                            NavRail(destination, signedIn.name, onLock = lock) { destination = it }
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when (destination) {
                                Destination.REGISTER -> RegisterScreen(
                                    wide = wide,
                                    onCheckout = { checkout = true }
                                )
                                Destination.ORDERS -> OrdersScreen(
                                    onGoToRegister = { destination = Destination.REGISTER }
                                )
                                Destination.MENU -> MenuScreen()
                                Destination.SETTINGS -> SettingsScreen()
                            }
                        }
                    }
                }
            }
        }

        receiptOrder?.let { order ->
            ReceiptDialog(
                order = order,
                settings = state.settings,
                onDismiss = { receiptOrder = null }
            )
        }

        recoveryCode?.let { code ->
            RecoveryCodeDialog(code = code, onDone = authViewModel::recoveryCodeShown)
        }

        state.storageWarning?.let { message ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Saved data") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissStorageWarning() }) { Text("OK") }
                }
            )
        }
    }
}

@Composable
private fun NavRail(current: Destination, signedInName: String, onLock: () -> Unit, onSelect: (Destination) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxHeight().width(104.dp)
    ) {
        Column(
            Modifier.fillMaxHeight().padding(vertical = 22.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("🍕", fontSize = 26.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "PIZZA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(30.dp))
            Destination.entries.forEach { item ->
                RailItem(item, item == current) { onSelect(item) }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.weight(1f))
            LockRailItem(signedInName, onLock)
        }
    }
}

/** Bottom of the rail: who is signed in, tap to lock. */
@Composable
private fun LockRailItem(name: String, onLock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onLock)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("Lock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RailItem(destination: Destination, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(destination.icon, contentDescription = destination.label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun BottomBar(current: Destination, onLock: () -> Unit, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Destination.entries.forEach { item ->
            NavigationBarItem(
                selected = item == current,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = onLock,
            icon = { Icon(Icons.Rounded.Lock, contentDescription = "Lock") },
            label = { Text("Lock") }
        )
    }
}

/** Keeps status and navigation bar icons readable when the theme is switched manually. */
@Composable
private fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(dark, view) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        onDispose { }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
