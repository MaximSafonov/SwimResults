package com.msafonov.swimresults.data.local

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

private object AndroidHistoryContext {
    var applicationContext: Context? = null
}

@Composable
actual fun InitializeSwimHistoryStorage() {
    AndroidHistoryContext.applicationContext = LocalContext.current.applicationContext
}

actual fun createSwimHistoryDatabaseBuilder(): RoomDatabase.Builder<SwimHistoryDatabase> {
    val context = checkNotNull(AndroidHistoryContext.applicationContext) {
        "Swim history storage must be initialized before Koin."
    }
    return Room.databaseBuilder(
        context,
        File(context.filesDir, "swim-history.db").absolutePath
    )
}
