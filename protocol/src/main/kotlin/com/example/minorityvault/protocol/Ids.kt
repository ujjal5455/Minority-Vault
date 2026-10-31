package com.example.minorityvault.protocol

import com.example.minorityvault.engine.PlayerId

// Player ids travel as plain strings.

internal fun <V, W> Map<PlayerId, V>.byWireId(convert: (V) -> W): Map<String, W> =
    entries.associate { (id, value) -> id.value to convert(value) }

internal fun <V, W> Map<String, V>.byPlayerId(convert: (V) -> W): Map<PlayerId, W> =
    entries.associate { (id, value) -> PlayerId(id) to convert(value) }

internal fun List<PlayerId>.toWireIds(): List<String> = map { it.value }

internal fun List<String>.toPlayerIds(): List<PlayerId> = map(::PlayerId)
