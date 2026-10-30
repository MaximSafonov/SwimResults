package com.msafonov.swimresults.checkresult.api

import com.msafonov.swimresults.data.model.CheckedResult
import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent
import com.msafonov.swimresults.data.model.SwimStyle
import kotlinx.coroutines.flow.Flow

interface CheckResult {
    val stopWatchTime: Flow<String?>
    fun dropTime()
    fun rankTable(type: RankTableType): List<SwimEvent>
    fun checkResult(eventTable: List<SwimEvent>, distance: Int, swimStyle: SwimStyle, result: String): CheckedResult?
}
