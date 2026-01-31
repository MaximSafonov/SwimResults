package com.msafonov.swimresults.data.model

import kotlin.time.Duration

data class SwimEvent(
    val distance: Int,
    val style: SwimStyle,
    val results: Map<Duration, SwimmingRank>
)