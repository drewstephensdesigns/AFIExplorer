package com.drewcodesit.afiexplorer.utils.objects

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This DataStore is used to persist lightweight UI-related preferences
// that should survive configuration changes and process death,
// such as the user's selected sort order.
val Context.dataStore by preferencesDataStore("library_prefs")

// Represents the available sort modes for the library / favorites list.
// This enum is stored as a String in DataStore using its [name] value.
enum class LibrarySortMode {
    TITLE,
    NUMBER
}

// Singleton object responsible for reading and writing library-related
// UI preferences using Preferences DataStore.
object LibraryPrefs {

    private val SORT_MODE_KEY =
        stringPreferencesKey("library_sort_mode")

    // Persists the user's selected [LibrarySortMode] to DataStore.
    // This function is `suspend` and must be called from a coroutine.
    suspend fun setSortMode(context: Context, mode: LibrarySortMode) {
        context.dataStore.edit { prefs ->
            prefs[SORT_MODE_KEY] = mode.name
        }
    }

    // Returns a cold [Flow] that emits the currently saved [LibrarySortMode]
    fun sortModeFlow(context: Context): Flow<LibrarySortMode> =
        context.dataStore.data.map { prefs ->
            LibrarySortMode.valueOf(
                prefs[SORT_MODE_KEY] ?: LibrarySortMode.TITLE.name
            )
        }
}