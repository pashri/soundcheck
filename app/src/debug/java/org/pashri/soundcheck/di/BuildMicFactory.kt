package org.pashri.soundcheck.di

import android.content.Intent
import org.pashri.soundcheck.audio.AndroidMic
import org.pashri.soundcheck.audio.DemoToneMic

/**
 * The debug build's microphone: the real one, unless the launch intent carries
 * [EXTRA_DEMO_TUNE_HZ], which makes every tool hear a steady tone at that frequency instead.
 * For screenshots and emulator checks only, e.g.
 * `adb -e shell am start -n org.pashri.soundcheck.debug/org.pashri.soundcheck.MainActivity
 * --ef demoTuneHz 442.04`.
 *
 * @return the debug microphone.
 */
fun buildMic(): BuildMic = DemoToneBuildMic(mic = DemoToneMic(real = AndroidMic()))

/** The intent extra holding the demo tone's frequency in Hz, as a float. */
const val EXTRA_DEMO_TUNE_HZ: String = "demoTuneHz"

private class DemoToneBuildMic(override val mic: DemoToneMic) : BuildMic {
    override fun onLaunch(intent: Intent) {
        if (!intent.hasExtra(EXTRA_DEMO_TUNE_HZ)) return
        mic.toneHz = intent.getFloatExtra(EXTRA_DEMO_TUNE_HZ, 0f).toDouble()
    }
}
