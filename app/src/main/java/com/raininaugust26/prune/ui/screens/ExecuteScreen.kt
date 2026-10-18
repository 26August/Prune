package com.raininaugust26.prune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raininaugust26.prune.data.*
import com.raininaugust26.prune.root.RootExecutor
import com.raininaugust26.prune.root.ShellContract
import kotlinx.coroutines.*

@Composable
fun ExecuteScreen() {
    val ctx = LocalContext.current
    var dry by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf(DefaultAction.disable) }
    var wipe by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var cautionAck by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }
    var summary by remember { mutableStateOf<ExecSummary?>(null) }
    val scope = rememberCoroutineScope()

    val selected = AppState.selected.toList()
    val hasCaution = selected.any { id -> AppState.matched.find { it.id==id }?.level=="caution" }
    val needCautionAck = hasCaution && !cautionAck

    LaunchedEffect(Unit) {
        val s = SettingsStore(ctx)
        s.flow.collect { st -> dry = st.dryRun; action = st.defaultAction; wipe = st.wipe }
    }

    fun runNow() {
        if (selected.isEmpty() || running || needCautionAck) return
        running = true; log = emptyList(); summary = null
        AppState.logLines = emptyList()
        scope.launch(Dispatchers.IO) {
            val actStr = when(action) { DefaultAction.disable->"disable"; DefaultAction.uninstall->"uninstall" }
            val okPkgs = java.util.Collections.synchronizedList(mutableListOf<String>())
            val wasDry = dry
            val code = RootExecutor.run(ctx, actStr, selected, dry, wipe) { line ->
                ShellContract.parseLine(line)?.let { if (it.status == ExecStatus.OK) okPkgs += it.pkg }
                scope.launch(Dispatchers.Main) {
                    log = log + line
                    AppState.logLines = log
                    ShellContract.parseDone(line)?.let { summary = it }
                }
            }
            withContext(Dispatchers.Main) {
                running = false
                if (!wasDry) AppState.selected = AppState.selected - okPkgs.toSet()
                AppState.refreshHistory(ctx)
                AppState.refresh(ctx)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Execute — ${selected.size} package(s)", style=MaterialTheme.typography.titleMedium)
        Text(selected.joinToString(", ").take(300), style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=action==DefaultAction.disable, onClick={action=DefaultAction.disable}, label={Text("Disable")})
            FilterChip(selected=action==DefaultAction.uninstall, onClick={action=DefaultAction.uninstall}, label={Text("Uninstall (user 0)")})
        }
        Row {
            Switch(checked=dry, onCheckedChange={dry=it}); Spacer(Modifier.width(8.dp)); Text("Dry-run")
            Spacer(Modifier.width(16.dp))
            Switch(checked=wipe, onCheckedChange={wipe=it}); Spacer(Modifier.width(8.dp)); Text("Wipe data")
        }
        if (hasCaution) {
            Row { Checkbox(checked=cautionAck, onCheckedChange={cautionAck=it}); Text("I understand caution packages may break features.") }
        }
        Button(onClick={runNow()}, enabled=!running && selected.isNotEmpty() && !needCautionAck, modifier=Modifier.fillMaxWidth()) {
            Text(if(running) "Running..." else if(dry) "Dry-run" else "Confirm & Run")
        }
        if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
        summary?.let { Text("DONE ${it.total} ok=${it.ok} fail=${it.fail} skip=${it.skip} dry=${it.dry}", style=MaterialTheme.typography.bodySmall) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(2.dp)) {
            items(log) { l ->
                val c = when {
                    l.startsWith("OK|") -> MaterialTheme.colorScheme.primary
                    l.startsWith("FAIL|") -> MaterialTheme.colorScheme.error
                    l.startsWith("SKIP|") -> MaterialTheme.colorScheme.secondary
                    l.startsWith("DRY|") -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(l, style=MaterialTheme.typography.bodySmall, color=c)
            }
        }
    }
}
