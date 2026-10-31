package com.example.minorityvault.net

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.random.Random

class NetBasicsTest {
    @Test
    fun `each Android version asks for exactly what Nearby needs`() {
        val bluetooth = listOf(
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
        )
        val coarse = "android.permission.ACCESS_COARSE_LOCATION"
        val fine = "android.permission.ACCESS_FINE_LOCATION"
        val wifi = "android.permission.NEARBY_WIFI_DEVICES"
        val localNetwork = "android.permission.ACCESS_LOCAL_NETWORK"

        NearbyPermissions.required(26) shouldContainExactlyInAnyOrder listOf(coarse)
        NearbyPermissions.required(29) shouldContainExactlyInAnyOrder listOf(coarse, fine)
        NearbyPermissions.required(31) shouldContainExactlyInAnyOrder listOf(coarse, fine) + bluetooth
        NearbyPermissions.required(33) shouldContainExactlyInAnyOrder bluetooth + wifi
        NearbyPermissions.required(36) shouldContainExactlyInAnyOrder bluetooth + wifi
        NearbyPermissions.required(37) shouldContainExactlyInAnyOrder bluetooth + wifi + localNetwork
    }

    @Test
    fun `the clock offset comes from the quickest round trip`() {
        val sync = ClockSync()
        sync.offsetMillis shouldBe null

        // Slow trip: 400 ms, host stamped 5_300 → offset 5_300 - (1_000 + 200) = 4_100.
        sync.add(sentAt = 1_000, hostTime = 5_300, receivedAt = 1_400)
        // Quick trip: 20 ms, host stamped 7_010 → offset 7_010 - (3_000 + 10) = 4_000.
        sync.add(sentAt = 3_000, hostTime = 7_010, receivedAt = 3_020)

        sync.offsetMillis shouldBe 4_000
    }

    @Test
    fun `room codes avoid letters that read like numbers`() {
        val random = Random(3)
        repeat(200) {
            val code = random.roomCode()
            code.length shouldBe 4
            code.none { it in "IO01" } shouldBe true
        }
    }

    @Test
    fun `hosts advertise their room code and name together`() {
        EndpointName.parse("e1", EndpointName.of("KTRW", "Asha | the bold")) shouldBe
            NearbyHost("e1", "KTRW", "Asha | the bold")
        EndpointName.parse("e2", "no separator") shouldBe null
    }

    @Test
    fun `names are tidied before they are checked`() {
        cleanName("  Asha   Rao ", maxLength = 16) shouldBe "Asha Rao"
        cleanName("   ", maxLength = 16) shouldBe null
        cleanName("x".repeat(17), maxLength = 16) shouldBe null
    }
}
