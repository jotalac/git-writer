package dev.jotalac

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.glance.action.ActionParameters
import androidx.lifecycle.lifecycleScope
import dev.jotalac.core.di.initFileKitAndroid
import dev.jotalac.feature.widget.WidgetNotesProvider
import dev.jotalac.widget.NotesWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        val NOTE_PATH_PARAM = ActionParameters.Key<String>("widget_note_path")
        val NEW_NOTE_PARAM = ActionParameters.Key<Boolean>("widget_new_note")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initFileKitAndroid(this)

        // don't reopen the widget note when the activity is recreated (rotation etc.)
        // or relaunched from recents with the old widget intent
        val launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (savedInstanceState == null && !launchedFromHistory) openFromWidget(intent)

        setContent {
            App()
        }

    }

    // when the app is running and widget note is clicked
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromWidget(intent)
    }

    // is called when the app is exited
    override fun onStop() {
        super.onStop()
        // notes or the active notebook may have changed - keep the home screen widget up to date
        (application as MainApplication).appScope.launch { NotesWidget.refresh(applicationContext) }
    }

    // the widget opens a note, or makes a new one when its "new note" button was tapped
    private fun openFromWidget(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(NEW_NOTE_PARAM.name, false)) {
            intent.removeExtra(NEW_NOTE_PARAM.name)
            lifecycleScope.launch { WidgetNotesProvider.createNote() }
            return
        }
        val notePath = intent.getStringExtra(NOTE_PATH_PARAM.name) ?: return
        intent.removeExtra(NOTE_PATH_PARAM.name)
        lifecycleScope.launch { WidgetNotesProvider.openNote(notePath) }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
