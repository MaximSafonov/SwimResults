package com.msafonov.swimresults.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import kotlinx.coroutines.flow.Flow

data class CompetitionWithSwims(
    @Embedded val competition: CompetitionEntity,
    @Relation(parentColumn = "id", entityColumn = "competitionId")
    val swims: List<SwimRecordEntity>
)

@Dao
abstract class SwimHistoryDao {
    @Transaction
    @Query("SELECT * FROM competitions ORDER BY date DESC, name COLLATE NOCASE ASC")
    abstract fun observeHistory(): Flow<List<CompetitionWithSwims>>

    @Query("SELECT * FROM competitions ORDER BY date DESC, name COLLATE NOCASE ASC")
    abstract fun observeCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE id = :id")
    abstract suspend fun getCompetition(id: Long): CompetitionEntity?

    @Query("SELECT * FROM competitions WHERE name = :name AND date = :date LIMIT 1")
    abstract suspend fun findCompetition(name: String, date: String): CompetitionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCompetition(entity: CompetitionEntity): Long

    @Insert
    abstract suspend fun insertSwim(entity: SwimRecordEntity): Long

    @Transaction
    open suspend fun getOrCreateCompetition(name: String, date: String): CompetitionEntity {
        val existing = findCompetition(name, date)
        if (existing != null) return existing
        val id = insertCompetition(CompetitionEntity(name = name, date = date))
        return if (id == -1L) checkNotNull(findCompetition(name, date))
        else checkNotNull(getCompetition(id))
    }

    @Transaction
    open suspend fun insertSwimWithCompetition(
        competitionName: String,
        competitionDate: String,
        swim: SwimRecordEntity
    ): Long {
        val competition = getOrCreateCompetition(competitionName, competitionDate)
        return insertSwim(swim.copy(competitionId = competition.id))
    }
}
