package dev.rajmyr.systemdeck.data.device

import android.os.Build

class DeviceInfoProvider {
    fun read(): DeviceSnapshot {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { "unavailable" }
        } else {
            "unavailable"
        }

        return DeviceSnapshot(
            manufacturer = Build.MANUFACTURER.ifBlank { "unknown" },
            model = Build.MODEL.ifBlank { "unknown" },
            device = Build.DEVICE.ifBlank { "unknown" },
            soc = soc,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            sdk = Build.VERSION.SDK_INT,
            kernel = System.getProperty("os.version") ?: "unknown",
            abi = Build.SUPPORTED_ABIS.joinToString().ifBlank { "unknown" },
            cpuCores = Runtime.getRuntime().availableProcessors(),
        )
    }
}
