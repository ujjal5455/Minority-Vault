package com.example.minorityvault.protocol

import com.example.minorityvault.engine.Ballot
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.Vote
import com.example.minorityvault.engine.VoteTotals

// Engine ↔ wire conversions for the small building blocks. Every toDto() has a matching toEngine().

fun Phase.toWire(): WirePhase = when (this) {
    Phase.Lobby -> WirePhase.LOBBY
    Phase.Question -> WirePhase.QUESTION
    Phase.Negotiation -> WirePhase.NEGOTIATION
    Phase.Vote -> WirePhase.VOTE
    Phase.FlipWindow -> WirePhase.FLIP_WINDOW
    Phase.Reveal -> WirePhase.REVEAL
    Phase.Settlement -> WirePhase.SETTLEMENT
    Phase.GameOver -> WirePhase.GAME_OVER
}

fun WirePhase.toEngine(): Phase = when (this) {
    WirePhase.LOBBY -> Phase.Lobby
    WirePhase.QUESTION -> Phase.Question
    WirePhase.NEGOTIATION -> Phase.Negotiation
    WirePhase.VOTE -> Phase.Vote
    WirePhase.FLIP_WINDOW -> Phase.FlipWindow
    WirePhase.REVEAL -> Phase.Reveal
    WirePhase.SETTLEMENT -> Phase.Settlement
    WirePhase.GAME_OVER -> Phase.GameOver
}

fun VoteTotals.toDto() = VoteTotalsDto(yes, no)

fun VoteTotalsDto.toEngine() = VoteTotals(yes, no)

fun Ballot.toDto() = BallotDto(selection, doubled, locked)

fun BallotDto.toEngine() = Ballot(selection, doubled, locked)

fun Vote.toDto() = VoteDto(choice, doubled, flipped)

fun VoteDto.toEngine() = Vote(choice, doubled, flipped)
