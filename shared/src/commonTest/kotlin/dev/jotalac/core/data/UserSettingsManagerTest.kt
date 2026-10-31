package dev.jotalac.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jotalac.core.domain.AppFontFamily
import dev.jotalac.core.domain.AppLanguage
import dev.jotalac.core.domain.AppThemeAccentColor
import dev.jotalac.core.domain.AppThemeMode
import dev.jotalac.core.domain.GitConflictResolutionStrategy
import dev.jotalac.core.domain.SpellcheckLanguage
import dev.jotalac.testing.fakes.InMemoryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserSettingsManagerTest {

    private val dataStore = InMemoryDataStore()
    private val settings = UserSettingsManager(dataStore)

    private suspend fun current() = settings.userSettingsStateFlow.first()

    @Test
    fun `a fresh install uses the defaults`() = runTest {
        assertEquals(UserSettingsState(), current())
    }

    @Test
    fun `every setting is saved under its own key`() = runTest {
        // a key shared by two settings would make one overwrite the other
        settings.setThemeMode(AppThemeMode.entries.last())
        settings.setThemeAccentColor(AppThemeAccentColor.entries.last())
        settings.setFont(AppFontFamily.entries.last())
        settings.setLanguage(AppLanguage.GERMAN)
        settings.setGitConflictStrategy(GitConflictResolutionStrategy.REMOTE)
        settings.setUseDynamicColor(true)
        settings.setSpellcheckLanguage(SpellcheckLanguage.Of(AppLanguage.CZECH))
        settings.setEditorFullWidth(true)
        settings.setAutoSync(true)

        assertEquals(
            UserSettingsState(
                themeMode = AppThemeMode.entries.last(),
                themeAccentColor = AppThemeAccentColor.entries.last(),
                font = AppFontFamily.entries.last(),
                language = AppLanguage.GERMAN,
                gitConflictStrategy = GitConflictResolutionStrategy.REMOTE,
                useDynamicColor = true,
                spellcheckLanguage = SpellcheckLanguage.Of(AppLanguage.CZECH),
                editorFullWidth = true,
                autoSync = true,
            ),
            current(),
        )
    }

    @Test
    fun `the spellcheck can follow the app language again or be turned off`() = runTest {
        settings.setSpellcheckLanguage(SpellcheckLanguage.Of(AppLanguage.CZECH))

        settings.setSpellcheckLanguage(SpellcheckLanguage.SameAsApp)
        assertEquals(SpellcheckLanguage.SameAsApp, current().spellcheckLanguage)

        settings.setSpellcheckLanguage(SpellcheckLanguage.Off)
        assertEquals(SpellcheckLanguage.Off, current().spellcheckLanguage)
    }

    @Test
    fun `a saved value this version does not know falls back to the default`() = runTest {
        // e.g. written by a newer version of the app, or an option that was removed - must not crash
        dataStore.edit {
            it[stringPreferencesKey("theme_mode")] = "SEPIA"
            it[stringPreferencesKey("language")] = "KLINGON"
            it[stringPreferencesKey("conflict_strategy")] = "ASK_EVERY_TIME"
        }

        val state = current()

        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertEquals(AppLanguage.ENGLISH, state.language)
        assertEquals(GitConflictResolutionStrategy.MANUAL, state.gitConflictStrategy)
    }
}
