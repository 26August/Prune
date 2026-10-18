package com.raininaugust26.prune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raininaugust26.prune.data.PackageStatus

@Composable
fun PackageDetailScreen(pkgId: String, onBack: ()->Unit) {
    val entry = AppState.matched.find { it.id == pkgId }
    val ctx = LocalContext.current
    if (entry == null) { Text("Package not found: $pkgId"); return }
    val status = AppState.scanMap[pkgId] ?: PackageStatus.unknown
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(entry.name, style=MaterialTheme.typography.headlineSmall)
        Text(entry.id, style=MaterialTheme.typography.bodySmall)
        AssistChip(onClick={}, label={Text("${entry.level} · $status")})
        Text(entry.description)
        Text("Impact: ${entry.impact}", style=MaterialTheme.typography.bodySmall)
        Text("Actions: ${entry.actions.joinToString()}")
        Text("Verified: ${entry.verified}")
        HorizontalDivider()
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={
                AppState.selected = setOf(pkgId)
            }){Text("Select")}
            OutlinedButton(onClick=onBack){Text("Back")}
        }
    }
}
