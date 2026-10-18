package com.raininaugust26.prune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.raininaugust26.prune.data.SettingsStore
import com.raininaugust26.prune.ui.PruneAppNav
import com.raininaugust26.prune.ui.theme.PruneThemeFromPref

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            var theme by remember { mutableStateOf("system") }
            LaunchedEffect(Unit) {
                SettingsStore(ctx).flow.collect { theme = it.theme }
            }
            PruneThemeFromPref(theme = theme) { PruneAppNav() }
        }
    }
}
