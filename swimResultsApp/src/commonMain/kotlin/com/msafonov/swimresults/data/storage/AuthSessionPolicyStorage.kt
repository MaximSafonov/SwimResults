package com.msafonov.swimresults.data.storage

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.msafonov.swimresults.data.local.SettingsDataStoreOwner
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

/** Non-secret restore permission only; identity and credentials belong to the SDK. */
interface AuthSessionPolicyStorage {
    suspend fun restoreAllowed(): Boolean
    suspend fun setRestoreAllowed(allowed: Boolean)
}

@Single
internal class DataStoreAuthSessionPolicyStorage(private val owner: SettingsDataStoreOwner) : AuthSessionPolicyStorage {
    private val key = booleanPreferencesKey("vk_restore_allowed")
    override suspend fun restoreAllowed(): Boolean = owner.dataStore.data.first()[key] ?: false
    override suspend fun setRestoreAllowed(allowed: Boolean) {
        owner.dataStore.edit { it[key] = allowed }
    }
}
