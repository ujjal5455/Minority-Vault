package com.example.minorityvault.hotseat

import com.example.minorityvault.engine.ChooseTwist
import com.example.minorityvault.engine.GameEngine
import com.example.minorityvault.engine.Item
import com.example.minorityvault.engine.Phase
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.QuestionId
import com.example.minorityvault.engine.ReduceResult
import com.example.minorityvault.engine.RuleError
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.feature.round.RoundAction
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class HotSeatTest {
    private val questions = (1..30).map { QuestionId("q$it") }
    private val names = listOf("Asha", "Ben", "Chandrika", "Dev")

    private fun newSession(seed: Long = 1) = HotSeatSession.start(names, seed, questions)

    private fun HotSeatModel.send(vararg events: HotSeatEvent) =
        events.fold(this) { model, event -> model.on(event, nextSeed = { 42L }, questions = questions) }

    @Test
    fun `setup needs a distinct name for every seat, 4 to 8 players`() {
        val model = HotSeatModel()

        model.canStart shouldBe false
        model.send(HotSeatEvent.RemovePlayer(0)).names.size shouldBe 4

        val named = names.foldIndexed(model) { i, m, name -> m.send(HotSeatEvent.SetName(i, name)) }
        named.canStart shouldBe true
        named.send(HotSeatEvent.SetName(1, " asha ")).canStart shouldBe false

        val full = (1..10).fold(named) { m, _ -> m.send(HotSeatEvent.AddPlayer) }
        full.names.size shouldBe 8
    }

    @Test
    fun `every question screen announces the twist the round will use`() {
        var session = newSession(seed = 3)
        repeat(3) {
            val round = checkNotNull(session.game.round)
            round.twist shouldBe round.twistOptions.first()
            val ui = HotSeatModel(names, session).toUiState()
            ui.shouldBeInstanceOf<HotSeatUiState.Question>()
            ui.state.header.twist?.id shouldBe round.twistOptions.first().id

            session = session.continuePublic()
            checkNotNull(session.game.round).twist shouldBe round.twistOptions.first()
            session = session.startVote().let { s -> names.indices.fold(s) { acc, _ -> acc.finishTurn() } }
                .continuePublic()
            if (session.game.phase == Phase.FlipWindow) {
                session = names.indices.fold(session) { acc, _ -> acc.finishTurn() }.continuePublic()
            }
        }
    }

    @Test
    fun `negotiation passes the phone to each player, behind a cover`() {
        var session = newSession().continuePublic()

        session.step shouldBe HotSeatStep.Cover(PlayerId("p1"), Phase.Negotiation)
        session = session.showTurn()
        session.step shouldBe HotSeatStep.Turn(PlayerId("p1"), Phase.Negotiation)

        repeat(3) { session = session.finishTurn().showTurn() }
        session.step shouldBe HotSeatStep.Turn(PlayerId("p4"), Phase.Negotiation)

        session = session.finishTurn()
        session.step shouldBe HotSeatStep.NegotiationPassDone
        session.anotherNegotiationPass().step shouldBe HotSeatStep.Cover(PlayerId("p1"), Phase.Negotiation)
        session.startVote().step shouldBe HotSeatStep.Cover(PlayerId("p1"), Phase.Vote)
    }

    @Test
    fun `an armed Double Vote is applied when the player locks`() {
        val voting = newSession().continuePublic().startVote().showTurn()

        val locked = voting
            .act(RoundAction.SelectVote(Side.YES))
            .act(RoundAction.ArmDoubleVote(true))
            .act(RoundAction.Lock())

        val ballot = checkNotNull(locked.game.round).ballots.getValue(PlayerId("p1"))
        ballot.locked shouldBe true
        ballot.doubled shouldBe true
        locked.doubleArmed shouldBe false
    }

    @Test
    fun `a refused action shows an error and changes nothing`() {
        val voting = newSession().continuePublic().startVote().showTurn()

        val refused = voting.act(RoundAction.Lock())

        refused.error shouldBe RuleError.NothingSelected
        refused.game shouldBe voting.game
        refused.act(RoundAction.SelectVote(Side.NO)).error shouldBe null
    }

    @Test
    fun `after the last vote the round is revealed, then the next question comes`() {
        var session = newSession().continuePublic().startVote()
        names.indices.forEach { i ->
            session = session.showTurn()
                .act(RoundAction.SelectVote(if (i == 0) Side.YES else Side.NO))
                .act(RoundAction.Lock())
                .finishTurn()
        }

        session.step shouldBe HotSeatStep.Reveal
        checkNotNull(session.game.round?.summary).minorityPlayers shouldBe listOf(PlayerId("p1"))
        session.continuePublic().game.phase shouldBe Phase.Question
        session.continuePublic().game.round?.number shouldBe 2
    }

    @Test
    fun `cover screens show only the next player's name`() {
        val model = HotSeatModel(names, newSession().continuePublic().startVote())

        model.toUiState() shouldBe HotSeatUiState.PassPhone("Asha", Phase.Vote)
        val turn = model.copy(session = model.session?.showTurn()).toUiState()
        turn.shouldBeInstanceOf<HotSeatUiState.Vote>()
        turn.state.me shouldBe PlayerId("p1")
    }

    @Test
    fun `second chance gives every player a flip turn before the reveal`() {
        val seed = (1L..500L).first { seed ->
            checkNotNull(newSession(seed).game.round).twistOptions.any { it.id == TwistId.SECOND_CHANCE }
        }
        val start = newSession(seed)
        val chosen = (GameEngine.reduce(start.game, ChooseTwist(TwistId.SECOND_CHANCE)) as ReduceResult.Applied).state
        var session = start.copy(game = chosen).continuePublic().startVote()
        names.indices.forEach { i ->
            session = session.showTurn().act(RoundAction.SelectVote(if (i < 2) Side.YES else Side.NO))
                .act(RoundAction.Lock()).finishTurn()
        }

        session.step shouldBe HotSeatStep.Cover(PlayerId("p1"), Phase.FlipWindow)
        session = session.showTurn().act(RoundAction.Flip())
        repeat(names.size) {
            session =
                session.finishTurn().let { if (it.step is HotSeatStep.Cover) it.showTurn() else it }
        }

        session.step shouldBe HotSeatStep.Reveal
        checkNotNull(session.game.round?.summary).minorityPlayers shouldBe listOf(PlayerId("p2"))
    }

    @Test
    fun `a whole match can be played through events alone`() {
        var model = names.foldIndexed(HotSeatModel()) { i, m, name -> m.send(HotSeatEvent.SetName(i, name)) }
            .send(HotSeatEvent.Start)
        var guard = 0
        while (model.toUiState() !is HotSeatUiState.GameOver) {
            check(guard++ < MAX_EVENTS) { "match never ended" }
            model = when (val ui = model.toUiState()) {
                is HotSeatUiState.Question, is HotSeatUiState.Reveal -> model.send(HotSeatEvent.Continue)

                is HotSeatUiState.PassPhone -> model.send(HotSeatEvent.ShowTurn)

                is HotSeatUiState.NegotiationPassDone -> model.send(HotSeatEvent.StartVote)

                is HotSeatUiState.Vote -> {
                    val side = if (ui.state.me == PlayerId("p1")) Side.YES else Side.NO
                    val armed = ui.state.doubleVoteAvailable && ui.state.header.round == 1
                    model.send(
                        HotSeatEvent.Round(RoundAction.SelectVote(side)),
                        HotSeatEvent.Round(RoundAction.ArmDoubleVote(armed)),
                        HotSeatEvent.Round(RoundAction.Lock()),
                        HotSeatEvent.FinishTurn,
                    )
                }

                else -> model.send(HotSeatEvent.FinishTurn)
            }
        }
        val game = checkNotNull(model.session).game
        game.history.size shouldBe 10
        game.totalCoins shouldBe 400
        game.players.all { it.itemCount(Item.DOUBLE_VOTE) == 0 } shouldBe true
        model.send(HotSeatEvent.PlayAgain).session?.game?.phase shouldBe Phase.Question
        model.send(HotSeatEvent.NewPlayers).toUiState().shouldBeInstanceOf<HotSeatUiState.Setup>()
    }

    private companion object {
        const val MAX_EVENTS = 1000
    }
}
