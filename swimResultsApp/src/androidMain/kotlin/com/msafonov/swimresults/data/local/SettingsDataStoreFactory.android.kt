package com.msafonov.swimresults.data.local

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File

private object AndroidSettingsContext {
    @Volatile
    var applicationContext: Context? = null
}

@Composable
internal actual fun InitializeSettingsStorage() {
    AndroidSettingsContext.applicationContext = LocalContext.current.applicationContext
}

internal actual fun settingsDataStorePath(): String {
    val context = checkNotNull(AndroidSettingsContext.applicationContext) {
        "Settings storage must be initialized before starting Koin."
    }
    return File(context.filesDir, SETTINGS_DATASTORE_FILE_NAME).absolutePath
}
