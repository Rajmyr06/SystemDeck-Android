package dev.rajmyr.systemdeck.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DeckFormatTest {
    @Test
    fun frequency_formatsMHzAndGHz() {
        assertEquals("845 MHz", DeckFormat.frequency(845_000L))
        assertEquals("3.19 GHz", DeckFormat.frequency(3_187_200L))
        assertEquals("—", DeckFormat.frequency(null))
    }

    @Test
    fun bytes_and_rate_areBinaryAndStable() {
        assertEquals("1 KiB", DeckFormat.bytes(1_024L))
        assertEquals("1.0 MiB", DeckFormat.bytes(1_048_576L))
        assertEquals("1 MiB/s", normalizeRate(DeckFormat.rate(1_048_576L)))
        assertEquals("—", DeckFormat.rate(-1L))
    }

    @Test
    fun percent_isClampedAndHandlesZeroTotal() {
        assertEquals(0, DeckFormat.percent(10L, 0L))
        assertEquals(50, DeckFormat.percent(50L, 100L))
        assertEquals(100, DeckFormat.percent(150L, 100L))
    }

    private fun normalizeRate(value: String): String = value.replace("1.00", "1")
}
