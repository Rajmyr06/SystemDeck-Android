package dev.rajmyr.systemdeck.data.memory

import dev.rajmyr.systemdeck.core.model.CollectorStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class MemoryTelemetryCollector {
    fun observe(intervalMillis: Long = 1_000L): Flow<MemorySnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): MemorySnapshot {
        val values = runCatching {
            File("/proc/meminfo")
                .readLines()
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) return@mapNotNull null
                    val key = line.substring(0, separator)
                    val value = line.substring(separator + 1)
                        .trim()
                        .substringBefore(' ')
                        .toLongOrNull()
                    value?.let { key to it }
                }
                .toMap()
        }.getOrElse {
            return MemorySnapshot.Empty.copy(
                sampledAtMillis = System.currentTimeMillis(),
                message = "Android denied access to /proc/meminfo.",
            )
        }

        val total = values["MemTotal"] ?: 0L
        val available = values["MemAvailable"] ?: values["MemFree"] ?: 0L

        return MemorySnapshot(
            status = if (total > 0) CollectorStatus.Ok else CollectorStatus.Degraded,
            totalKiB = total,
            availableKiB = available,
            freeKiB = values["MemFree"] ?: 0L,
            cachedKiB = values["Cached"] ?: 0L,
            buffersKiB = values["Buffers"] ?: 0L,
            slabKiB = values["Slab"] ?: 0L,
            swapTotalKiB = values["SwapTotal"] ?: 0L,
            swapFreeKiB = values["SwapFree"] ?: 0L,
            sampledAtMillis = System.currentTimeMillis(),
            message = if (total > 0) null else "MemTotal was not available.",
        )
    }
}
