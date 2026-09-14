package dev.rajmyr.systemdeck.core.telemetry

import dev.rajmyr.systemdeck.core.model.CollectorStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryHealthTest {
    @Test
    fun noSample_isWaiting() {
        val health = collectorHealth(
            id = "cpu",
            label = "CPU",
            status = CollectorStatus.Unavailable,
            sampledAtMillis = 0L,
            expectedIntervalMillis = 1_000L,
            nowMillis = 10_000L,
        )
        assertEquals(SampleFreshness.Waiting, health.freshness)
    }

    @Test
    fun threeMissedIntervals_boundaryRemainsFresh_thenBecomesStale() {
        val boundary = collectorHealth(
            id = "cpu",
            label = "CPU",
            status = CollectorStatus.Ok,
            sampledAtMillis = 7_000L,
            expectedIntervalMillis = 1_000L,
            nowMillis = 10_000L,
        )
        val stale = collectorHealth(
            id = "cpu",
            label = "CPU",
            status = CollectorStatus.Ok,
            sampledAtMillis = 6_999L,
            expectedIntervalMillis = 1_000L,
            nowMillis = 10_000L,
        )

        assertEquals(SampleFreshness.Fresh, boundary.freshness)
        assertEquals(SampleFreshness.Stale, stale.freshness)
        assertTrue(boundary.isHealthy)
    }
}
