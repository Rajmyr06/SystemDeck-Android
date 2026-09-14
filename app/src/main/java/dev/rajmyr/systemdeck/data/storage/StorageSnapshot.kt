package dev.rajmyr.systemdeck.data.storage

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class StorageSnapshot(
    val status: CollectorStatus,
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usagePercent: Int,
    val dataPath: String,
    val sharedStorageState: String,
    val sharedStorageEmulated: Boolean,
    val sharedStorageRemovable: Boolean,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    companion object {
        val Empty = StorageSnapshot(
            status = CollectorStatus.Unavailable,
            totalBytes = 0L,
            usedBytes = 0L,
            availableBytes = 0L,
            usagePercent = 0,
            dataPath = "—",
            sharedStorageState = "Unknown",
            sharedStorageEmulated = false,
            sharedStorageRemovable = false,
            sampledAtMillis = 0L,
            message = "Storage collector has not sampled yet.",
        )
    }
}
