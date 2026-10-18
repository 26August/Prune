package com.raininaugust26.prune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raininaugust26.prune.data.ExecStatus
import com.raininaugust26.prune.root.RootExecutor
import kotlinx.coroutines.*

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AppState.refreshHistory(ctx) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("History", style = MaterialTheme.typography.titleMedium)
        if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(AppState.history) { h ->
                val restorable = h.lines.filter { it.status == ExecStatus.OK }.map { it.pkg }.distinct()
                val isDryRun = h.lines.any { it.status == ExecStatus.DRY } && restorable.isEmpty()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(h.ts, style = MaterialTheme.typography.titleSmall)
                        if (isDryRun) Text("Dry-run: no changes were made", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        Text("Action: ${h.action}  total=${h.summary?.total ?: h.lines.size} ok=${h.summary?.ok ?: 0} fail=${h.summary?.fail ?: 0} skip=${h.summary?.skip ?: 0} dry=${h.summary?.dry ?: h.lines.count { it.status == ExecStatus.DRY }}", style = MaterialTheme.typography.bodySmall)
                        h.lines.take(40).forEach { l ->
                            val color = when (l.status) {
                                ExecStatus.OK -> MaterialTheme.colorScheme.primary
                                ExecStatus.FAIL -> MaterialTheme.colorScheme.error
                                ExecStatus.SKIP -> MaterialTheme.colorScheme.secondary
                                ExecStatus.DRY -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text("${l.status}|${l.pkg}|${l.msg}", style = MaterialTheme.typography.bodySmall, color = color)
                        }
                        if (h.lines.size > 40) Text("... and ${h.lines.size - 40} more", style = MaterialTheme.typography.bodySmall)
                        if (!isDryRun && restorable.isNotEmpty()) Button(
                            enabled = !running,
                            onClick = {
                                val pkgs = restorable
                                running = true; msg = "Restoring ${pkgs.size} package(s)..."
                                scope.launch(Dispatchers.IO) {
                                    val lines = mutableListOf<String>()
                                    RootExecutor.run(ctx, "restore", pkgs, false, false) { lines += it }
                                    withContext(Dispatchers.Main) {
                                        running = false
                                        msg = lines.lastOrNull { it.startsWith("DONE|") } ?: "Restore done"
                                        AppState.refresh(ctx)
                                    }
                                }
                            }
                        ) { Text(if (running) "Restoring..." else "Restore") }
                    }
                }
            }
        }
        if (AppState.history.isEmpty()) Text("No history yet. Run an action first.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { AppState.refreshHistory(ctx) }) { Text("Refresh") }
    }
}
