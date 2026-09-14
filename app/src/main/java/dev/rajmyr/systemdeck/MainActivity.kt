package dev.rajmyr.systemdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.rajmyr.systemdeck.feature.shell.SystemDeckApp
import dev.rajmyr.systemdeck.feature.widget.SystemDeckWidgetProvider
import dev.rajmyr.systemdeck.feature.widget.WidgetRefreshScheduler
import dev.rajmyr.systemdeck.ui.theme.SystemDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Existing widgets must also be scheduled after an app upgrade where
        // AppWidgetProvider.onEnabled() will not necessarily run again.
        WidgetRefreshScheduler.syncWithInstalledWidgets(applicationContext)
        SystemDeckWidgetProvider.requestUpdate(applicationContext)

        setContent {
            SystemDeckTheme {
                SystemDeckApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SystemDeckWidgetProvider.requestUpdate(applicationContext)
    }
}
