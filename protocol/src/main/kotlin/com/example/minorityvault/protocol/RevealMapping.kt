package com.example.minorityvault.protocol

import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.QuestionId
import com.example.minorityvault.engine.RevealSequence
import com.example.minorityvault.engine.RevealStep
import com.example.minorityvault.engine.RoundSummary
import com.example.minorityvault.engine.Twist
import com.example.minorityvault.engine.TwistInfo

fun TwistInfo.toDto() = TwistInfoDto(id, exactMinoritySize)

fun TwistInfoDto.toEngine() = TwistInfo(id, exactMinoritySize)

fun Twist.toDto(): TwistDto = when (this) {
    is Twist.ExactMinority -> TwistDto.ExactMinority(size)
    Twist.SecondChance -> TwistDto.SecondChance
    is Twist.Insider -> TwistDto.Insider(player.value)
    Twist.RobinHood -> TwistDto.RobinHood
    Twist.NoTwist -> TwistDto.NoTwist
}

fun TwistDto.toEngine(): Twist = when (this) {
    is TwistDto.ExactMinority -> Twist.ExactMinority(size)
    TwistDto.SecondChance -> Twist.SecondChance
    is TwistDto.Insider -> Twist.Insider(PlayerId(player))
    TwistDto.RobinHood -> Twist.RobinHood
    TwistDto.NoTwist -> Twist.NoTwist
}

fun RevealStep.toDto(): RevealStepDto = when (this) {
    is RevealStep.VotesShown -> RevealStepDto.VotesShown(votes.byWireId { it.toDto() })
    is RevealStep.TotalsShown -> RevealStepDto.TotalsShown(totals.toDto())
    is RevealStep.MinorityFound -> RevealStepDto.MinorityFound(side, players.toWireIds())
    is RevealStep.TwistResolved -> RevealStepDto.TwistResolved(twist.toDto(), winners.toWireIds())
    is RevealStep.FinesPaid -> RevealStepDto.FinesPaid(fines.byWireId { it })
    is RevealStep.PotSplit -> RevealStepDto.PotSplit(winners.toWireIds(), share, potLeft)
    is RevealStep.PromisesBroken -> RevealStepDto.PromisesBroken(players.toWireIds())
    is RevealStep.DebtorsChanged -> RevealStepDto.DebtorsChanged(newDebtors.toWireIds(), recovered.toWireIds())
    is RevealStep.Settled -> RevealStepDto.Settled(balances.byWireId { it }, pot, previous.byWireId { it })
}

fun RevealStepDto.toEngine(): RevealStep = when (this) {
    is RevealStepDto.VotesShown -> RevealStep.VotesShown(votes.byPlayerId { it.toEngine() })
    is RevealStepDto.TotalsShown -> RevealStep.TotalsShown(totals.toEngine())
    is RevealStepDto.MinorityFound -> RevealStep.MinorityFound(side, players.toPlayerIds())
    is RevealStepDto.TwistResolved -> RevealStep.TwistResolved(twist.toEngine(), winners.toPlayerIds())
    is RevealStepDto.FinesPaid -> RevealStep.FinesPaid(fines.byPlayerId { it })
    is RevealStepDto.PotSplit -> RevealStep.PotSplit(winners.toPlayerIds(), share, potLeft)
    is RevealStepDto.PromisesBroken -> RevealStep.PromisesBroken(players.toPlayerIds())
    is RevealStepDto.DebtorsChanged -> RevealStep.DebtorsChanged(newDebtors.toPlayerIds(), recovered.toPlayerIds())
    is RevealStepDto.Settled -> RevealStep.Settled(balances.byPlayerId { it }, pot, previous.byPlayerId { it })
}

fun RevealSequence.toDto() = RevealSequenceDto(steps.map { it.toDto() })

fun RevealSequenceDto.toEngine() = RevealSequence(steps.map { it.toEngine() })

fun RoundSummary.toDto() = RoundSummaryDto(
    round = round,
    questionId = questionId.value,
    twist = twist.toDto(),
    votes = votes.byWireId { it.toDto() },
    totals = totals.toDto(),
    minoritySide = minoritySide,
    minorityPlayers = minorityPlayers.toWireIds(),
    winners = winners.toWireIds(),
    fines = fines.byWireId { it },
    winnings = winnings.byWireId { it },
    contracts = contracts.map { it.toDto() },
    transfers = transfers.map { it.toDto() },
    promises = promises.map { it.toDto() },
    newDebtors = newDebtors.toWireIds(),
    recoveredDebtors = recoveredDebtors.toWireIds(),
    potBefore = potBefore,
    potAfter = potAfter,
    balancesBefore = balancesBefore.byWireId { it },
    balancesAfter = balancesAfter.byWireId { it },
)

fun RoundSummaryDto.toEngine() = RoundSummary(
    round = round,
    questionId = QuestionId(questionId),
    twist = twist.toEngine(),
    votes = votes.byPlayerId { it.toEngine() },
    totals = totals.toEngine(),
    minoritySide = minoritySide,
    minorityPlayers = minorityPlayers.toPlayerIds(),
    winners = winners.toPlayerIds(),
    fines = fines.byPlayerId { it },
    winnings = winnings.byPlayerId { it },
    contracts = contracts.map { it.toEngine() },
    transfers = transfers.map { it.toEngine() },
    promises = promises.map { it.toEngine() },
    newDebtors = newDebtors.toPlayerIds(),
    recoveredDebtors = recoveredDebtors.toPlayerIds(),
    potBefore = potBefore,
    potAfter = potAfter,
    balancesBefore = balancesBefore.byPlayerId { it },
    balancesAfter = balancesAfter.byPlayerId { it },
)
