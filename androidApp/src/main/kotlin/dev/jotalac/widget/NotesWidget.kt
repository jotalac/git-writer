package dev.jotalac.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.jotalac.MainActivity
import dev.jotalac.R
import dev.jotalac.feature.widget.WidgetNote
import dev.jotalac.feature.widget.WidgetNotebook
import dev.jotalac.feature.widget.WidgetNotesProvider
import kotlinx.coroutines.flow.catch

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}

private sealed interface NotesWidgetState {
    data object Loading : NotesWidgetState
    data class Loaded(val notebook: WidgetNotebook?) : NotesWidgetState
}

class NotesWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        // bumped on every refresh - glance keeps the session (composition) alive between updates,
        // so update() alone only recomposes and would never reload the notes
        private val REFRESH_TOKEN_KEY = longPreferencesKey("refresh_token")

        suspend fun refresh(context: Context) {
            GlanceAppWidgetManager(context)
                .getGlanceIds(NotesWidget::class.java)
                .forEach { refresh(context, it) }
        }

        suspend fun refresh(context: Context, glanceId: GlanceId) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[REFRESH_TOKEN_KEY] = System.currentTimeMillis()
            }
            NotesWidget().update(context, glanceId)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val refreshToken = currentState(REFRESH_TOKEN_KEY) ?: 0L

            // load the notes inside the composition, so it reacts to notebook changes and refreshes
            val state by produceState<NotesWidgetState>(NotesWidgetState.Loading, refreshToken) {
                WidgetNotesProvider.activeNotebookFlow()
                    .catch { emit(null) }
                    .collect { value = NotesWidgetState.Loaded(it) }
            }

            GlanceTheme {
                NotesWidgetContent(context, state)
            }
        }
    }
}

class RefreshNotesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NotesWidget.refresh(context, glanceId)
    }
}

@Composable
private fun NotesWidgetContent(context: Context, state: NotesWidgetState) {
    val notebook = (state as? NotesWidgetState.Loaded)?.notebook

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        Header(title = notebook?.name ?: context.getString(R.string.app_name))
        Spacer(GlanceModifier.height(8.dp))

        when {
            state is NotesWidgetState.Loading -> CircularProgressIndicator()
            notebook == null -> EmptyMessage(context.getString(R.string.widget_no_notebook))
            notebook.notes.isEmpty() -> EmptyMessage(context.getString(R.string.widget_no_notes))
            else -> LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(notebook.notes, itemId = { it.path.hashCode().toLong() }) { note ->
                    Column {
                        NoteItem(note)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>())
        )
        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.widget_refresh),
            contentDescription = "Refresh",
            onClick = actionRunCallback<RefreshNotesAction>(),
            backgroundColor = null,
            contentColor = GlanceTheme.colors.onSurfaceVariant,
        )
        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.note_add),
            contentDescription = "New note",
            onClick = actionStartActivity<MainActivity>(
                actionParametersOf(MainActivity.NEW_NOTE_PARAM to true)
            ),
            backgroundColor = null,
            contentColor = GlanceTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteItem(note: WidgetNote) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(12.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(
                // this opens the note from the widget and starts the main activity and send the notes path to it
                actionStartActivity<MainActivity>(
                    actionParametersOf(MainActivity.NOTE_PATH_PARAM to note.path)
                )
            )
    ) {
        Text(
            text = note.title,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        )
        note.folder?.let { folder ->
            Text(
                text = folder,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
            )
        }
        if (note.preview.isNotEmpty()) {
            Text(
                text = note.preview,
                maxLines = 2,
                style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
        )
    }
}
