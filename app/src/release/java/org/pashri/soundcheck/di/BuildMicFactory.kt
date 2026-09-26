package org.pashri.soundcheck.di

import android.content.Intent
import org.pashri.soundcheck.audio.AndroidMic
import org.pashri.soundcheck.audio.MicInput

/**
 * The release build's microphone: always the real one, with no launch options.
 *
 * @return the release microphone.
 */
fun buildMic(): BuildMic = RealBuildMic

private object RealBuildMic : BuildMic {
    override val mic: MicInput = AndroidMic()

    override fun onLaunch(intent: Intent) = Unit
}
