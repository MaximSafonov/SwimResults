package com.msafonov.swimresults.data.repository

import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.data.model.TimeComponents.Companion.toDuration
import com.msafonov.swimresults.data.storage.RankTable
import com.msafonov.swimresults.data.storage.StopWatchStorage
import com.msafonov.swimresults.checkresult.api.CheckResult
import kotlin.time.Duration

class CheckResultRepository(
    private val rankTableStorage: RankTable,
    private val stopWatchStorage: StopWatchStorage
) : CheckResult {
    override val stopWatchTime = stopWatchStorage.stopTime
    override fun dropTime() = stopWatchStorage.dropTime()

    override fun rankTable(type: RankTableType) = rankTableStorage.rankTable(type)

    fun Map<Duration, SwimmingRank>.checkResult(targetValue: TimeComponents): CheckedResult? {
        return try {
            val sortedSet = this.keys.toSet()
            val nextRank = sortedSet.filter { it < targetValue.toDuration() }.maxOrNull()
            val currentRank = sortedSet.filter { it >= targetValue.toDuration() }.minOrNull()

            CheckedResult(
                currentRank = currentRank?.let { this.getValue(it) },
                nextRank = nextRank?.let { this.getValue(it) },
                toNextRank = nextRank?.let {
                    TimeComponents.calculateDiff(
                        TimeComponents.durationToComponents(it), targetValue
                    )
                },
                toPreviousRank = currentRank?.let {
                    TimeComponents.calculateDiff(
                        TimeComponents.durationToComponents(it), targetValue
                    )
                },
            )

        } catch (e: Exception) {
            return null
        }
    }

    override fun checkResult(
        eventTable: List<SwimEvent>,
        distance: Int,
        swimStyle: SwimStyle,
        result: String
    ): CheckedResult? {
        val table = eventTable.firstOrNull {
            it.distance == distance && it.style == swimStyle
        }?.results ?: return null

        val checkedValue = TimeComponents(
            minutes = result.substringBefore(':').toComponent(),
            seconds = result.substringAfter(':').substringBefore('.').toComponent(),
            milliseconds = result.substringAfter('.').toComponent(),
        )
        return table.checkResult(checkedValue)
    }

    fun String.toComponent() = this.toLongOrNull() ?: 0L

}