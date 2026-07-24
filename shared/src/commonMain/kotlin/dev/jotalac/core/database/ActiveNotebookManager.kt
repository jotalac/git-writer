package dev.jotalac.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

const val DATA_STORE_FILE_NAME = "app_preferences.preferences_pb"

fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}

data class ActiveNotebookState(
    val notebookId: Long?,
    val notePath: String? = null
)

class ActiveNotebookManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACTIVE_NOTEBOOK_ID_KEY = longPreferencesKey("active_notebook_id")
        private val ACTIVE_NOTE_PATH_KEY = stringPreferencesKey("active_note_path")

    }

    val activeNotebookStateFlow: Flow<ActiveNotebookState?> = dataStore.data
        .map { preferences ->
            ActiveNotebookState(
                notebookId = preferences[ACTIVE_NOTEBOOK_ID_KEY],
                notePath = preferences[ACTIVE_NOTE_PATH_KEY]
            )
        }

    suspend fun setActiveNotebook(id: Long) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_NOTEBOOK_ID_KEY] = id
            preferences.remove(ACTIVE_NOTE_PATH_KEY)
        }
    }

    suspend fun setActiveNotePath(notePath: String) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_NOTE_PATH_KEY] = notePath
        }
    }



    suspend fun clearActiveNotebook() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_NOTEBOOK_ID_KEY)
            preferences.remove(ACTIVE_NOTE_PATH_KEY)
        }
    }

    suspend fun clearActiveNote() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_NOTE_PATH_KEY)
        }
    }
}