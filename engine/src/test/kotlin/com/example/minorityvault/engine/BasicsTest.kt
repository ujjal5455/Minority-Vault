package com.example.minorityvault.engine

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameConfigTest {
    private val config = GameConfig()

    @Test
    fun `fines follow the tiers and the final round is all-in`() {
        listOf(1, 3, 4, 6, 7, 9).map { config.fineFor(it, balance = 100) } shouldBe listOf(10, 10, 20, 20, 30, 30)
        config.fineFor(10, balance = 100) shouldBe 50
        config.fineFor(10, balance = 75) shouldBe 37
    }

    @Test
    fun `a shorter match still ends all-in`() {
        val short = GameConfig(rounds = 5)

        short.fineFor(4, balance = 100) shouldBe 20
        short.fineFor(5, balance = 100) shouldBe 50
    }

    @Test
    fun `negotiation length is limited to 60–300 seconds`() {
        assertThrows<IllegalArgumentException> { GameConfig(negotiationSeconds = 59) }
        assertThrows<IllegalArgumentException> { GameConfig(negotiationSeconds = 301) }
        GameConfig(negotiationSeconds = 60).phaseSeconds(Phase.Negotiation) shouldBe 60
    }

    @Test
    fun `only timed phases have durations`() {
        config.phaseSeconds(Phase.Vote) shouldBe 20
        config.phaseSeconds(Phase.Reveal) shouldBe null
    }
}

class SeededRandomTest {
    @Test
    fun `the same seed gives the same sequence`() {
        val a = SeededRandom(42)
        val b = SeededRandom(42)

        List(20) { a.nextLong() } shouldBe List(20) { b.nextLong() }
        SeededRandom(42).nextLong() shouldNotBe SeededRandom(43).nextLong()
    }

    @Test
    fun `the sequence is pinned so a saved seed replays the same game forever`() {
        // SplitMix64 reference values for seed 0.
        val random = SeededRandom(0)

        random.nextLong() shouldBe 0xE220A8397B1DCDAFuL.toLong()
        random.nextLong() shouldBe 0x6E789E6AA1B965F4uL.toLong()
    }

    @Test
    fun `nextInt stays in range and shuffles keep every item`() {
        val random = SeededRandom(7)

        List(1000) { random.nextInt(6) }.all { it in 0..5 } shouldBe true
        random.shuffled((1..10).toList()).sorted() shouldBe (1..10).toList()
    }
}
