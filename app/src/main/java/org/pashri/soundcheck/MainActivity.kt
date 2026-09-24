package org.pashri.soundcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.pashri.soundcheck.ui.SoundcheckApp
import org.pashri.soundcheck.ui.theme.SoundcheckTheme

/** The single activity hosting every Soundcheck screen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundcheckTheme {
                SoundcheckApp()
            }
        }
    }
}
