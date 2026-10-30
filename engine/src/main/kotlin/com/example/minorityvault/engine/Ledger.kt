package com.example.minorityvault.engine

/**
 * Every coin movement goes through a Ledger, so coins are never created or destroyed and no balance goes
 * below 0: each payment is capped at what the payer has left (CLAUDE.md §2 "Money invariants").
 */
internal class Ledger(players: List<Player>, pot: Int) {
    private val balances = LinkedHashMap<PlayerId, Int>().apply { players.forEach { put(it.id, it.balance) } }

    var pot: Int = pot
        private set

    fun balance(player: PlayerId): Int = balances.getValue(player)

    /** A copy in seat order. */
    fun balances(): Map<PlayerId, Int> = LinkedHashMap(balances)

    /** Moves up to [amount] from [from] into the pot. Returns what was actually paid. */
    fun payIntoPot(from: PlayerId, amount: Int): Int {
        val paid = amount.coerceIn(0, balance(from))
        balances[from] = balance(from) - paid
        pot += paid
        return paid
    }

    fun payFromPot(to: PlayerId, amount: Int) {
        check(amount in 0..pot) { "cannot pay $amount from a pot of $pot" }
        balances[to] = balance(to) + amount
        pot -= amount
    }

    /** Moves up to [amount] from [from] to [to]. Returns what was actually moved. */
    fun transfer(from: PlayerId, to: PlayerId, amount: Int): Int {
        val moved = amount.coerceIn(0, balance(from))
        balances[from] = balance(from) - moved
        balances[to] = balance(to) + moved
        return moved
    }

    fun applyTo(players: List<Player>): List<Player> = players.map { it.copy(balance = balance(it.id)) }
}
