package com.raininaugust26.prune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.raininaugust26.prune.ui.PruneAppNav
import com.raininaugust26.prune.ui.theme.PruneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PruneTheme { PruneAppNav() } }
    }
}
