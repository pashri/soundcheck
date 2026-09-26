package org.pashri.soundcheck.ui.warmup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import org.pashri.soundcheck.playback.PlaybackService

/**
 * What to call just before a Start. On Android 13 and later the first Start ever asks to show
 * notifications, for the lock-screen controls; playback goes ahead whatever the answer, and a
 * yes redraws the playback notification (the playing Programme's media card, or the
 * Metronome's). The Warm-up home, the Programme editor and the Metronome share it, so the
 * question is asked only once.
 *
 * @return asks for the notification permission when [shouldAskForNotifications] says to.
 */
@Composable
internal fun rememberNotificationPrompt(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) refreshPlaybackNotification(context) },
    )
    return {
        if (shouldAskForNotifications(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * True once ever, on Android 13 and later without the notification permission: a flag in
 * [PROMPT_PREFS] remembers that the question was asked, across launches. The first Start
 * asks, for the lock-screen controls; playback goes ahead whatever the answer.
 *
 * @param context any context.
 * @return whether to ask now.
 */
internal fun shouldAskForNotifications(context: Context): Boolean {
    if (!needsNotificationPermission(context)) return false
    val prefs = context.getSharedPreferences(PROMPT_PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean(ASKED_NOTIFICATIONS, false)) return false
    prefs.edit { putBoolean(ASKED_NOTIFICATIONS, true) }
    return true
}

/**
 * Asks the playback service to put its notification up again, now that it may show (as
 * the media card, or as the Metronome's). With neither a Programme loaded nor the
 * Metronome playing or paused, the service just stops again.
 */
private fun refreshPlaybackNotification(context: Context) {
    val intent = Intent(context, PlaybackService::class.java)
        .setAction(PlaybackService.ACTION_REFRESH)
    try {
        context.startService(intent)
    } catch (e: IllegalStateException) {
        Log.w(LOG_TAG, "Couldn't refresh the playback notification", e)
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private const val LOG_TAG = "NotificationPrompt"
private const val PROMPT_PREFS = "permission_prompts"
private const val ASKED_NOTIFICATIONS = "asked_notifications"
