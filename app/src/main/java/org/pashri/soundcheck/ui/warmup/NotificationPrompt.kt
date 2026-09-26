package org.pashri.soundcheck.ui.warmup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * What to call just before a Start. On Android 13 and later the first Start ever asks to show
 * notifications, for the lock-screen controls; playback goes ahead whatever the answer. The
 * Warm-up home and the Programme editor share it, so the question is asked only once.
 *
 * @return asks for the notification permission when [shouldAskForNotifications] says to.
 */
@Composable
internal fun rememberNotificationPrompt(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
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

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private const val PROMPT_PREFS = "permission_prompts"
private const val ASKED_NOTIFICATIONS = "asked_notifications"
