package com.example.minorityvault.net

/**
 * Estimates how far the host's clock is ahead of ours from heartbeat round trips, so countdowns on every phone
 * end together. Uses the quickest recent round trip: the less time a message spent in the air, the less it can
 * have skewed the reading.
 */
class ClockSync(private val keep: Int = DEFAULT_KEEP) {
    private data class Sample(val roundTrip: Long, val offset: Long)

    private val samples = ArrayDeque<Sample>()

    /** Host time minus local time, or null before the first reply. */
    val offsetMillis: Long? get() = samples.minByOrNull { it.roundTrip }?.offset

    /** [sentAt] and [receivedAt] are on our clock; [hostTime] is when the host replied, on its clock. */
    fun add(sentAt: Long, hostTime: Long, receivedAt: Long) {
        val roundTrip = receivedAt - sentAt
        if (roundTrip < 0) return
        samples.addLast(Sample(roundTrip, hostTime - (sentAt + roundTrip / 2)))
        while (samples.size > keep) samples.removeFirst()
    }

    fun reset() = samples.clear()

    private companion object {
        const val DEFAULT_KEEP = 8
    }
}
