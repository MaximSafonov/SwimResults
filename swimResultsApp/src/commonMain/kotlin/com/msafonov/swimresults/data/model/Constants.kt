package com.msafonov.swimresults.data.model

object Constants {

    const val M50 = 50
    const val M100 = 100
    const val M200 = 200
    const val M400 = 400
    const val M800 = 800
    const val M1500 = 1500


    val COMMON_LIST = listOf(M50, M100, M200)
    val FREESTYLE_LIST = COMMON_LIST + listOf(M400, M800, M1500)
    val MEDLEY_LIST_SHORT = listOf(M100, M200, M400)
    val MEDLEY_LIST_LONG = listOf(M200, M400)

}