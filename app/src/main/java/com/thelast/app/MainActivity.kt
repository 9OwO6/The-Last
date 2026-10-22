package com.thelast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thelast.app.data.datastore.ThemeMode
import com.thelast.app.navigation.TheLastNavHost
import com.thelast.app.ui.theme.TheLastTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        (application as TheLastApplication).updates.check()
        (application as TheLastApplication).cloudSync.let { it.checkPair(); it.schedule() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by (application as TheLastApplication).settingsStore.themeMode.collectAsState(
                initial = ThemeMode.System,
            )
            TheLastTheme(themeMode = themeMode) {
                TheLastNavHost()
            }
        }
    }
}
