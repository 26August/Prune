package com.raininaugust26.prune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onStart: ()->Unit) {
    val ctx = LocalContext.current
    val info = AppState.deviceInfo
    val matched = AppState.matched
    val scan = AppState.scanMap
    val installedCount = matched.count { scan[it.id] == com.raininaugust26.prune.data.PackageStatus.installed }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Prune", style = MaterialTheme.typography.headlineMedium)
        Card { Column(Modifier.padding(12.dp)) {
            Text("Device: ${info.manufacturer} ${info.brand} ${info.model}")
            Text("Android ${info.android} (SDK ${info.sdk})")
            Text("Root: ${info.rootProvider}")
            Text("Packages matched: ${matched.size}  | Installed: $installedCount  | Selected: ${AppState.selected.size}")
        } }
        Button(onClick = { AppState.refresh(ctx) }) { Text("Rescan") }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start — Select Packages") }
        if (AppState.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Default action is Disable (safe). Use dry-run first.", style = MaterialTheme.typography.bodySmall)
    }
}
