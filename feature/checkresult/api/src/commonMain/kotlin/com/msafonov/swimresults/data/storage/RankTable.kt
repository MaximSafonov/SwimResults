package com.msafonov.swimresults.data.storage

import com.msafonov.swimresults.data.model.RankTableType
import com.msafonov.swimresults.data.model.SwimEvent

interface RankTable{
    fun rankTable(type: RankTableType): List<SwimEvent>
}
