package com.msafonov.swimresults.data.local

import androidx.compose.runtime.Composable
import platform.Foundation.NSHomeDirectory

internal actual fun settingsDataStorePath(): String =
    "${NSHomeDirectory()}/Documents/$SETTINGS_DATASTORE_FILE_NAME"

@Composable
actual fun InitializeSettingsStorage() = Unit
