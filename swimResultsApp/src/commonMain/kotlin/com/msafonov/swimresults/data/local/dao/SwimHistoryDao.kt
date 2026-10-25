package com.msafonov.swimresults.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.msafonov.swimresults.data.local.entity.CompetitionEntity
import com.msafonov.swimresults.data.local.entity.SwimRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    @Query("SELECT * FROM swim_records WHERE id = :id")
    abstract suspend fun getSwim(id: Long): SwimRecordEntity?

    @Update
    abstract suspend fun updateSwim(entity: SwimRecordEntity): Int

    @Query("DELETE FROM swim_records WHERE id = :id")
    abstract suspend fun deleteSwim(id: Long): Int

    /** DAO projection never emits rows belonging to another athlete. Null is the device bucket. */
    open fun observeScopedHistory(athleteId: Long?): Flow<List<CompetitionWithSwims>> = observeHistory().map { groups ->
        groups.map { it.copy(swims = it.swims.filter { row -> row.athleteId == athleteId }) }
            .filter { it.swims.isNotEmpty() }
    }

    @Query("SELECT * FROM athletes WHERE id = :id AND accountId = :accountId")
    abstract suspend fun ownedLinkTarget(accountId: String, id: Long): com.msafonov.swimresults.data.local.entity.AthleteEntity?

    @Query("SELECT * FROM swim_records WHERE id IN (:ids)")
    abstract suspend fun selectedSwims(ids: List<Long>): List<SwimRecordEntity>

    @Query("UPDATE swim_records SET athleteId = :athleteId WHERE id IN (:ids) AND athleteId IS NULL")
    abstract suspend fun assignUnassigned(ids: List<Long>, athleteId: Long): Int

    @Transaction
    open suspend fun linkDeviceRecords(accountId: String, athleteId: Long, ids: List<Long>) {
        require(ids.isNotEmpty() && ids.size <= 200 && ids.toSet().size == ids.size)
        requireNotNull(ownedLinkTarget(accountId, athleteId)) { "Athlete is unavailable" }
        val rows = selectedSwims(ids)
        check(rows.size == ids.size && rows.map { it.id }.toSet() == ids.toSet() && rows.all { it.athleteId == null }) {
            "Selected records changed"
        }
        check(assignUnassigned(ids, athleteId) == ids.size) { "Selected records changed" }
    }

    @Transaction
    open suspend fun updateSwimWithCompetition(
        swim: SwimRecordEntity,
        selectedCompetitionId: Long?,
        competitionName: String,
        competitionDate: String
    ): Int {
        val current = getSwim(swim.id) ?: return 0
        val competition = if (selectedCompetitionId != null) {
            checkNotNull(getCompetition(selectedCompetitionId)) { "Competition no longer exists" }
        } else {
            getOrCreateCompetition(competitionName, competitionDate)
        }
        val updated = swim.copy(competitionId = competition.id)
        if (current == updated) return 1
        // Throw inside the transaction so a failed update cannot leave a new competition behind.
        check(updateSwim(updated) == 1) { "Swim no longer exists" }
        return 1
    }

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
