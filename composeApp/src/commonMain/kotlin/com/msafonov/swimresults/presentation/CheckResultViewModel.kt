package com.msafonov.swimresults.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.SwimmingRank
import com.msafonov.swimresults.data.model.TimeComponents
import com.msafonov.swimresults.data.model.TimeComponents.Companion.toDuration
import com.msafonov.swimresults.data.storage.RankTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration

class CheckResultViewModel {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var isDarkTheme: MutableStateFlow<Boolean> = MutableStateFlow(false)
    fun updateTheme(input: Boolean) {
        isDarkTheme.value = input
    }

    var minutes: String by mutableStateOf("")
        private set

    fun updateMinutes(input: String) {
        minutes = input
    }

    var seconds: String by mutableStateOf("")
        private set

    fun updateSeconds(input: String) {
        seconds = input
    }

    var milliseconds: String by mutableStateOf("")
        private set

    fun updateMilliseconds(input: String) {
        milliseconds = input
    }

    private val _poolType = MutableStateFlow<PoolType>(PoolType.Short)
    val poolType = _poolType.asStateFlow()

    fun checkPool(poolType: PoolType) {
        _poolType.value = poolType
    }

    private val _swimmer = MutableStateFlow<Swimmer>(Swimmer.Women)
    val swimmer = _swimmer.asStateFlow()

    fun setSwimmer(swimmer: Swimmer) {
        _swimmer.value = swimmer
    }

    private val _swimStyle = MutableStateFlow<SwimStyle>(SwimStyle.FREESTYLE)
    val swimStyle = _swimStyle.asStateFlow()

    fun setSwimStyle(swimStyle: SwimStyle) {
        _swimStyle.value = swimStyle

        when (swimStyle) {
            SwimStyle.MEDLEY -> {
                if (poolType.value == PoolType.Short && !Constants.MEDLEY_LIST_SHORT.contains(
                        distance.value
                    )
                ) {
                    setDistance(Constants.MEDLEY_LIST_SHORT.first())
                }

                if (poolType.value == PoolType.Long && !Constants.MEDLEY_LIST_LONG.contains(distance.value)) {
                    setDistance(Constants.MEDLEY_LIST_LONG.first())
                }
            }

            SwimStyle.BREASTSTROKE, SwimStyle.BUTTERFLY, SwimStyle.BACKSTROKE -> {
                if (!Constants.COMMON_LIST.contains(distance.value)) setDistance(Constants.COMMON_LIST.first())
            }

            else -> Unit
        }
    }

    val distanceList: Flow<List<Int>> = combine(swimStyle, poolType) { style, pool ->
        when (style) {
            SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
            SwimStyle.MEDLEY -> {
                if (pool == PoolType.Short) Constants.MEDLEY_LIST_SHORT
                else Constants.MEDLEY_LIST_LONG
            }

            else -> Constants.COMMON_LIST
        }
    }

    private val _distance = MutableStateFlow(Constants.COMMON_LIST.first())
    val distance = _distance.asStateFlow()
    fun setDistance(distance: Int) {
        _distance.value = distance
    }

    fun checkResult() {
        val table = when (poolType.value) {
            PoolType.Short -> {
                if (swimmer.value == Swimmer.Women) RankTable.shortWaterWomen
                else RankTable.shortWaterMen
            }

            PoolType.Long -> {
                if (swimmer.value == Swimmer.Women) RankTable.longWaterWomen
                else RankTable.longWaterMen
            }
        }

        val checkedValue = TimeComponents(
            minutes = minutes.toComponent(),
            seconds = seconds.toComponent(),
            milliseconds = milliseconds.toComponent(),
        )

        val swimEvent =
            table.firstOrNull {
                it.distance == distance.value && it.style == swimStyle.value
            }?.results?.findKeyNearestToValue(checkedValue.toDuration())

        val next = swimEvent?.second?.second?.let {
            TimeComponents.calculateDiff(
                TimeComponents.durationToComponents(it), checkedValue
            )
        }?.toString() ?: "ТЫ ПРОСТО КОСМОС"

        _result.value =
            "Твой разряд: ${swimEvent?.first?.text ?: "не айс чета"} \nМолодец, так держать =)\n\nДо следующего разряда:\n$next"

    }

    fun Map<Duration, SwimmingRank>.findKeyNearestToValue(targetValue: Duration): Pair<SwimmingRank?, Pair<SwimmingRank?, Duration?>>? {
        return try {
            val sortedSet = this.keys.toSet()
            val smaller = sortedSet.filter { it < targetValue }.maxOrNull()
            val larger = sortedSet.filter { it >= targetValue }.minOrNull()
            Pair(
                larger?.let { this.getValue(it) },
                Pair(smaller?.let { this.getValue(it) }, smaller)
            )
        } catch (e: Exception) {
            return null
        }
    }


    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()

    fun String.toComponent() = this.toLongOrNull() ?: 0L


}