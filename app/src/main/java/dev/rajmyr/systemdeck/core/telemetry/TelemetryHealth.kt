package dev.rajmyr.systemdeck.core.telemetry

import dev.rajmyr.systemdeck.core.model.CollectorStatus

enum class SampleFreshness {
    Waiting,
    Fresh,
    Stale,
}

data class CollectorHealth(
    val id: String,
    val label: String,
    val status: CollectorStatus,
    val freshness: SampleFreshness,
    val ageMillis: Long?,
    val expectedIntervalMillis: Long,
    val message: String? = null,
) {
    val isHealthy: Boolean
        get() = status == CollectorStatus.Ok && freshness == SampleFreshness.Fresh

    val isOperational: Boolean
        get() = status != CollectorStatus.Unavailable && freshness == SampleFreshness.Fresh
}

fun collectorHealth(
    id: String,
    label: String,
    status: CollectorStatus,
    sampledAtMillis: Long,
    expectedIntervalMillis: Long,
    nowMillis: Long,
    message: String? = null,
): CollectorHealth {
    val age = sampledAtMillis
        .takeIf { it > 0L }
        ?.let { (nowMillis - it).coerceAtLeast(0L) }

    // Three missed intervals is enough to call a stream stale while still
    // allowing scheduler jitter and one slow sample without false alarms.
    val staleAfterMillis = expectedIntervalMillis * 3L
    val freshness = when {
        sampledAtMillis <= 0L -> SampleFreshness.Waiting
        age != null && age > staleAfterMillis -> SampleFreshness.Stale
        else -> SampleFreshness.Fresh
    }

    return CollectorHealth(
        id = id,
        label = label,
        status = status,
        freshness = freshness,
        ageMillis = age,
        expectedIntervalMillis = expectedIntervalMillis,
        message = message,
    )
}
