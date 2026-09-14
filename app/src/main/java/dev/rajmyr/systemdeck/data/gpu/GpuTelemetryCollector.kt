package dev.rajmyr.systemdeck.data.gpu

import android.app.ActivityManager
import android.content.Context
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class GpuTelemetryCollector(
    context: Context,
) {
    private val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
    private val kgslRoot = File("/sys/class/kgsl/kgsl-3d0")

    fun observe(intervalMillis: Long = 1_000L): Flow<GpuSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(read())
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    fun read(): GpuSnapshot {
        val glEs = runCatching { activityManager.deviceConfigurationInfo.glEsVersion }.getOrNull()
        val sources = mutableListOf<String>()

        val current = firstLong(
            "gpuclk",
            "devfreq/cur_freq",
        )?.also { sources += "current frequency" }

        val minimum = firstLong(
            "min_gpuclk",
            "devfreq/min_freq",
        )?.also { sources += "minimum frequency" }

        val maximum = firstLong(
            "max_gpuclk",
            "devfreq/max_freq",
        )?.also { sources += "maximum frequency" }

        val busyPair = readText(File(kgslRoot, "gpubusy"))
            ?.split(Regex("\\s+"))
            ?.mapNotNull { it.toLongOrNull() }
            ?.takeIf { it.size >= 2 }
            ?.let { it[0] to it[1] }
            ?.also { sources += "gpubusy" }

        val busyPercent = busyPair?.let { (busy, total) ->
            // Qualcomm KGSL exposes two raw counters through gpubusy. Treat
            // their ratio as experimental only when the pair is internally
            // sane; never clamp an impossible busy > total sample into a
            // believable percentage.
            if (total > 0L && busy in 0L..total) {
                busy.toDouble() / total.toDouble() * 100.0
            } else null
        }

        val model = listOf(
            File(kgslRoot, "gpu_model"),
            File(kgslRoot, "model"),
        ).firstNotNullOfOrNull(::readText)

        val present = kgslRoot.exists()
        val status = when {
            !present -> CollectorStatus.Unavailable
            current != null || maximum != null || busyPair != null -> CollectorStatus.Ok
            else -> CollectorStatus.Degraded
        }

        return GpuSnapshot(
            status = status,
            deviceLabel = model?.takeIf { it.length in 2..64 } ?: if (present) "Qualcomm KGSL 3D0" else "GPU unavailable",
            glEsVersion = glEs,
            currentHz = normalizeHz(current),
            minimumHz = normalizeHz(minimum),
            maximumHz = normalizeHz(maximum),
            busyPercent = busyPercent,
            busyRaw = busyPair?.first,
            totalRaw = busyPair?.second,
            readableSources = sources,
            sampledAtMillis = System.currentTimeMillis(),
            message = when (status) {
                CollectorStatus.Ok -> "KGSL exposes at least one GPU telemetry source without elevated access. Busy ratio remains experimental."
                CollectorStatus.Degraded -> "KGSL is present, but the probed frequency and busy files are not readable to this app."
                CollectorStatus.Unavailable -> "The standard Qualcomm KGSL device path is not present."
            },
        )
    }

    private fun firstLong(vararg relativePaths: String): Long? = relativePaths.firstNotNullOfOrNull { relative ->
        readText(File(kgslRoot, relative))?.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun normalizeHz(value: Long?): Long? {
        value ?: return null
        return when {
            value in 1_000L..9_999_999L -> value * 1_000L
            value >= 10_000_000L -> value
            else -> null
        }
    }

    private fun readText(file: File): String? = runCatching {
        file.readText().trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}
