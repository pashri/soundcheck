package org.pashri.soundcheck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.warmup.nameProblem

/**
 * Asks for a name or label. Once something has been typed it says what is wrong with it,
 * and the confirm button stays greyed out until the name can be used.
 *
 * @param title e.g. "Rename programme".
 * @param initial the text to start from.
 * @param taken names the new one must not match, ignoring case.
 * @param confirmLabel e.g. "Rename".
 * @param onConfirm called with the trimmed name.
 * @param onDismiss called on Cancel or a tap outside.
 * @param capitalize false for Sound labels, which are usually lower case.
 */
@Composable
fun NameDialog(
    title: String,
    initial: String,
    taken: List<String>,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    capitalize: Boolean = true,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var touched by rememberSaveable { mutableStateOf(false) }
    val problem = nameProblem(name = text, taken = taken)
    val confirm = { if (problem == null) onConfirm(text.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    touched = true
                },
                singleLine = true,
                isError = touched && problem != null,
                supportingText = {
                    if (touched && problem != null) Text(text = problem.message)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = if (capitalize) {
                        KeyboardCapitalization.Sentences
                    } else {
                        KeyboardCapitalization.None
                    },
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = problem == null) { Text(text = confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
    )
}

/**
 * Asks before something is deleted.
 *
 * @param title e.g. "Delete Morning?".
 * @param text what goes with it.
 * @param confirmLabel e.g. "Delete".
 * @param onConfirm deletes.
 * @param onDismiss called on Cancel or a tap outside.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
    )
}

/**
 * A muted text button for a rare or destructive action, such as "Delete programme".
 *
 * @param text the label.
 * @param onClick what a tap does.
 * @param modifier modifier for the button.
 */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = ManuscriptType.button, color = Manuscript.colors.muted)
    }
}
