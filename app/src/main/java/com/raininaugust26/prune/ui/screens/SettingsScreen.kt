package com.raininaugust26.prune.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raininaugust26.prune.data.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SettingsState()) }
    var brandInput by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val store = SettingsStore(ctx)
        store.flow.collect { state = it; brandInput = it.brandOverride ?: "" }
    }
    val store = remember { SettingsStore(ctx) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.defaultAction == DefaultAction.disable, onClick = { scope.launch { store.setAction(DefaultAction.disable) } }, label = { Text("Disable") })
            FilterChip(selected = state.defaultAction == DefaultAction.uninstall, onClick = { scope.launch { store.setAction(DefaultAction.uninstall) } }, label = { Text("Uninstall (user 0)") })
        }
        Row { Switch(checked = state.dryRun, onCheckedChange = { scope.launch { store.setDry(it) } }); Spacer(Modifier.width(8.dp)); Text("Dry-run default ON") }
        Row { Switch(checked = state.wipe, onCheckedChange = { scope.launch { store.setWipe(it) } }); Spacer(Modifier.width(8.dp)); Text("Wipe data on uninstall") }
        Row { Switch(checked = state.showCaution, onCheckedChange = { scope.launch { store.setShowCaution(it) } }); Spacer(Modifier.width(8.dp)); Text("Show caution level") }
        Text("Reboot after", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.reboot == RebootPref.Ask, onClick = { scope.launch { store.setReboot(RebootPref.Ask) } }, label = { Text("Ask") })
            FilterChip(selected = state.reboot == RebootPref.Auto, onClick = { scope.launch { store.setReboot(RebootPref.Auto) } }, label = { Text("Auto") })
            FilterChip(selected = state.reboot == RebootPref.Never, onClick = { scope.launch { store.setReboot(RebootPref.Never) } }, label = { Text("Never") })
        }
        OutlinedTextField(value = brandInput, onValueChange = { brandInput = it }, label = { Text("Brand override (empty = auto)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { scope.launch { store.setBrandOverride(brandInput.ifBlank { null }); AppState.refresh(ctx) } }) { Text("Apply brand") }
        Text("Theme", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.theme == "system", onClick = { scope.launch { store.setTheme("system") } }, label = { Text("System") })
            FilterChip(selected = state.theme == "light", onClick = { scope.launch { store.setTheme("light") } }, label = { Text("Light") })
            FilterChip(selected = state.theme == "dark", onClick = { scope.launch { store.setTheme("dark") } }, label = { Text("Dark") })
        }
        HorizontalDivider()
        Text("Export / Import custom list (JSON same schema as packages/*.json) — place file in app files and select via file manager.", style = MaterialTheme.typography.bodySmall)
        Text("Backup before execution is always ON. Logs kept: last 20 runs in files/prune/.", style = MaterialTheme.typography.bodySmall)
    }
}
