package com.msafonov.swimresults.data.model

data class SwimEvent(
    val distance: Int,
    val style: SwimStyle,
    val results: Map<SwimmingRank, TimeComponents>
)