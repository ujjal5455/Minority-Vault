package com.example.minorityvault.protocol

/**
 * The wire version in every envelope. Phones must share [MAJOR] to play together. A new [MINOR] may only add
 * optional fields, which older phones ignore.
 */
object ProtocolVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val CURRENT = "$MAJOR.$MINOR"

    /** The major part of [version], or null if it isn't a version at all. */
    fun majorOf(version: String): Int? = version.substringBefore('.').toIntOrNull()
}
