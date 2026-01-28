package com.msafonov.swimresults

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform