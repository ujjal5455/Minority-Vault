package com.example.minorityvault.net

import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.protocol.LobbyPlayerDto
import kotlin.random.Random

/** Timings for connections. These aren't game rules, so they live here rather than in [GameConfig]. */
data class NetConfig(
    val minPlayers: Int = GameConfig.DEFAULT_MIN_PLAYERS,
    val maxPlayers: Int = GameConfig.DEFAULT_MAX_PLAYERS,
    /** How long a dropped player may take to come back to their seat (CLAUDE.md §7). */
    val rejoinWindowMillis: Long = 60_000,
    val heartbeatMillis: Long = 3_000,
    /** No message for this long counts as a drop, even if the radio hasn't noticed. */
    val silenceTimeoutMillis: Long = 12_000,
    /** A phone that connects but never says Join is let go after this. */
    val joinTimeoutMillis: Long = 10_000,
    val tickMillis: Long = 1_000,
    /** Lets a farewell message leave before the connection closes. */
    val farewellMillis: Long = 500,
    val maxNameLength: Int = 16,
)

data class LobbyPlayer(
    val id: PlayerId,
    val name: String,
    val isHost: Boolean,
    val isBot: Boolean,
    val connected: Boolean,
)

internal fun LobbyPlayer.toDto() = LobbyPlayerDto(id.value, name, isHost, isBot, connected)

internal fun LobbyPlayerDto.toLobbyPlayer() = LobbyPlayer(PlayerId(id), name, isHost, isBot, connected)

/** A host found while browsing. */
data class NearbyHost(val endpointId: String, val roomCode: String, val hostName: String)

/** Hosts advertise as "ROOM|Host name", so browsers can show both before connecting. */
internal object EndpointName {
    private const val SEPARATOR = '|'

    fun of(roomCode: String, hostName: String) = "$roomCode$SEPARATOR$hostName"

    fun parse(endpointId: String, name: String): NearbyHost? {
        val code = name.substringBefore(SEPARATOR, missingDelimiterValue = "")
        val host = name.substringAfter(SEPARATOR, missingDelimiterValue = "")
        return if (code.isNotEmpty() && host.isNotEmpty()) NearbyHost(endpointId, code, host) else null
    }
}

/** Short and easy to read aloud: no I, O, 0 or 1 to confuse. */
internal fun Random.roomCode(): String = String(CharArray(ROOM_CODE_LENGTH) { ROOM_CODE_LETTERS.random(this) })

/** The secret a player keeps to get their seat back after a drop. */
internal fun Random.playerToken(): String = nextBytes(TOKEN_BYTES).joinToString("") { "%02x".format(it) }

/** Trimmed, with runs of spaces squeezed, or null if empty or too long. */
fun cleanName(name: String, maxLength: Int): String? =
    name.trim().replace(WHITESPACE, " ").takeIf { it.isNotEmpty() && it.length <= maxLength }

private val WHITESPACE = Regex("\\s+")
private const val ROOM_CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"
private const val ROOM_CODE_LENGTH = 4
private const val TOKEN_BYTES = 16
