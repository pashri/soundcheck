package org.pashri.soundcheck

import android.app.Application
import org.pashri.soundcheck.di.AppContainer

/** Creates the [AppContainer] once per process. */
class SoundcheckApplication : Application() {
    /** Dependencies shared by every screen. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
