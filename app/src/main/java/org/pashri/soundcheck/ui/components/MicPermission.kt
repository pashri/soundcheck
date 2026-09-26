package org.pashri.soundcheck.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/**
 * Whether Soundcheck may use the microphone right now.
 *
 * @return true if the permission is granted.
 */
fun Activity.hasMicPermission(): Boolean =
    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Opens Soundcheck's page in the phone's Settings, where the microphone can be allowed. */
fun Activity.openAppSettings() {
    val uri = Uri.fromParts("package", packageName, null)
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    } catch (_: ActivityNotFoundException) {
        // No Settings app to open on this device; nothing more to do.
    }
}
