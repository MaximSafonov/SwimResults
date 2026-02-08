package com.msafonov.swimresults.data.model

data class CheckedResult(
    val currentRank: SwimmingRank?,
    val nextRank: SwimmingRank?,
    val toNextRank: TimeComponents?,
    val toPreviousRank: TimeComponents?,
)