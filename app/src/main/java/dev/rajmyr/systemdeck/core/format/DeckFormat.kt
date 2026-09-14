package dev.rajmyr.systemdeck.core.format

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object DeckFormat {
    fun frequency(khz: Long?): String {
        if (khz == null || khz <= 0) return "—"
        return if (khz >= 1_000_000) {
            String.format(Locale.US, "%.2f GHz", khz / 1_000_000.0)
        } else {
            "${(khz / 1_000.0).roundToInt()} MHz"
        }
    }

    fun kibToGiB(kib: Long?): String {
        if (kib == null || kib < 0) return "—"
        return String.format(Locale.US, "%.2f GiB", kib / 1024.0 / 1024.0)
    }

    fun percent(part: Long, total: Long): Int {
        if (total <= 0) return 0
        return ((part.toDouble() / total.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun voltage(milliVolts: Int?): String {
        if (milliVolts == null || milliVolts <= 0) return "—"
        return String.format(Locale.US, "%.3f V", milliVolts / 1000.0)
    }

    fun current(microAmps: Int?): String {
        if (microAmps == null) return "—"
        val milliAmps = microAmps / 1000.0
        return String.format(Locale.US, "%+.0f mA", milliAmps)
    }

    fun charge(microAmpHours: Int?): String {
        if (microAmpHours == null || microAmpHours < 0) return "—"
        return String.format(Locale.US, "%.0f mAh", microAmpHours / 1000.0)
    }

    fun power(watts: Double?): String {
        if (watts == null || watts.isNaN() || watts.isInfinite()) return "—"
        return String.format(Locale.US, "%+.2f W", watts)
    }

    fun temperature(celsius: Double?): String {
        if (celsius == null || celsius.isNaN() || celsius.isInfinite()) return "—"
        return String.format(Locale.US, "%.1f °C", celsius)
    }

    fun duration(millis: Long?): String {
        if (millis == null || millis < 0) return "—"
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun thermalHeadroom(value: Float?): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "—"
        return String.format(Locale.US, "%.2f", value)
    }
    fun bytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "—"
        val gib = bytes / 1024.0 / 1024.0 / 1024.0
        val mib = bytes / 1024.0 / 1024.0
        val kib = bytes / 1024.0
        return when {
            gib >= 1.0 -> String.format(Locale.US, "%.1f GiB", gib)
            mib >= 1.0 -> String.format(Locale.US, "%.1f MiB", mib)
            kib >= 1.0 -> String.format(Locale.US, "%.0f KiB", kib)
            else -> "$bytes B"
        }
    }

    fun rate(bytesPerSecond: Long?): String {
        if (bytesPerSecond == null || bytesPerSecond < 0L) return "—"
        val mib = bytesPerSecond / 1024.0 / 1024.0
        val kib = bytesPerSecond / 1024.0
        return when {
            mib >= 1.0 -> String.format(Locale.US, "%.2f MiB/s", mib)
            kib >= 1.0 -> String.format(Locale.US, "%.0f KiB/s", kib)
            else -> "$bytesPerSecond B/s"
        }
    }

    fun rateCompact(bytesPerSecond: Long?): String {
        if (bytesPerSecond == null || bytesPerSecond < 0L) return "—"
        val mib = bytesPerSecond / 1024.0 / 1024.0
        val kib = bytesPerSecond / 1024.0
        return when {
            mib >= 1.0 -> String.format(Locale.US, "%.1fM/s", mib)
            kib >= 1.0 -> String.format(Locale.US, "%.0fK/s", kib)
            else -> "${bytesPerSecond}B/s"
        }
    }

    fun bandwidthKbps(kbps: Int?): String {
        if (kbps == null || kbps <= 0) return "—"
        return if (kbps >= 1000) {
            String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
        } else {
            "$kbps Kbps"
        }
    }

}
