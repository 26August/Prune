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

@Composable
fun PackageListScreen(onExecute: ()->Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf(Profile.Custom) }
    val showCaution = remember { mutableStateOf(false) }
    var showRemoved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val s = SettingsStore(ctx)
        s.flow.collect { showCaution.value = it.showCaution }
    }
    val levels = listOf("safe","optional","caution")
    val curLevel = levels[tab]
    val filtered = AppState.matched.filter { e ->
        val status = AppState.scanMap[e.id] ?: return@filter false
        val visibleByRemoved = if (showRemoved) status == PackageStatus.uninstalled else status != PackageStatus.uninstalled
        visibleByRemoved && (query.isBlank() || e.id.contains(query, true) || e.name.contains(query, true)) && e.level == curLevel
    }.let { list ->
        if (curLevel == "caution" && !showCaution.value) emptyList() else list
    }

    fun applyProfile(p: Profile) {
        profile = p
        val sel = when(p) {
            Profile.Safe -> AppState.matched.filter { it.level == "safe" && AppState.scanMap[it.id]==PackageStatus.installed }.map { it.id }
            Profile.Aggressive -> AppState.matched.filter { (it.level=="safe"||it.level=="optional") && AppState.scanMap[it.id]==PackageStatus.installed }.map { it.id }
            Profile.Custom -> emptyList()
        }
        AppState.selected = sel.toSet()
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value=query, onValueChange={query=it}, label={Text("Search")}, modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=profile==Profile.Safe, onClick={applyProfile(Profile.Safe)}, label={Text("Safe")})
            FilterChip(selected=profile==Profile.Aggressive, onClick={applyProfile(Profile.Aggressive)}, label={Text("Aggressive")})
            FilterChip(selected=profile==Profile.Custom, onClick={applyProfile(Profile.Custom)}, label={Text("Custom")})
            FilterChip(selected=showRemoved, onClick={showRemoved=!showRemoved}, label={Text("Show removed")})
        }
        TabRow(selectedTabIndex=tab) {
            Tab(selected=tab==0, onClick={tab=0}, text={Text("Safe")})
            Tab(selected=tab==1, onClick={tab=1}, text={Text("Optional")})
            Tab(selected=tab==2, onClick={tab=2}, text={Text("Caution")})
        }
        if (curLevel=="caution" && !showCaution.value) {
            Text("Caution level hidden. Enable in Settings.", style=MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={
                val ids = filtered.filter { AppState.scanMap[it.id] != PackageStatus.uninstalled }.map{it.id}.toSet()
                AppState.selected = AppState.selected + ids
            }){Text("Select all")}
            TextButton(onClick={
                val ids = filtered.map{it.id}.toSet()
                AppState.selected = AppState.selected - ids
            }){Text("Clear")}
            Spacer(Modifier.weight(1f))
            Text("${AppState.selected.size} selected")
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(4.dp), modifier=Modifier.weight(1f)) {
            items(filtered) { e ->
                val st = AppState.scanMap[e.id] ?: PackageStatus.unknown
                val checked = e.id in AppState.selected
                Card {
                    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style=MaterialTheme.typography.titleSmall)
                            Text(e.id, style=MaterialTheme.typography.bodySmall)
                            AssistChip(onClick={}, label={Text("${e.level} · $st")})
                        }
                        Checkbox(checked=checked, enabled=st != PackageStatus.uninstalled, onCheckedChange={ c ->
                            AppState.selected = if(c) AppState.selected + e.id else AppState.selected - e.id
                        })
                    }
                }
            }
        }
        Button(onClick=onExecute, modifier=Modifier.fillMaxWidth(), enabled=AppState.selected.isNotEmpty()){Text("Review & Execute (${AppState.selected.size})")}
    }
}
