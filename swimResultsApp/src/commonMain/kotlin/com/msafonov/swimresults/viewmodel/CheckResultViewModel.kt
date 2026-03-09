package com.msafonov.swimresults.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.Constants
import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.repository.CheckResultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CheckResultViewModel(
    private val repository: CheckResultRepository
) : ViewModel() {

    private val _result = MutableStateFlow<CheckedResult?>(null)
    val result = _result.asStateFlow()

    private val _timerInput = MutableStateFlow(TextFieldValue(""))
    val timerInput = _timerInput.asStateFlow()
    fun onInputChanged(newValue: TextFieldValue) {
        repository.dropTime()
        _timerInput.value = newValue
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

    private val _distance = MutableStateFlow(Constants.COMMON_LIST.first())
    val distance = _distance.asStateFlow()
    fun setDistance(distance: Int) {
        _distance.value = distance
    }

    val stopWatchTime =
        repository.stopWatchTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(500), null)

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

    val eventTable: StateFlow<List<SwimEvent>> = combine(
        poolType,
        swimmer
    ) { pool, swimmer ->
        val type = when (pool) {
            PoolType.Short -> {
                if (swimmer == Swimmer.Women) RankTableType.ShortWaterWomen
                else RankTableType.ShortWaterMan
            }

            PoolType.Long -> {
                if (swimmer == Swimmer.Women) RankTableType.LongWaterWomen
                else RankTableType.ShortWaterMan
            }
        }
        repository.rankTable(type)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.rankTable(RankTableType.ShortWaterWomen)
    )

    val currentEvent: StateFlow<SwimEvent?> = combine(
        eventTable,
        distance,
        swimStyle,
    ) { table, distance, style ->
        table.firstOrNull { it.distance == distance && it.style == style }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val distanceList: StateFlow<List<Int>> = combine(swimStyle, poolType) { style, pool ->
        val list = when (style) {
            SwimStyle.FREESTYLE -> Constants.FREESTYLE_LIST
            SwimStyle.MEDLEY -> {
                if (pool == PoolType.Short) Constants.MEDLEY_LIST_SHORT
                else Constants.MEDLEY_LIST_LONG
            }

            else -> Constants.COMMON_LIST
        }
        if (!list.contains(_distance.value)) setDistance(list.first())

        list
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Constants.COMMON_LIST)

    fun checkResult() {
        _result.value = repository.checkResult(
            eventTable = eventTable.value,
            swimStyle = swimStyle.value,
            distance = distance.value,
            result = timerInput.value.text
        )
    }

}