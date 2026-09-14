package dev.rajmyr.systemdeck.data.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class StorageTelemetryCollector(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun observe(intervalMillis: Long = 30_000L): Flow<StorageSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): StorageSnapshot {
        val sampledAt = System.currentTimeMillis()
        val dataPath = appContext.filesDir.absolutePath

        val statFs = runCatching { StatFs(dataPath) }.getOrNull()
            ?: return StorageSnapshot.Empty.copy(
                sampledAtMillis = sampledAt,
                dataPath = dataPath,
                message = "StatFs could not read the internal data volume.",
            )

        val total = runCatching { statFs.totalBytes }.getOrDefault(0L).coerceAtLeast(0L)
        val available = runCatching { statFs.availableBytes }
            .getOrDefault(0L)
            .coerceIn(0L, total.coerceAtLeast(0L))
        val used = (total - available).coerceAtLeast(0L)
        val percent = if (total > 0L) {
            (used.toDouble() / total.toDouble() * 100.0).roundToInt().coerceIn(0, 100)
        } else 0

        return StorageSnapshot(
            status = if (total > 0L) CollectorStatus.Ok else CollectorStatus.Degraded,
            totalBytes = total,
            usedBytes = used,
            availableBytes = available,
            usagePercent = percent,
            dataPath = dataPath,
            sharedStorageState = Environment.getExternalStorageState(),
            sharedStorageEmulated = Environment.isExternalStorageEmulated(),
            sharedStorageRemovable = Environment.isExternalStorageRemovable(),
            sampledAtMillis = sampledAt,
            message = if (total > 0L) {
                null
            } else {
                "Internal storage totals were not available from StatFs."
            },
        )
    }
}
