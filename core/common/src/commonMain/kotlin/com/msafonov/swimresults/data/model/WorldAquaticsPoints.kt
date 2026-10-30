package com.msafonov.swimresults.data.model

/**
 * World Aquatics 2026 individual-event bases, in centiseconds.
 * Source: https://resources.fina.org/fina/document/2026/09/05/9b48fb98-233c-4ce9-b67b-bc6163319bbd/Points-Base-times-SCM-and-LCM-2026_09.2026.pdf
 * SCM effective 2026-09-01 through 2027-08-31; LCM effective 2026-01-01 through 2026-12-31.
 * Refresh this dataset each season from https://www.worldaquatics.com/swimming/points.
 */
object WorldAquaticsPoints {
    const val SEASON = 2026
    private val bases = mapOf(
        // pool, swimmer, style, distance -> base centiseconds
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 50 to 1990,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 50 to 2283,
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 100 to 4484,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 100 to 4993,
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 200 to 9861,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 200 to 10936,
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 400 to 21225,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 400 to 23025,
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 800 to 44046,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 800 to 47400,
        PoolType.Short to Swimmer.Men to SwimStyle.FREESTYLE to 1500 to 84688,
        PoolType.Short to Swimmer.Women to SwimStyle.FREESTYLE to 1500 to 90824,
        PoolType.Short to Swimmer.Men to SwimStyle.BACKSTROKE to 50 to 2211,
        PoolType.Short to Swimmer.Women to SwimStyle.BACKSTROKE to 50 to 2523,
        PoolType.Short to Swimmer.Men to SwimStyle.BACKSTROKE to 100 to 4816,
        PoolType.Short to Swimmer.Women to SwimStyle.BACKSTROKE to 100 to 5402,
        PoolType.Short to Swimmer.Men to SwimStyle.BACKSTROKE to 200 to 10512,
        PoolType.Short to Swimmer.Women to SwimStyle.BACKSTROKE to 200 to 11733,
        PoolType.Short to Swimmer.Men to SwimStyle.BREASTSTROKE to 50 to 2495,
        PoolType.Short to Swimmer.Women to SwimStyle.BREASTSTROKE to 50 to 2837,
        PoolType.Short to Swimmer.Men to SwimStyle.BREASTSTROKE to 100 to 5528,
        PoolType.Short to Swimmer.Women to SwimStyle.BREASTSTROKE to 100 to 6236,
        PoolType.Short to Swimmer.Men to SwimStyle.BREASTSTROKE to 200 to 11952,
        PoolType.Short to Swimmer.Women to SwimStyle.BREASTSTROKE to 200 to 13250,
        PoolType.Short to Swimmer.Men to SwimStyle.BUTTERFLY to 50 to 2132,
        PoolType.Short to Swimmer.Women to SwimStyle.BUTTERFLY to 50 to 2372,
        PoolType.Short to Swimmer.Men to SwimStyle.BUTTERFLY to 100 to 4768,
        PoolType.Short to Swimmer.Women to SwimStyle.BUTTERFLY to 100 to 5271,
        PoolType.Short to Swimmer.Men to SwimStyle.BUTTERFLY to 200 to 10685,
        PoolType.Short to Swimmer.Women to SwimStyle.BUTTERFLY to 200 to 11932,
        PoolType.Short to Swimmer.Men to SwimStyle.MEDLEY to 100 to 4928,
        PoolType.Short to Swimmer.Women to SwimStyle.MEDLEY to 100 to 5511,
        PoolType.Short to Swimmer.Men to SwimStyle.MEDLEY to 200 to 10888,
        PoolType.Short to Swimmer.Women to SwimStyle.MEDLEY to 200 to 12163,
        PoolType.Short to Swimmer.Men to SwimStyle.MEDLEY to 400 to 23481,
        PoolType.Short to Swimmer.Women to SwimStyle.MEDLEY to 400 to 25548,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 50 to 2091,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 50 to 2361,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 100 to 4640,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 100 to 5171,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 200 to 10200,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 200 to 11223,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 400 to 21996,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 400 to 23418,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 800 to 45212,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 800 to 48412,
        PoolType.Long to Swimmer.Men to SwimStyle.FREESTYLE to 1500 to 87067,
        PoolType.Long to Swimmer.Women to SwimStyle.FREESTYLE to 1500 to 92048,
        PoolType.Long to Swimmer.Men to SwimStyle.BACKSTROKE to 50 to 2355,
        PoolType.Long to Swimmer.Women to SwimStyle.BACKSTROKE to 50 to 2686,
        PoolType.Long to Swimmer.Men to SwimStyle.BACKSTROKE to 100 to 5160,
        PoolType.Long to Swimmer.Women to SwimStyle.BACKSTROKE to 100 to 5713,
        PoolType.Long to Swimmer.Men to SwimStyle.BACKSTROKE to 200 to 11192,
        PoolType.Long to Swimmer.Women to SwimStyle.BACKSTROKE to 200 to 12314,
        PoolType.Long to Swimmer.Men to SwimStyle.BREASTSTROKE to 50 to 2595,
        PoolType.Long to Swimmer.Women to SwimStyle.BREASTSTROKE to 50 to 2916,
        PoolType.Long to Swimmer.Men to SwimStyle.BREASTSTROKE to 100 to 5688,
        PoolType.Long to Swimmer.Women to SwimStyle.BREASTSTROKE to 100 to 6413,
        PoolType.Long to Swimmer.Men to SwimStyle.BREASTSTROKE to 200 to 12548,
        PoolType.Long to Swimmer.Women to SwimStyle.BREASTSTROKE to 200 to 13755,
        PoolType.Long to Swimmer.Men to SwimStyle.BUTTERFLY to 50 to 2227,
        PoolType.Long to Swimmer.Women to SwimStyle.BUTTERFLY to 50 to 2443,
        PoolType.Long to Swimmer.Men to SwimStyle.BUTTERFLY to 100 to 4945,
        PoolType.Long to Swimmer.Women to SwimStyle.BUTTERFLY to 100 to 5460,
        PoolType.Long to Swimmer.Men to SwimStyle.BUTTERFLY to 200 to 11034,
        PoolType.Long to Swimmer.Women to SwimStyle.BUTTERFLY to 200 to 12181,
        PoolType.Long to Swimmer.Men to SwimStyle.MEDLEY to 200 to 11269,
        PoolType.Long to Swimmer.Women to SwimStyle.MEDLEY to 200 to 12570,
        PoolType.Long to Swimmer.Men to SwimStyle.MEDLEY to 400 to 24250,
        PoolType.Long to Swimmer.Women to SwimStyle.MEDLEY to 400 to 26365,
    ).mapKeys { (tuple, _) -> tuple }

    fun baseCentiseconds(pool: PoolType, swimmer: Swimmer, style: SwimStyle, distance: Int): Long? =
        bases[pool to swimmer to style to distance]?.toLong()

    fun score(pool: PoolType, swimmer: Swimmer, style: SwimStyle, distance: Int, timeCentiseconds: Long): Int? {
        if (timeCentiseconds <= 0) return null
        val base = baseCentiseconds(pool, swimmer, style, distance) ?: return null
        val ratio = base.toDouble() / timeCentiseconds.toDouble()
        val points = 1000.0 * ratio * ratio * ratio
        if (!points.isFinite() || points > Int.MAX_VALUE) return null
        return kotlin.math.floor(points).toInt()
    }
}
