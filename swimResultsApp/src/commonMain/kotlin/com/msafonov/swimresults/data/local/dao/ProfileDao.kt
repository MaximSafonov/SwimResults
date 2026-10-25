package com.msafonov.swimresults.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.msafonov.swimresults.data.local.entity.AccountEntity
import com.msafonov.swimresults.data.local.entity.AthleteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    abstract fun observeAccount(accountId: String): Flow<AccountEntity?>
    @Query("SELECT * FROM athletes WHERE accountId = :accountId ORDER BY id")
    abstract fun observeAthletes(accountId: String): Flow<List<AthleteEntity>>
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    abstract suspend fun getAccount(accountId: String): AccountEntity?
    @Query("SELECT * FROM athletes WHERE id = :id AND accountId = :accountId")
    abstract suspend fun ownedAthlete(accountId: String, id: Long): AthleteEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAccount(account: AccountEntity): Long
    @Query("UPDATE accounts SET displayName = :name WHERE id = :accountId")
    abstract suspend fun updateAccountName(accountId: String, name: String)
    @Insert
    abstract suspend fun insertAthlete(athlete: AthleteEntity): Long
    @Query("UPDATE athletes SET name = :name, birthDate = :birthDate, sportsGender = :gender, rank = :rank WHERE id = :id AND accountId = :accountId")
    abstract suspend fun updateAthlete(accountId: String, id: Long, name: String, birthDate: String?, gender: String?, rank: String?): Int
    @Query("UPDATE accounts SET activeAthleteId = :athleteId WHERE id = :accountId")
    abstract suspend fun updateActive(accountId: String, athleteId: Long): Int

    @Transaction
    open suspend fun ensureAccount(account: AccountEntity) {
        insertAccount(account)
        updateAccountName(account.id, account.displayName)
    }

    @Transaction
    open suspend fun selectAthlete(accountId: String, athleteId: Long) {
        requireNotNull(ownedAthlete(accountId, athleteId)) { "Athlete is unavailable" }
        if (getAccount(accountId)?.activeAthleteId != athleteId) check(updateActive(accountId, athleteId) == 1)
    }

    @Transaction
    open suspend fun saveAthlete(athlete: AthleteEntity): Long {
        val account = checkNotNull(getAccount(athlete.accountId))
        if (athlete.id != 0L) {
            check(updateAthlete(athlete.accountId, athlete.id, athlete.name, athlete.birthDate, athlete.sportsGender, athlete.rank) == 1)
            return athlete.id
        }
        val id = insertAthlete(athlete)
        if (account.activeAthleteId == null) check(updateActive(athlete.accountId, id) == 1)
        return id
    }
}
