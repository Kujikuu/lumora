package com.iptvcinema.tv.core.epg

/**
 * How much past guide data is kept. Long enough to list catch-up programmes for providers
 * that archive a few days, short enough to keep the programs table small.
 */
object EpgRetention {
    const val PAST_MS = 72L * 60 * 60 * 1000
}
