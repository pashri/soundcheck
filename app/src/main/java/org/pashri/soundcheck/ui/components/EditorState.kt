package org.pashri.soundcheck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** What an editor screen shows: nothing yet, a closing screen, or its content. */
sealed interface EditorState<out T> {
    /** The library hasn't loaded yet. */
    data object Loading : EditorState<Nothing>

    /** The thing being edited no longer exists, so the screen closes. */
    data object Gone : EditorState<Nothing>

    /**
     * The content.
     *
     * @property value what to show.
     */
    data class Ready<out T>(val value: T) : EditorState<T>
}

/**
 * An editor's state once everything it needs has loaded.
 *
 * @param content what to show, or null when the thing edited is gone.
 * @return [EditorState.Ready], or [EditorState.Gone] for null.
 */
fun <T> readyOrGone(content: T?): EditorState<T> =
    content?.let { EditorState.Ready(it) } ?: EditorState.Gone

/**
 * Shows an editor's content when it is ready, nothing while loading, and closes the screen
 * once when its subject is gone.
 *
 * @param state the editor's state.
 * @param onGone closes the screen.
 * @param content the screen for a ready state.
 */
@Composable
fun <T> EditorFrame(state: EditorState<T>, onGone: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        EditorState.Loading -> Unit
        EditorState.Gone -> LaunchedEffect(key1 = Unit) { onGone() }
        is EditorState.Ready -> content(state.value)
    }
}
