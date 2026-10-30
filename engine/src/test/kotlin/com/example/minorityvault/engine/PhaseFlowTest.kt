package com.example.minorityvault.engine

import com.example.minorityvault.engine.Side.NO
import com.example.minorityvault.engine.Side.YES
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PhaseFlowTest {
    @Test
    fun `a game needs 4 to 8 players with distinct ids and enough questions`() {
        lobby(players = 3).rejection(StartGame) shouldBe RuleError.InvalidSetup(SetupProblem.PLAYER_COUNT)
        lobby(players = 9).rejection(StartGame) shouldBe RuleError.InvalidSetup(SetupProblem.PLAYER_COUNT)
        lobby(players = 4).apply(StartGame).phase shouldBe Phase.Question
        lobby(players = 8).apply(StartGame).phase shouldBe Phase.Question

        val duplicated = GameState.newLobby(GameConfig(), seats(4) + seats(1), questions(), seed = 1)
        duplicated.rejection(StartGame) shouldBe RuleError.InvalidSetup(SetupProblem.DUPLICATE_PLAYER)

        val fewQuestions = GameState.newLobby(GameConfig(), seats(5), questions(3), seed = 1)
        fewQuestions.rejection(StartGame) shouldBe RuleError.InvalidSetup(SetupProblem.NOT_ENOUGH_QUESTIONS)
    }

    @Test
    fun `a game starts only once`() {
        lobby().apply(StartGame).rejection(StartGame) shouldBe RuleError.WrongPhase(Phase.Question)
    }

    @Test
    fun `a round runs QUESTION, NEGOTIATION, VOTE, REVEAL, SETTLEMENT, then the next QUESTION`() {
        val phases = generateSequence(startedGame()) { it.advance() }.take(6).map { it.phase }.toList()

        phases shouldBe listOf(
            Phase.Question,
            Phase.Negotiation,
            Phase.Vote,
            Phase.Reveal,
            Phase.Settlement,
            Phase.Question,
        )
    }

    @Test
    fun `a timer for a phase the game already left is rejected`() {
        val negotiating = startedGame().advance()

        negotiating.rejection(TimerExpired(Phase.Question)) shouldBe
            RuleError.StalePhase(expected = Phase.Question, actual = Phase.Negotiation)
        negotiating.apply(TimerExpired(Phase.Negotiation)).phase shouldBe Phase.Vote
    }

    @Test
    fun `questions never repeat within a match and the match ends after the last round`() {
        val events = mutableListOf<PublicEvent>()
        var state = lobby().apply(StartGame)
        while (state.phase != Phase.GameOver) {
            events += state.events(AdvancePhase(state.phase))
            state = state.advance()
        }

        state.history shouldHaveSize 10
        state.history.map { it.questionId }.toSet() shouldHaveSize 10
        state.rejection(AdvancePhase(Phase.GameOver)) shouldBe RuleError.WrongPhase(Phase.GameOver)
        events.last() shouldBe PublicEvent.GameEnded(state.winners)
    }

    @Test
    fun `the richest player wins and ties share the win`() {
        val state = startedGame(config = GameConfig(rounds = 1))
            .playVote(P1 to YES, P2 to YES, P3 to NO, P4 to NO, P5 to NO)
            .advanceTo(Phase.GameOver)

        state.winners shouldBe listOf(P1, P2)
        state.toPublicView().winners shouldBe listOf(P1, P2)
    }

    @Test
    fun `nobody wins if everyone ends as a Debtor`() {
        val state = startedGame(config = GameConfig(rounds = 1))
            .withBalances(0, 0, 0, 0, 0)
            .playVote(P1 to YES, P2 to NO)
            .advanceTo(Phase.GameOver)

        state.players.all { it.isDebtor } shouldBe true
        state.winners shouldBe emptyList()
    }

    @Test
    fun `the same seed and intents always produce the same game`() {
        fun play() = lobby(seed = 99).apply(StartGame).advance()
            .apply(MakePromise(P1, PromiseTarget.Everyone, YES))
            .playVote(P1 to YES, P2 to NO, P3 to NO)
            .advanceTo(Phase.Question)

        play() shouldBe play()
    }
}

class VoteMechanicsTest {
    private val voting = startedGame().advanceTo(Phase.Vote)

    @Test
    fun `a vote must be selected before locking, and unlocked before changing`() {
        voting.rejection(VoteLock(P1)) shouldBe RuleError.NothingSelected
        voting.rejection(VoteUnlock(P1)) shouldBe RuleError.NotLocked
        voting.vote(P1, YES).rejection(VoteSelect(P1, NO)) shouldBe RuleError.VoteIsLocked
        voting.vote(P1, YES).apply(VoteUnlock(P1)).vote(P1, NO).advance().summary.votes.getValue(P1).choice shouldBe
            VoteChoice.NO
    }

    @Test
    fun `a vote selected but never locked is an abstention`() {
        voting.apply(VoteSelect(P1, YES)).advance().summary.votes.getValue(P1) shouldBe Vote.ABSTAIN
    }

    @Test
    fun `votes can only be changed during VOTE`() {
        startedGame().rejection(VoteSelect(P1, YES)) shouldBe RuleError.WrongPhase(Phase.Question)
        startedGame().rejection(UseItem(P1, Item.DOUBLE_VOTE)) shouldBe RuleError.WrongPhase(Phase.Question)
    }

    @Test
    fun `unknown players are rejected`() {
        val ghost = PlayerId("ghost")
        voting.rejection(VoteSelect(ghost, YES)) shouldBe RuleError.UnknownPlayer(ghost)
        voting.rejection(VoteSelect(P1, YES, forPlayer = ghost)) shouldBe RuleError.UnknownPlayer(ghost)
    }

    @Test
    fun `the Double Vote is applied before locking and consumed only when a locked vote closes`() {
        val doubled = voting.apply(VoteSelect(P1, YES)).apply(UseItem(P1, Item.DOUBLE_VOTE))

        doubled.rejection(UseItem(P1, Item.DOUBLE_VOTE)) shouldBe RuleError.ItemAlreadyApplied(Item.DOUBLE_VOTE)
        doubled.apply(VoteLock(P1)).rejection(UseItem(P1, Item.DOUBLE_VOTE)) shouldBe RuleError.VoteIsLocked

        // Never locked: abstains and keeps the item.
        val unlockedAtClose = doubled.advance()
        unlockedAtClose.summary.votes.getValue(P1) shouldBe Vote.ABSTAIN
        checkNotNull(unlockedAtClose.player(P1)).itemCount(Item.DOUBLE_VOTE) shouldBe 1

        // Unlocking takes the Double Vote back off the ballot.
        val relocked = doubled.apply(VoteLock(P1)).apply(VoteUnlock(P1)).apply(VoteLock(P1)).advance()
        relocked.summary.votes.getValue(P1) shouldBe Vote(VoteChoice.YES, doubled = false)
        checkNotNull(relocked.player(P1)).itemCount(Item.DOUBLE_VOTE) shouldBe 1
    }

    @Test
    fun `the Double Vote can be used once per match`() {
        val nextRound = voting.vote(P1, YES, doubled = true).advance().advanceTo(Phase.Vote)

        nextRound.rejection(UseItem(P1, Item.DOUBLE_VOTE)) shouldBe RuleError.NoItemLeft(Item.DOUBLE_VOTE)
    }
}
