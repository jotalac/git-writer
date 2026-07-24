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

class AppPreferencesManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACTIVE_NOTEBOOK_ID_KEY = longPreferencesKey("active_notebook_id")
    }

    val activeNotebookIdFlow: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[ACTIVE_NOTEBOOK_ID_KEY]
        }

    suspend fun setActiveNotebook(id: Long) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_NOTEBOOK_ID_KEY] = id
        }
    }

    suspend fun clearActiveNotebook() {
        dataStore.edit { preferences ->
            preferences.remove(ACTIVE_NOTEBOOK_ID_KEY)
        }
    }
}