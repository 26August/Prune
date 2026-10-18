package com.raininaugust26.prune.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.raininaugust26.prune.ui.screens.*

@Composable
fun PruneAppNav() {
    val nav = rememberNavController()
    var selected by remember { mutableStateOf(0) }
    val routes = listOf("root","home","list","exec","history","settings")
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = selected==0, onClick = { selected=0; nav.navigate("home") }, icon = { Icon(Icons.Filled.Home,"") }, label = { Text("Home") })
            NavigationBarItem(selected = selected==1, onClick = { selected=1; nav.navigate("list") }, icon = { Icon(Icons.Filled.List,"") }, label = { Text("List") })
            NavigationBarItem(selected = selected==2, onClick = { selected=2; nav.navigate("exec") }, icon = { Icon(Icons.Filled.PlayArrow,"") }, label = { Text("Run") })
            NavigationBarItem(selected = selected==3, onClick = { selected=3; nav.navigate("history") }, icon = { Icon(Icons.Filled.History,"") }, label = { Text("History") })
            NavigationBarItem(selected = selected==4, onClick = { selected=4; nav.navigate("settings") }, icon = { Icon(Icons.Filled.Settings,"") }, label = { Text("Settings") })
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            NavHost(navController = nav, startDestination = "root") {
                composable("root") { RootCheckScreen(onContinue = { nav.navigate("home") }) }
                composable("home") { HomeScreen(onStart = { nav.navigate("list") }) }
                composable("list") { PackageListScreen(onExecute = { nav.navigate("exec") }) }
                composable("exec") { ExecuteScreen() }
                composable("history") { HistoryScreen() }
                composable("settings") { SettingsScreen() }
                composable("detail/{pkg}") { backStack ->
                    val pkg = backStack.arguments?.getString("pkg") ?: ""
                    PackageDetailScreen(pkgId = pkg, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
