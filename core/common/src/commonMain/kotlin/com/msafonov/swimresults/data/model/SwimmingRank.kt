package com.msafonov.swimresults.data.model

enum class SwimmingRank(val text: String) {
    ThirdJunior(Constants.THIRD_YOUNG),
    SecondJunior(Constants.SECOND_YOUNG),
    FirstJunior(Constants.FIRST_YOUNG),
    Third(Constants.THIRD),
    Second(Constants.SECOND),
    First(Constants.FIRST),
    CandidateMasterOfSports(Constants.CANDIDATE_MASTER),
    MasterOfSports(Constants.MASTER),
    InternationalMasterOfSports(Constants.WORLD_MASTER);
}