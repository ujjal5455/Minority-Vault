package com.example.minorityvault.protocol

import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PrivatePlayerView
import com.example.minorityvault.engine.PublicGameView
import com.example.minorityvault.engine.PublicPlayer
import com.example.minorityvault.engine.PublicPromise
import com.example.minorityvault.engine.QuestionId

fun PublicPlayer.toDto() = PublicPlayerDto(id.value, name, isBot, balance, isDebtor, brokenPromises, hasLocked)

fun PublicPlayerDto.toEngine() = PublicPlayer(PlayerId(id), name, isBot, balance, isDebtor, brokenPromises, hasLocked)

fun PublicPromise.toDto() = PublicPromiseDto(from.value, side)

fun PublicPromiseDto.toEngine() = PublicPromise(PlayerId(from), side)

fun PublicGameView.toDto() = PublicGameViewDto(
    phase = phase.toWire(),
    round = round,
    totalRounds = totalRounds,
    questionId = questionId?.value,
    twist = twist?.toDto(),
    isAllIn = isAllIn,
    baseFine = baseFine,
    contractFee = contractFee,
    pot = pot,
    players = players.map { it.toDto() },
    publicPromises = publicPromises.map { it.toDto() },
    flipWindowTotals = flipWindowTotals?.toDto(),
    reveal = reveal?.toDto(),
    winners = winners?.toWireIds(),
    finalHistory = finalHistory?.map { it.toDto() },
)

fun PublicGameViewDto.toEngine() = PublicGameView(
    phase = phase.toEngine(),
    round = round,
    totalRounds = totalRounds,
    questionId = questionId?.let(::QuestionId),
    twist = twist?.toEngine(),
    isAllIn = isAllIn,
    baseFine = baseFine,
    contractFee = contractFee,
    pot = pot,
    players = players.map { it.toEngine() },
    publicPromises = publicPromises.map { it.toEngine() },
    flipWindowTotals = flipWindowTotals?.toEngine(),
    reveal = reveal?.toEngine(),
    winners = winners?.toPlayerIds(),
    finalHistory = finalHistory?.map { it.toEngine() },
)

fun PrivatePlayerView.toDto() = PrivatePlayerViewDto(
    playerId = playerId.value,
    items = items,
    ballot = ballot?.toDto(),
    vote = vote?.toDto(),
    voteSoldTo = voteSoldTo?.value,
    controlledBallots = controlledBallots.byWireId { it.toDto() },
    controlledVotes = controlledVotes.byWireId { it.toDto() },
    isInsider = isInsider,
    insiderTotals = insiderTotals?.toDto(),
    contracts = contracts.map { it.toDto() },
    promisesMade = promisesMade.map { it.toDto() },
    promisesReceived = promisesReceived.map { it.toDto() },
    transfers = transfers.map { it.toDto() },
    judgedPromises = judgedPromises.map { it.toDto() },
)

fun PrivatePlayerViewDto.toEngine() = PrivatePlayerView(
    playerId = PlayerId(playerId),
    items = items,
    ballot = ballot?.toEngine(),
    vote = vote?.toEngine(),
    voteSoldTo = voteSoldTo?.let(::PlayerId),
    controlledBallots = controlledBallots.byPlayerId { it.toEngine() },
    controlledVotes = controlledVotes.byPlayerId { it.toEngine() },
    isInsider = isInsider,
    insiderTotals = insiderTotals?.toEngine(),
    contracts = contracts.map { it.toEngine() },
    promisesMade = promisesMade.map { it.toEngine() },
    promisesReceived = promisesReceived.map { it.toEngine() },
    transfers = transfers.map { it.toEngine() },
    judgedPromises = judgedPromises.map { it.toEngine() },
)
