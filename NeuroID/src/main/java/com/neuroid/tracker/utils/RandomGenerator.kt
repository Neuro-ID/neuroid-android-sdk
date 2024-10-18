package com.neuroid.tracker.utils

/**
 * because we cannot mock Math, we create a wrapper for the random number generator
 */
class RandomGenerator {
    fun getRandom(multiplier: Int): Double {
        return Math.random() * multiplier
    }
}