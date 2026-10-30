package com.msafonov.swimresults.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import kotlin.time.Duration

data class CheckResultFilters(
    val swimmer: Swimmer = Swimmer.Women,
    val poolType: PoolType = PoolType.Short,
    val swimStyle: SwimStyle = SwimStyle.FREESTYLE,
    val distance: Int = Constants.M50
) {
    val distances: List<Int>
        get() = when (swimStyle) {
            SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
            SwimStyle.MEDLEY -> if (poolType == PoolType.Short) {
                Constants.MEDLEY_LIST_SHORT
            } else Constants.MEDLEY_LIST_LONG
            else -> Constants.COMMON_LIST
        }

    val tableType: RankTableType
        get() = when (poolType) {
            PoolType.Short -> if (swimmer == Swimmer.Women) {
                RankTableType.ShortWaterWomen
            } else RankTableType.ShortWaterMan
            PoolType.Long -> if (swimmer == Swimmer.Women) {
                RankTableType.LongWaterWomen
            } else RankTableType.LongWaterMan
        }

    // Preserve a valid distance; every invalid selection falls back to the minimum.
    fun normalized(): CheckResultFilters = if (distance in distances) this else {
        copy(distance = distances.min())
    }
}

enum class StandardsStatus { Loading, Ready, Unavailable, Error }

data class QualificationStandard(val rank: SwimmingRank, val time: Duration?)

fun qualificationStandards(event: SwimEvent?): List<QualificationStandard> {
    val timesByRank = event?.results?.entries?.associate { (time, rank) -> rank to time }.orEmpty()
    return swimmingRankHierarchy.map { QualificationStandard(it, timesByRank[it]) }
}

data class CheckResultUiState(
    val filters: CheckResultFilters = CheckResultFilters(),
    val status: StandardsStatus = StandardsStatus.Loading,
    val eventTable: List<SwimEvent> = emptyList(),
    val currentEvent: SwimEvent? = null,
    val standards: List<QualificationStandard> = qualificationStandards(null),
    val timerInput: TextFieldValue = TextFieldValue(),
    val resultDetails: ResultDetailsUiState? = null,
    val resultExpanded: Boolean = false
) {
    val result: CheckedResult? get() = resultDetails?.checkedResult
    val showResult: Boolean get() = resultDetails != null
    val canCheckResult: Boolean
        get() = status == StandardsStatus.Ready && currentEvent != null &&
            timerInput.text.any { it.isDigit() }
}

internal fun CheckResultUiState.withTable(table: List<SwimEvent>): CheckResultUiState {
    val event = table.firstOrNull {
        it.distance == filters.distance && it.style == filters.swimStyle
    }
    return copy(
        status = if (event == null) StandardsStatus.Unavailable else StandardsStatus.Ready,
        eventTable = table,
        currentEvent = event,
        standards = qualificationStandards(event)
    )
}
