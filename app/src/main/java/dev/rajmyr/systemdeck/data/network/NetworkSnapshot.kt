package dev.rajmyr.systemdeck.data.network

import dev.rajmyr.systemdeck.core.model.CollectorStatus

data class NetworkSnapshot(
    val status: CollectorStatus,
    val connected: Boolean,
    val transport: String,
    val interfaceName: String?,
    val validated: Boolean?,
    val metered: Boolean?,
    val ipv4: List<String>,
    val ipv6: List<String>,
    val dnsServers: List<String>,
    val estimatedDownKbps: Int?,
    val estimatedUpKbps: Int?,
    val totalRxBytes: Long?,
    val totalTxBytes: Long?,
    val rxBytesPerSecond: Long?,
    val txBytesPerSecond: Long?,
    val sampledAtMillis: Long,
    val message: String? = null,
) {
    companion object {
        val Empty = NetworkSnapshot(
            status = CollectorStatus.Unavailable,
            connected = false,
            transport = "Offline",
            interfaceName = null,
            validated = null,
            metered = null,
            ipv4 = emptyList(),
            ipv6 = emptyList(),
            dnsServers = emptyList(),
            estimatedDownKbps = null,
            estimatedUpKbps = null,
            totalRxBytes = null,
            totalTxBytes = null,
            rxBytesPerSecond = null,
            txBytesPerSecond = null,
            sampledAtMillis = 0L,
            message = "Network collector has not sampled yet.",
        )
    }
}
