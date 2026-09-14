package dev.rajmyr.systemdeck.data.device

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val soc: String,
    val androidVersion: String,
    val sdk: Int,
    val kernel: String,
    val abi: String,
    val cpuCores: Int,
)
