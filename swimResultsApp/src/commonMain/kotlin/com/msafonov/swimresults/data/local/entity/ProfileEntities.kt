package com.msafonov.swimresults.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index(value = ["provider", "providerUserId"], unique = true)])
data class AccountEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val providerUserId: String,
    val displayName: String,
    val activeAthleteId: Long? = null
)

@Entity(tableName = "athletes", foreignKeys = [ForeignKey(
    entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT
)], indices = [Index("accountId")])
data class AthleteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val name: String,
    val birthDate: String?,
    val sportsGender: String?,
    val rank: String? = null
)
