package dev.rajmyr.systemdeck.data.gpu

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class GpuSnapshot(
    val status: CollectorStatus,
    val deviceLabel: String,
    val glEsVersion: String?,
    val currentHz: Long?,
    val minimumHz: Long?,
    val maximumHz: Long?,
    val busyPercent: Double?,
    val busyRaw: Long?,
    val totalRaw: Long?,
    val readableSources: List<String>,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    companion object {
        val Empty = GpuSnapshot(
            status = CollectorStatus.Unavailable,
            deviceLabel = "KGSL GPU",
            glEsVersion = null,
            currentHz = null,
            minimumHz = null,
            maximumHz = null,
            busyPercent = null,
            busyRaw = null,
            totalRaw = null,
            readableSources = emptyList(),
            sampledAtMillis = 0L,
            message = "GPU capability probe has not sampled yet.",
        )
    }
}
