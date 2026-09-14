package dev.rajmyr.systemdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.rajmyr.systemdeck.feature.shell.SystemDeckApp
import dev.rajmyr.systemdeck.ui.theme.SystemDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SystemDeckTheme {
                SystemDeckApp()
            }
        }
    }
}
