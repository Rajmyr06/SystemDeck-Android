package dev.rajmyr.systemdeck.core.telemetry

/** Derived only from retained samples; never substitutes for missing telemetry. */
data class HistoryStatistics(
    val count: Int,
    val latest: Double,
    val minimum: Double,
    val maximum: Double,
    val average: Double,
)

fun List<HistoryPoint>.statistics(): HistoryStatistics? {
    val finite = asSequence().map { it.value }.filter { it.isFinite() }.toList()
    if (finite.isEmpty()) return null
    return HistoryStatistics(
        count = finite.size,
        latest = finite.last(),
        minimum = finite.minOrNull() ?: return null,
        maximum = finite.maxOrNull() ?: return null,
        average = finite.average(),
    )
}
