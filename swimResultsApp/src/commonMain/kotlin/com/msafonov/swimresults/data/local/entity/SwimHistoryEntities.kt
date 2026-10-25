package com.msafonov.swimresults.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "competitions",
    indices = [Index(value = ["name", "date"], unique = true)]
)
data class CompetitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ISO-8601 date (yyyy-MM-dd). */
    val date: String
)

@Entity(
    tableName = "swim_records",
    foreignKeys = [ForeignKey(
        entity = CompetitionEntity::class,
        parentColumns = ["id"],
        childColumns = ["competitionId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = AthleteEntity::class, parentColumns = ["id"], childColumns = ["athleteId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("competitionId"), Index("swimDate"), Index("athleteId")]
)
data class SwimRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val competitionId: Long,
    /** ISO-8601 date (yyyy-MM-dd). */
    val swimDate: String,
    val distanceMeters: Int,
    val style: String,
    val gender: String,
    val poolLengthMeters: Int,
    val timeCentiseconds: Long,
    /** Historical snapshot; null means no qualification was achieved or available. */
    val qualification: String?,
    val athleteId: Long? = null
)
