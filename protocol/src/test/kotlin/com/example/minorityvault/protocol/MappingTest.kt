package com.example.minorityvault.protocol

import com.example.minorityvault.engine.GameConfig
import com.example.minorityvault.engine.MakePromise
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.PlayerSeat
import com.example.minorityvault.engine.PromiseTarget
import com.example.minorityvault.engine.Side
import com.example.minorityvault.engine.sim.RandomPolicy
import com.example.minorityvault.engine.sim.simulateGame
import com.example.minorityvault.engine.toPrivateView
import com.example.minorityvault.engine.toPublicView
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MappingTest {
    private val codec = ProtocolCodec()

    private fun HostEvent.overTheAir(): HostEvent =
        (codec.decodeHostEvent(codec.encode(0, this)) as Decoded.Message).message

    @Test
    fun `every view of simulated games arrives exactly as the engine made it`() {
        val phasesSeen = mutableSetOf<WirePhase>()
        for (game in 0 until GAMES) {
            val seats = (1..4 + game % 5).map { PlayerSeat(PlayerId("p$it"), "Player $it") }
            simulateGame(
                config = GameConfig(),
                seats = seats,
                policies = seats.associate { it.id to RandomPolicy() },
                seed = game.toLong(),
                observe = { state ->
                    val public = state.toPublicView()
                    phasesSeen += public.phase.toWire()
                    val sent = HostEvent.PublicView(public.toDto()).overTheAir()
                    sent.shouldBeInstanceOf<HostEvent.PublicView>().view.toEngine() shouldBe public
                    seats.forEach { seat ->
                        val private = checkNotNull(state.toPrivateView(seat.id))
                        val sentPrivate = HostEvent.PrivateView(private.toDto()).overTheAir()
                        sentPrivate.shouldBeInstanceOf<HostEvent.PrivateView>().view.toEngine() shouldBe private
                    }
                },
            )
        }
        phasesSeen shouldContainAll WirePhase.entries - WirePhase.LOBBY
    }

    @Test
    fun `every rule error survives the trip`() {
        ruleErrorSamples.forEach { error ->
            val sent = HostEvent.Error(ErrorCode.RULE, error.toDto()).overTheAir()
            sent.shouldBeInstanceOf<HostEvent.Error>().ruleError?.toEngine() shouldBe error
        }
    }

    @Test
    fun `the sender of an intent is whoever the connection belongs to`() {
        val sender = PlayerId("p7")
        val intents = clientSamples.mapNotNull { it.toEngineIntent(sender) }

        intents.size shouldBe clientSamples.size - NON_ENGINE_MESSAGES
        intents.forEach { it.player shouldBe sender }
    }

    @Test
    fun `a promise without a recipient is a promise to everyone`() {
        val intent = ClientIntent.MakePromise(to = null, side = Side.NO)

        val made = intent.toEngineIntent(PlayerId("p1"))
        made.shouldBeInstanceOf<MakePromise>().to shouldBe PromiseTarget.Everyone
    }

    private companion object {
        const val GAMES = 25

        /** Join, Ready, ChatPublic, ChatDm and Heartbeat are handled by the session, not the engine. */
        const val NON_ENGINE_MESSAGES = 5
    }
}
