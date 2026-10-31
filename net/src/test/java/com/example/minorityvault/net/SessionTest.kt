package com.example.minorityvault.net

import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.protocol.ClientIntent
import com.example.minorityvault.protocol.Decoded
import com.example.minorityvault.protocol.ErrorCode
import com.example.minorityvault.protocol.HostEvent
import com.example.minorityvault.protocol.ProtocolCodec
import com.example.minorityvault.protocol.ProtocolVersion
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SessionTest {
    private class Phone(val transport: FakeTransport, val session: ClientSession)

    /** A room on a fake network, with simulated time. Sessions run in the test's background scope. */
    private class Room(val test: TestScope) {
        val network = FakeNetwork()
        val now = { test.testScheduler.currentTime }
        val host = HostSession(network.transport(), test.backgroundScope, "Asha", now, Random(SEED)).also {
            it.start()
            test.runCurrent()
        }

        fun phone(codec: ProtocolCodec = ProtocolCodec(), clock: () -> Long = now): Phone {
            val transport = network.transport()
            return Phone(transport, ClientSession(transport, test.backgroundScope, clock, codec = codec))
        }

        /** Browses, picks the only room, and tries to join it as [name]. */
        fun join(phone: Phone, name: String): ClientState {
            phone.session.browse()
            test.runCurrent()
            val room = phone.session.state.value.shouldBeInstanceOf<ClientState.Browsing>().hosts.single()
            phone.session.join(room, name)
            test.runCurrent()
            return phone.session.state.value
        }

        fun seated(name: String): Phone = phone().also { join(it, name).shouldBeInstanceOf<ClientState.InLobby>() }

        fun pass(millis: Long) {
            test.advanceTimeBy(millis)
            test.runCurrent()
        }
    }

    private val Phone.lobby get() = session.state.value.shouldBeInstanceOf<ClientState.InLobby>()

    private fun Room.hostSees(name: String) = host.state.value.players.singleOrNull { it.name == name }

    @Test
    fun `three phones join one lobby and everyone sees everyone`() = runTest {
        val room = Room(this)
        val phones = listOf("Ben", "Chandrika", "Dev").map(room::seated)

        val everyone = listOf("Asha", "Ben", "Chandrika", "Dev")
        room.host.state.value.players.map { it.name } shouldContainExactly everyone
        room.host.state.value.canStart shouldBe true
        phones.forEach { phone ->
            phone.lobby.lobby.players.map { it.name } shouldContainExactly everyone
            phone.lobby.host.roomCode shouldBe room.host.state.value.roomCode
            phone.lobby.host.hostName shouldBe "Asha"
        }
        phones.map { it.lobby.me }.distinct() shouldHaveSize 3
    }

    @Test
    fun `a taken name is refused and the player can try another`() = runTest {
        val room = Room(this)
        val ben = room.phone()

        val refused = room.join(ben, "  asha ").shouldBeInstanceOf<ClientState.NameRefused>()
        refused.reason shouldBe ErrorCode.NAME_TAKEN
        ben.session.join(refused.host, "Ben")
        runCurrent()

        ben.lobby.lobby.players.map { it.name } shouldContainExactly listOf("Asha", "Ben")
    }

    @Test
    fun `blank and overlong names are refused`() = runTest {
        val room = Room(this)

        room.join(room.phone(), "   ").shouldBeInstanceOf<ClientState.NameRefused>().reason shouldBe
            ErrorCode.NAME_INVALID
        room.join(room.phone(), "x".repeat(NetConfig().maxNameLength + 1))
            .shouldBeInstanceOf<ClientState.NameRefused>().reason shouldBe ErrorCode.NAME_INVALID
    }

    @Test
    fun `a full room turns newcomers away`() = runTest {
        val room = Room(this)
        repeat(NetConfig().maxPlayers - 1) { room.host.addBot() }
        runCurrent()

        room.host.state.value.isFull shouldBe true
        room.join(room.phone(), "Ben") shouldBe ClientState.Ended(EndReason.LobbyFull)
    }

    @Test
    fun `a phone on another major version gets a clear refusal`() = runTest {
        val room = Room(this)
        val newer = "${ProtocolVersion.MAJOR + 1}.0"

        room.join(room.phone(codec = ProtocolCodec(newer)), "Ben") shouldBe
            ClientState.Ended(EndReason.VersionMismatch(ours = newer, theirs = ProtocolVersion.CURRENT))
        room.hostSees("Ben") shouldBe null
    }

    @Test
    fun `a dropped phone gets its own seat back within the minute`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")
        val seat = ben.lobby.me

        room.network.goOffline(ben.transport)
        runCurrent()
        room.hostSees("Ben")?.connected shouldBe false
        ben.lobby.reconnecting shouldBe true

        room.pass(20_000)
        room.network.goOnline(ben.transport)
        room.pass(15_000)

        ben.lobby.reconnecting shouldBe false
        ben.lobby.me shouldBe seat
        room.hostSees("Ben")?.connected shouldBe true
        room.host.state.value.players shouldHaveSize 2
    }

    @Test
    fun `a seat is given up after a minute away`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")

        room.network.goOffline(ben.transport)
        room.pass(61_000)

        room.hostSees("Ben") shouldBe null
        ben.session.state.value shouldBe ClientState.Ended(EndReason.HostLost)
    }

    @Test
    fun `a phone that goes silent counts as dropped, then finds its way back`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")

        room.network.mute(ben.transport)
        room.pass(NetConfig().silenceTimeoutMillis + 2_000)
        room.hostSees("Ben")?.connected shouldBe false
        ben.lobby.reconnecting shouldBe true

        room.network.mute(ben.transport, muted = false)
        room.pass(30_000)
        room.hostSees("Ben")?.connected shouldBe true
        ben.lobby.reconnecting shouldBe false
    }

    @Test
    fun `once the game starts, only players who already have a seat get in`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")
        repeat(2) { room.host.addBot() }
        room.host.startGame()
        runCurrent()
        room.host.state.value.started shouldBe true
        ben.lobby.lobby.started shouldBe true

        room.join(room.phone(), "Eve") shouldBe ClientState.Ended(EndReason.GameInProgress)

        room.network.goOffline(ben.transport)
        room.pass(5_000)
        room.network.goOnline(ben.transport)
        room.pass(15_000)
        ben.lobby.reconnecting shouldBe false
    }

    @Test
    fun `the start needs four players, all connected`() = runTest {
        val room = Room(this)
        room.host.addBot()
        val ben = room.seated("Ben")
        room.host.state.value.canStart shouldBe false

        room.host.addBot()
        runCurrent()
        room.host.state.value.canStart shouldBe true

        room.network.goOffline(ben.transport)
        runCurrent()
        room.host.state.value.canStart shouldBe false
    }

    @Test
    fun `heartbeats measure how far ahead the host's clock is`() = runTest {
        val room = Room(this)
        val ben = room.phone(clock = { room.now() - 7_000 })
        room.join(ben, "Ben")

        ben.session.clockOffsetMillis shouldBe 7_000
    }

    @Test
    fun `the host can show a player out`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")

        room.host.removePlayer(ben.lobby.me)
        room.pass(1_000)

        ben.session.state.value shouldBe ClientState.Ended(EndReason.RemovedByHost)
        room.hostSees("Ben") shouldBe null
    }

    @Test
    fun `closing the room tells everyone`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")

        launch { room.host.close() }
        runCurrent()

        ben.session.state.value shouldBe ClientState.Ended(EndReason.LobbyClosed)
    }

    @Test
    fun `game messages are tagged with the seat they came from`() = runTest {
        val room = Room(this)
        val ben = room.seated("Ben")
        val received = mutableListOf<Pair<PlayerId, ClientIntent>>()
        backgroundScope.launch { room.host.gameMessages.collect { received += it } }
        runCurrent()

        ben.session.send(ClientIntent.VoteLock())
        runCurrent()

        received shouldContainExactly listOf(ben.lobby.me to ClientIntent.VoteLock())
    }

    @Test
    fun `a phone must join before it plays, and is let go if it never does`() = runTest {
        val room = Room(this)
        val raw = room.network.transport()
        val events = mutableListOf<TransportEvent>()
        backgroundScope.launch { raw.events.collect { events += it } }
        val codec = ProtocolCodec()

        raw.startDiscovery()
        runCurrent()
        val hostId = events.filterIsInstance<TransportEvent.EndpointFound>().single().endpointId
        raw.requestConnection(hostId, "Mallory")
        runCurrent()
        raw.acceptConnection(hostId)
        runCurrent()
        raw.send(hostId, codec.encode(0, ClientIntent.VoteLock()))
        runCurrent()

        val replies = events.filterIsInstance<TransportEvent.Received>().map { codec.decodeHostEvent(it.bytes) }
        replies shouldContain Decoded.Message(0, HostEvent.Error(ErrorCode.NOT_JOINED))

        room.pass(NetConfig().joinTimeoutMillis + 2_000)
        events shouldContain TransportEvent.Disconnected(hostId)
    }

    private companion object {
        const val SEED = 7
    }
}
