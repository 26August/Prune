package com.raininaugust26.prune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raininaugust26.prune.root.RootExecutor
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

@Composable
fun RootCheckScreen(onContinue: ()->Unit) {
    val ctx = LocalContext.current
    var ok by remember { mutableStateOf<Boolean?>(null) }
    var provider by remember { mutableStateOf("Checking...") }
    fun check() {
        provider = "Checking..."
        ok = null
        CoroutineScope(Dispatchers.IO).launch {
            Shell.getShell { _ -> }
            val rooted = RootExecutor.isRooted()
            val prov = if (rooted) RootExecutor.provider() else "None"
            withContext(Dispatchers.Main) { ok = rooted; provider = prov; if (rooted) AppState.refresh(ctx) }
        }
    }
    LaunchedEffect(Unit) { check() }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Root Check", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        when (ok) {
            null -> CircularProgressIndicator()
            true -> {
                Text("Root granted ($provider)", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onContinue) { Text("Continue") }
            }
            false -> {
                Text("Root not available", color = MaterialTheme.colorScheme.error)
                Text("Provider: $provider", style = MaterialTheme.typography.bodySmall)
                Text("Enable Magisk / KernelSU / APatch and grant root, then retry.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { check() }) { Text("Retry") }
                TextButton(onClick = onContinue) { Text("Continue anyway") }
            }
        }
    }
}
