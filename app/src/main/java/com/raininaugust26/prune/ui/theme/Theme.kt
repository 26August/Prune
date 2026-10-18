package com.raininaugust26.prune.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val Light = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF2E7D32))
private val Dark = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF81C784))

@Composable
fun PruneTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable ()->Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
