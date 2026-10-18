package com.raininaugust26.prune.ui.screens

import androidx.compose.runtime.*
import com.raininaugust26.prune.data.*
import com.raininaugust26.prune.root.RootExecutor
import android.content.Context
import com.raininaugust26.prune.root.ShellContract
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object AppState {
    var deviceInfo by mutableStateOf(DeviceInfo())
    var scanMap by mutableStateOf<Map<String, PackageStatus>>(emptyMap())
    var matched by mutableStateOf<List<PackageEntry>>(emptyList())
    var selected by mutableStateOf(setOf<String>())
    var logLines by mutableStateOf(listOf<String>())
    var history by mutableStateOf(listOf<HistoryEntry>())
    var loading by mutableStateOf(false)

    fun refresh(ctx: Context) {
        loading = true
        CoroutineScope(Dispatchers.IO).launch {
            val repo = PackageRepository(ctx).also { it.load() }
            val infoMap = RootExecutor.deviceInfo(ctx)
            val scan = runCatching { RootExecutor.scan(ctx) }.getOrDefault(emptyMap())
            val store = SettingsStore(ctx)
            val override = runCatching { store.flow.first().brandOverride }.getOrNull()
            val info = DeviceInfo(
                brand = infoMap["brand"] ?: "", manufacturer = infoMap["manufacturer"] ?: "",
                model = infoMap["model"] ?: "", android = infoMap["android"] ?: "",
                sdk = infoMap["sdk"] ?: "", rootProvider = RootExecutor.provider()
            )
            val m = repo.matchedFor(info, override)
            withContext(Dispatchers.Main) {
                deviceInfo = info; scanMap = scan; matched = m; loading = false
                selected = selected.filter { scan[it] != PackageStatus.uninstalled }.toSet()
            }
            refreshHistory(ctx)
        }
    }
    fun refreshHistory(ctx: Context) {
        val wd = RootExecutor.workDir(ctx)
        val files = wd.listFiles { f -> f.name.startsWith("exec_") && f.name.endsWith(".log") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        val entries = files.take(20).mapNotNull { f ->
            val lines = f.readLines()
            val parsed = lines.mapNotNull { ShellContract.parseLine(it) }
            val done = lines.firstOrNull { it.startsWith("DONE|") }?.let { ShellContract.parseDone(it) }
            val action = f.name.substringAfter("exec_").substringBefore("_")
            HistoryEntry(f.name, action, parsed, done)
        }
        history = entries
    }
}
