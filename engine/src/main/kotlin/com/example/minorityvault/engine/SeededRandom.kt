package com.example.minorityvault.engine

import java.util.Collections

/**
 * SplitMix64. Unlike kotlin.random.Random, its sequence is fixed forever, so a seed replays the same game on any
 * device and Kotlin version. The engine keeps [state] inside [GameState]; nothing else may hold game randomness.
 */
class SeededRandom(seed: Long) {
    var state: Long = seed
        private set

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    /** An integer in `[0, bound)`. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** True with probability [percent] / 100. */
    fun chance(percent: Int): Boolean = nextInt(PERCENT) < percent

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    fun <T> shuffled(items: List<T>): List<T> {
        val result = items.toMutableList()
        for (i in result.lastIndex downTo 1) {
            Collections.swap(result, i, nextInt(i + 1))
        }
        return result
    }

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        const val MIX_1 = -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        const val MIX_2 = -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        const val SHIFT_1 = 30
        const val SHIFT_2 = 27
        const val SHIFT_3 = 31
    }
}
