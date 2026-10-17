package de.teutonstudio.fluxbattery

import kotlin.test.*
import org.junit.jupiter.api.Test
import java.util.Random

class EnergyMathTest {
    @Test fun boundaries() {
        assertEquals(0, EnergyMath.receive(0, 0, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, EnergyMath.receive(0, Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(1, EnergyMath.receive(Long.MAX_VALUE - 1, Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(0, EnergyMath.receive(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
        assertEquals(0, EnergyMath.extract(Long.MAX_VALUE, Long.MIN_VALUE))
        assertEquals(0, EnergyMath.receive(0, Long.MAX_VALUE, -1))
        assertEquals(Int.MAX_VALUE.toLong() + 100, EnergyMath.extract(Int.MAX_VALUE.toLong() + 100, Long.MAX_VALUE))
    }
    @Test fun randomizedConservation() {
        val random = Random(75219)
        repeat(10_000) {
            val capacity = random.nextLong().ushr(1)
            val stored = minOf(capacity, random.nextLong().ushr(1))
            val request = random.nextLong()
            val accepted = EnergyMath.receive(stored, capacity, request)
            val after = stored + accepted
            assertTrue(after in stored..capacity)
            assertEquals(accepted, EnergyMath.extract(after, accepted))
            assertEquals(stored, after - EnergyMath.extract(after, accepted))
        }
    }
    @Test fun invalidStateRejected() {
        assertFailsWith<IllegalArgumentException> { EnergyMath.receive(-1, 10, 1) }
        assertFailsWith<IllegalArgumentException> { EnergyMath.receive(11, 10, 1) }
    }
}
