package com.msafonov.swimresults.data.local

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath

internal const val SETTINGS_DATASTORE_FILE_NAME = "settings.preferences_pb"

internal class SettingsDataStoreOwner(
    val dataStore: DataStore<Preferences>,
    val scope: CoroutineScope
)

private object ProcessSettingsDataStore {
    val owner: SettingsDataStoreOwner by lazy {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        SettingsDataStoreOwner(
            dataStore = PreferenceDataStoreFactory.createWithPath(
                scope = scope,
                produceFile = { settingsDataStorePath().toPath() }
            ),
            scope = scope
        )
    }
}

internal fun getSettingsDataStoreOwner(): SettingsDataStoreOwner =
    ProcessSettingsDataStore.owner

internal expect fun settingsDataStorePath(): String

@Composable
internal expect fun InitializeSettingsStorage()
