package org.pashri.soundcheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.pashri.soundcheck.ui.SoundcheckApp
import org.pashri.soundcheck.ui.components.Tab
import org.pashri.soundcheck.ui.components.tabForRoute
import org.pashri.soundcheck.ui.theme.SoundcheckTheme

/**
 * The single activity hosting every Soundcheck screen. Tapping the playback notification
 * (its intent carries `FLAG_ACTIVITY_SINGLE_TOP` and `FLAG_ACTIVITY_CLEAR_TOP`) delivers
 * [onNewIntent] to the running instance instead of starting a second one, and asks it to
 * open the Warm-up tab.
 */
class MainActivity : ComponentActivity() {
    private var openTab by mutableStateOf<Tab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh instance should act on the extra; a recreation (rotation, dark mode,
        // a font or locale change) must not re-open the Warm-up over whatever tab was showing.
        if (savedInstanceState == null) {
            openTab = tabForRoute(intent.getStringExtra(EXTRA_OPEN_TAB))
        }
        val container = (application as SoundcheckApplication).container
        setContent {
            SoundcheckTheme {
                SoundcheckApp(
                    container = container,
                    openTab = openTab,
                    onTabOpened = {
                        openTab = null
                        intent.removeExtra(EXTRA_OPEN_TAB)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTab = tabForRoute(intent.getStringExtra(EXTRA_OPEN_TAB))
    }

    /** Intent extras [MainActivity] understands. */
    companion object {
        /** The tab to open, by [Tab.route]; the playback notification opens the Warm-up. */
        const val EXTRA_OPEN_TAB: String = "org.pashri.soundcheck.extra.OPEN_TAB"
    }
}
