package org.pashri.soundcheck.ui.warmup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.pashri.soundcheck.ui.components.AccentButton
import org.pashri.soundcheck.ui.components.LinkCard
import org.pashri.soundcheck.ui.components.ManuscriptIcons
import org.pashri.soundcheck.ui.components.OutlineButton
import org.pashri.soundcheck.ui.components.SectionLabel
import org.pashri.soundcheck.ui.theme.Manuscript
import org.pashri.soundcheck.ui.theme.ManuscriptType
import org.pashri.soundcheck.ui.theme.SerifFamily
import org.pashri.soundcheck.warmup.ProgrammeId

/**
 * Where the Warm-up home's links go. A link is null until its screen exists, and then its
 * button is left out.
 *
 * @property openPlaying opens the playing screen.
 * @property openSettings opens Settings.
 * @property editProgramme opens a Programme's editor.
 * @property openPatterns opens the Patterns library.
 * @property openSounds opens the Sounds library.
 */
data class HomeLinks(
    val openPlaying: () -> Unit,
    val openSettings: (() -> Unit)? = null,
    val editProgramme: ((ProgrammeId) -> Unit)? = null,
    val openPatterns: (() -> Unit)? = null,
    val openSounds: (() -> Unit)? = null,
)

/**
 * The Warm-up home, wired to its view model. On Android 13 and later the first Start ever
 * asks to show notifications, for the lock-screen controls; playback goes ahead whatever the
 * answer.
 *
 * @param factory builds the [WarmupHomeViewModel].
 * @param links where the home's buttons go.
 */
@Composable
fun WarmupHomeRoute(factory: ViewModelProvider.Factory, links: HomeLinks) {
    val viewModel: WarmupHomeViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val askForNotifications = rememberNotificationPrompt()
    val shown = state ?: return
    WarmupHomeScreen(
        state = shown,
        links = links,
        onStart = { id ->
            askForNotifications()
            if (viewModel.start(id)) links.openPlaying()
        },
        onNewProgramme = links.editProgramme?.let { edit ->
            { viewModel.newProgramme()?.let(edit) }
        },
        onDismissRestoredNotice = viewModel::dismissRestoredNotice,
    )
}

/**
 * The Warm-up home in the Manuscript design. Scrolls at large font and display sizes.
 *
 * @param state what to show.
 * @param links where the buttons go.
 * @param onStart plays a Programme.
 * @param onNewProgramme adds a Programme and opens it, or null while there is no editor.
 * @param onDismissRestoredNotice dismisses the notice that a saved document was restored.
 */
@Composable
fun WarmupHomeScreen(
    state: WarmupHomeUiState,
    links: HomeLinks,
    onStart: (ProgrammeId) -> Unit,
    onNewProgramme: (() -> Unit)?,
    onDismissRestoredNotice: () -> Unit = {},
) {
    val colors = Manuscript.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        HomeHeader(onOpenSettings = links.openSettings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            state.restoredNotice?.let {
                RestoredNotice(text = it, onDismiss = onDismissRestoredNotice)
            }
            state.nowPlaying?.let {
                LinkCard(label = it.label, value = it.title, onClick = links.openPlaying)
            }
            LinkCard(label = "YOUR RANGE", value = state.rangeLabel, onClick = links.openSettings)
            state.saveProblem?.let {
                Text(text = it, style = ManuscriptType.body, color = colors.accentText)
            }
            SectionLabel(text = "PROGRAMMES", modifier = Modifier.padding(top = 4.dp))
            if (state.programmes.isEmpty()) {
                Text(
                    text = "No programmes yet.",
                    style = ManuscriptType.body,
                    color = colors.muted,
                )
            }
            state.programmes.forEach { card ->
                ProgrammeEntry(
                    card = card,
                    onEdit = links.editProgramme?.let { edit -> { edit(card.id) } },
                    onStart = { onStart(card.id) },
                )
            }
            onNewProgramme?.let {
                OutlineButton(
                    text = "+ New programme",
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LibraryTiles(state = state, links = links)
        }
    }
}

@Composable
private fun RestoredNotice(text: String, onDismiss: () -> Unit) {
    val colors = Manuscript.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(colors.faint)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = ManuscriptType.body,
            color = colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Dismiss",
            style = ManuscriptType.button,
            color = colors.ink,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(start = 12.dp),
        )
    }
}

@Composable
private fun HomeHeader(onOpenSettings: (() -> Unit)?) {
    val colors = Manuscript.colors
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Warm-up",
                style = ManuscriptType.screenTitle,
                color = colors.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (onOpenSettings != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .semantics { contentDescription = "Settings" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ManuscriptIcons.Settings,
                        contentDescription = null,
                        tint = colors.ink,
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

@Composable
private fun ProgrammeEntry(card: ProgrammeCard, onEdit: (() -> Unit)?, onStart: () -> Unit) {
    val colors = Manuscript.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.name,
                style = PROGRAMME_NAME,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = summary(card = card, ink = colors.ink),
                style = ManuscriptType.body,
                color = colors.muted,
            )
        }
        card.problem?.let {
            Text(text = it, style = ManuscriptType.body, color = colors.accentText)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onEdit != null) {
                OutlineButton(
                    text = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.widthIn(min = 96.dp),
                    description = "Edit ${card.name}",
                )
            }
            AccentButton(
                text = "Start",
                onClick = onStart,
                modifier = Modifier.weight(1f),
                icon = ManuscriptIcons.Play,
                enabled = card.canStart,
                description = "Start ${card.name}",
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.rule)
    }
}

@Composable
private fun LibraryTiles(state: WarmupHomeUiState, links: HomeLinks) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LinkCard(
            label = "LIBRARY",
            value = "Patterns ${state.patternCount}",
            onClick = links.openPatterns,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        LinkCard(
            label = "LIBRARY",
            value = "Sounds ${state.soundCount}",
            onClick = links.openSounds,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

private fun summary(card: ProgrammeCard, ink: Color): AnnotatedString = buildAnnotatedString {
    if (card.sounds.isNotEmpty()) {
        val soundStyle = SpanStyle(
            fontFamily = SerifFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            color = ink,
        )
        withStyle(style = soundStyle) { append(card.sounds) }
        append(" · ")
    }
    append(card.stepsLabel)
}

private val PROGRAMME_NAME: TextStyle =
    ManuscriptType.screenTitle.copy(fontSize = 34.sp, lineHeight = 36.sp)
