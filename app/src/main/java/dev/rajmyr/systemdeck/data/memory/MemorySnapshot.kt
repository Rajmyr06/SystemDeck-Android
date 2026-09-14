package dev.rajmyr.systemdeck.data.memory

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class MemorySnapshot(
    val status: CollectorStatus,
    val totalKiB: Long,
    val availableKiB: Long,
    val freeKiB: Long,
    val cachedKiB: Long,
    val buffersKiB: Long,
    val slabKiB: Long,
    val swapTotalKiB: Long,
    val swapFreeKiB: Long,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    val usedKiB: Long
        get() = (totalKiB - availableKiB).coerceAtLeast(0)

    val swapUsedKiB: Long
        get() = (swapTotalKiB - swapFreeKiB).coerceAtLeast(0)

    companion object {
        val Empty = MemorySnapshot(
            status = CollectorStatus.Unavailable,
            totalKiB = 0,
            availableKiB = 0,
            freeKiB = 0,
            cachedKiB = 0,
            buffersKiB = 0,
            slabKiB = 0,
            swapTotalKiB = 0,
            swapFreeKiB = 0,
            sampledAtMillis = 0L,
            message = "Waiting for first memory sample.",
        )
    }
}
