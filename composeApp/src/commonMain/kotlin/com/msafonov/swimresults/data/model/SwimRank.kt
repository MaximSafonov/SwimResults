package com.msafonov.swimresults.data.model

sealed class SwimRank {
    abstract val swimmer: Swimmer
    abstract val swimStyle: SwimStyle
    abstract val distance: Int
    abstract val poolType: PoolType
    abstract val result: TimeComponents

    data class ThirdYoung(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents,
    ) : SwimRank()

    data class SecondYoung(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class FirstYoung(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class Third(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class Second(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class First(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class CandidateMasterOfSports(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

    data class MasterOfSports(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()


    data class InternationalMasterOfSports(
        override val swimmer: Swimmer,
        override val swimStyle: SwimStyle,
        override val distance: Int,
        override val poolType: PoolType,
        override val result: TimeComponents
    ) : SwimRank()

}

enum class SwimmingRank {
    ThirdJunior,
    SecondJunior,
    FirstJunior,
    Third,
    Second,
    First,
    CandidateMasterOfSports,
    MasterOfSports,
    InternationalMasterOfSports;
}