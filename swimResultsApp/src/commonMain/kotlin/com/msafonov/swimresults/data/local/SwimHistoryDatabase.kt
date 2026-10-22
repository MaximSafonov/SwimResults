package com.msafonov.swimresults.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.ConstructedBy
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.msafonov.swimresults.data.local.dao.SwimHistoryDao
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity

@Database(entities = [CompetitionEntity::class, SwimRecordEntity::class], version = 1, exportSchema = true)
@ConstructedBy(SwimHistoryDatabaseConstructor::class)
abstract class SwimHistoryDatabase : RoomDatabase() {
    abstract fun swimHistoryDao(): SwimHistoryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SwimHistoryDatabaseConstructor : RoomDatabaseConstructor<SwimHistoryDatabase>

expect fun createSwimHistoryDatabaseBuilder(): RoomDatabase.Builder<SwimHistoryDatabase>

@androidx.compose.runtime.Composable
expect fun InitializeSwimHistoryStorage()

fun buildSwimHistoryDatabase(): SwimHistoryDatabase = createSwimHistoryDatabaseBuilder()
    .setDriver(BundledSQLiteDriver())
    .build()
