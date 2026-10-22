@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.msafonov.swimresults.data.local

import androidx.compose.runtime.Composable
import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

@Composable
actual fun InitializeSwimHistoryStorage() = Unit

actual fun createSwimHistoryDatabaseBuilder(): RoomDatabase.Builder<SwimHistoryDatabase> {
    val directory = "${NSHomeDirectory()}/Documents"
    return Room.databaseBuilder<SwimHistoryDatabase>("$directory/swim-history.db")
}
