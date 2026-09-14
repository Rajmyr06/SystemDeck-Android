package dev.rajmyr.systemdeck.data.cpu

import dev.rajmyr.systemdeck.core.model.CollectorStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class CpuTelemetryCollector {
    fun observe(intervalMillis: Long = 1_000L): Flow<CpuSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): CpuSnapshot {
        val cpuRoot = File("/sys/devices/system/cpu")
        val cpuDirs = cpuRoot.listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("cpu\\d+")) }
            ?.sortedBy { it.name.removePrefix("cpu").toIntOrNull() ?: Int.MAX_VALUE }
            .orEmpty()

        if (cpuDirs.isEmpty()) {
            return CpuSnapshot(
                status = CollectorStatus.Unavailable,
                cores = emptyList(),
                clusters = emptyList(),
                sampledAtMillis = System.currentTimeMillis(),
                message = "CPU sysfs topology is not readable.",
            )
        }

        data class RawCore(
            val index: Int,
            val packageId: Int?,
            val online: Boolean,
            val currentKHz: Long?,
            val minKHz: Long?,
            val maxKHz: Long?,
            val governor: String?,
        )

        val raw = cpuDirs.mapNotNull { dir ->
            val index = dir.name.removePrefix("cpu").toIntOrNull() ?: return@mapNotNull null
            val cpufreq = File(dir, "cpufreq")
            RawCore(
                index = index,
                packageId = readInt(File(dir, "topology/physical_package_id")),
                online = readOnline(File(dir, "online")),
                currentKHz = readLong(File(cpufreq, "scaling_cur_freq")),
                minKHz = readLong(File(cpufreq, "scaling_min_freq"))
                    ?: readLong(File(cpufreq, "cpuinfo_min_freq")),
                maxKHz = readLong(File(cpufreq, "scaling_max_freq"))
                    ?: readLong(File(cpufreq, "cpuinfo_max_freq")),
                governor = readText(File(cpufreq, "scaling_governor")),
            )
        }

        val packages = raw.groupBy { it.packageId }
            .entries
            .sortedBy { (_, cores) -> cores.mapNotNull { it.maxKHz }.maxOrNull() ?: Long.MAX_VALUE }

        val labelByPackage = packages.mapIndexed { position, entry ->
            val label = when {
                packages.size == 3 && position == 0 -> "EFFICIENCY"
                packages.size == 3 && position == 1 -> "PERFORMANCE"
                packages.size == 3 && position == 2 -> "PRIME"
                packages.size == 2 && position == 0 -> "EFFICIENCY"
                packages.size == 2 && position == 1 -> "PERFORMANCE"
                packages.size == 1 -> "CPU"
                else -> "CLUSTER ${entry.key ?: position}"
            }
            entry.key to label
        }.toMap()

        val cores = raw.map { core ->
            CpuCoreSnapshot(
                index = core.index,
                packageId = core.packageId,
                clusterLabel = labelByPackage[core.packageId] ?: "CPU",
                online = core.online,
                currentKHz = core.currentKHz,
                minKHz = core.minKHz,
                maxKHz = core.maxKHz,
                governor = core.governor,
            )
        }

        val clusters = cores.groupBy { it.packageId }
            .entries
            .sortedBy { (_, clusterCores) ->
                clusterCores.mapNotNull { it.maxKHz }.maxOrNull() ?: Long.MAX_VALUE
            }
            .map { (packageId, clusterCores) ->
                CpuClusterSnapshot(
                    packageId = packageId,
                    label = labelByPackage[packageId] ?: "CPU",
                    cores = clusterCores.sortedBy { it.index },
                )
            }

        val readableFrequencyCount = cores.count { it.currentKHz != null }
        val status = when {
            cores.isEmpty() -> CollectorStatus.Unavailable
            readableFrequencyCount == cores.size -> CollectorStatus.Ok
            readableFrequencyCount > 0 -> CollectorStatus.Degraded
            else -> CollectorStatus.Degraded
        }

        return CpuSnapshot(
            status = status,
            cores = cores,
            clusters = clusters,
            sampledAtMillis = System.currentTimeMillis(),
            message = when (status) {
                CollectorStatus.Ok -> null
                CollectorStatus.Degraded -> "CPU topology is available, but some frequency nodes are restricted."
                CollectorStatus.Unavailable -> "CPU telemetry is unavailable."
            },
        )
    }

    private fun readOnline(file: File): Boolean {
        if (!file.exists()) return true
        return readText(file) != "0"
    }

    private fun readInt(file: File): Int? = readText(file)?.toIntOrNull()

    private fun readLong(file: File): Long? = readText(file)?.toLongOrNull()

    private fun readText(file: File): String? = runCatching {
        file.readText().trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}
