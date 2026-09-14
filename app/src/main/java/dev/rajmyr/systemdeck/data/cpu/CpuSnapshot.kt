package dev.rajmyr.systemdeck.data.cpu

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class CpuCoreSnapshot(
    val index: Int,
    val packageId: Int?,
    val clusterLabel: String,
    val online: Boolean,
    val currentKHz: Long?,
    val minKHz: Long?,
    val maxKHz: Long?,
    val governor: String?,
)

data class CpuClusterSnapshot(
    val packageId: Int?,
    val label: String,
    val cores: List<CpuCoreSnapshot>,
) {
    val currentMaxKHz: Long?
        get() = cores.mapNotNull { it.currentKHz }.maxOrNull()

    val maxKHz: Long?
        get() = cores.mapNotNull { it.maxKHz }.maxOrNull()
}

data class CpuSnapshot(
    val status: CollectorStatus,
    val cores: List<CpuCoreSnapshot>,
    val clusters: List<CpuClusterSnapshot>,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    companion object {
        val Empty = CpuSnapshot(
            status = CollectorStatus.Unavailable,
            cores = emptyList(),
            clusters = emptyList(),
            sampledAtMillis = 0L,
            message = "Waiting for first CPU sample.",
        )
    }
}
