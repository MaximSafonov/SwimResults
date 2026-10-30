package com.msafonov.swimresults.data.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.msafonov.swimresults.data.local.SettingsDataStoreOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import okio.IOException

interface SettingsStorage {
    val isDark: Flow<Boolean>
    fun toggleTheme()
}

internal class DataStoreSettingsStorage(
    private val owner: SettingsDataStoreOwner
): SettingsStorage {
    private val isDarkKey = booleanPreferencesKey("is_dark")

    override val isDark: Flow<Boolean> = owner.dataStore.data
        .retryWhen { cause, attempt ->
            if (cause is IOException && cause !is CorruptionException) {
                if (attempt == 0L) {
                    println("Unable to read theme settings; retrying.")
                }
                // Keep the last observed theme while transient reads fail.
                delay(1_000)
                true
            } else {
                false
            }
        }
        .map { preferences -> preferences[isDarkKey] ?: false }
        .distinctUntilChanged()

    override fun toggleTheme() {
        owner.scope.launch {
            try {
                owner.dataStore.edit { preferences ->
                    preferences[isDarkKey] = !(preferences[isDarkKey] ?: false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                // Retrying a toggle could invert an already committed value again.
                println("Unable to save theme settings: " + exception)
            }
        }
    }
}
