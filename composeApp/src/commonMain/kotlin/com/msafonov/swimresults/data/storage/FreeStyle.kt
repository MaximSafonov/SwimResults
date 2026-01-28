package com.msafonov.swimresults.data.storage

import com.msafonov.swimresults.data.model.PoolType
import com.msafonov.swimresults.data.model.SwimRank
import com.msafonov.swimresults.data.model.SwimStyle
import com.msafonov.swimresults.data.model.Swimmer
import com.msafonov.swimresults.data.model.TimeComponents

object FREESTYLE {
    val collectionShortWater = listOf(
        SwimRank.ThirdYoung(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("55.05")
        ),
        SwimRank.SecondYoung(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("45.05")
        ),
        SwimRank.FirstYoung(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("35.05")
        ),
        SwimRank.Third(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("29.05")
        ),
        SwimRank.Second(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("26.85")
        ),
        SwimRank.First(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("24.45")
        ),
        SwimRank.CandidateMasterOfSports(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("23.20")
        ),
        SwimRank.MasterOfSports(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("22.45")
        ),
        SwimRank.InternationalMasterOfSports(
            swimmer = Swimmer.Men,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("21.18")
        ),
        SwimRank.ThirdYoung(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("59.05")
        ),
        SwimRank.SecondYoung(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("49.55")
        ),
        SwimRank.FirstYoung(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("39.55")
        ),
        SwimRank.Third(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("32.55")
        ),
        SwimRank.Second(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("30.55")
        ),
        SwimRank.First(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("27.85")
        ),
        SwimRank.CandidateMasterOfSports(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("26.55")
        ),
        SwimRank.MasterOfSports(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("25.75")
        ),
        SwimRank.InternationalMasterOfSports(
            swimmer = Swimmer.Women,
            swimStyle = SwimStyle.FREESTYLE,
            distance = 50,
            poolType = PoolType.Short,
            result = TimeComponents.from("24.13")
        ),
    )
}

//55.05	45.05	35.05	29.05	26.85	24.45	23.20	22.45	21.18

//        override val swimmer: Swimmer,
//        override val swimStyle: SwimStyle,
//        override val distance: Int,
//        override val poolType: PoolType,
//        override val timeComponents: TimeComponents