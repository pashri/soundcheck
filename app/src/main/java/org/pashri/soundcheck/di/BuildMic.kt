package org.pashri.soundcheck.di

import android.content.Intent
import org.pashri.soundcheck.audio.MicInput

/**
 * The microphone as this build type provides it: `buildMic()` in the debug source set can
 * swap in a demo tone for emulator screenshots; the release one is always the real
 * microphone.
 */
interface BuildMic {
    /** The microphone every tool listens through. */
    val mic: MicInput

    /**
     * Reads any launch options for the microphone from the activity's intent.
     *
     * @param intent the intent that started or reopened the activity.
     */
    fun onLaunch(intent: Intent)
}
