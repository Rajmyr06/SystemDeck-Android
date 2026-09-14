package dev.rajmyr.systemdeck.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import dev.rajmyr.systemdeck.core.model.CollectorStatus
import java.net.Inet4Address
import java.net.Inet6Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class NetworkTelemetryCollector(
    context: Context,
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun observe(intervalMillis: Long = 1_000L): Flow<NetworkSnapshot> = flow {
        var previousRx: Long? = null
        var previousTx: Long? = null
        var previousElapsed: Long? = null

        while (currentCoroutineContext().isActive) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val totalRx = TrafficStats.getTotalRxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() }
            val totalTx = TrafficStats.getTotalTxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() }

            val deltaMillis = previousElapsed?.let { nowElapsed - it }?.takeIf { it > 0L }
            val rxRate = rate(totalRx, previousRx, deltaMillis)
            val txRate = rate(totalTx, previousTx, deltaMillis)

            emit(
                read(
                    totalRx = totalRx,
                    totalTx = totalTx,
                    rxBytesPerSecond = rxRate,
                    txBytesPerSecond = txRate,
                ),
            )

            previousRx = totalRx
            previousTx = totalTx
            previousElapsed = nowElapsed
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    private fun read(
        totalRx: Long?,
        totalTx: Long?,
        rxBytesPerSecond: Long?,
        txBytesPerSecond: Long?,
    ): NetworkSnapshot {
        val sampledAt = System.currentTimeMillis()

        val activeNetwork = runCatching { connectivityManager.activeNetwork }.getOrNull()
            ?: return NetworkSnapshot(
                status = CollectorStatus.Ok,
                connected = false,
                transport = "Offline",
                interfaceName = null,
                validated = false,
                metered = null,
                ipv4 = emptyList(),
                ipv6 = emptyList(),
                dnsServers = emptyList(),
                estimatedDownKbps = null,
                estimatedUpKbps = null,
                totalRxBytes = totalRx,
                totalTxBytes = totalTx,
                rxBytesPerSecond = rxBytesPerSecond,
                txBytesPerSecond = txBytesPerSecond,
                sampledAtMillis = sampledAt,
                message = "No active network is currently selected by Android.",
            )

        val capabilities = runCatching {
            connectivityManager.getNetworkCapabilities(activeNetwork)
        }.getOrNull()
        val linkProperties = runCatching {
            connectivityManager.getLinkProperties(activeNetwork)
        }.getOrNull()

        val addresses = linkProperties?.linkAddresses.orEmpty().mapNotNull { it.address }
        val ipv4 = addresses.filterIsInstance<Inet4Address>().mapNotNull { it.hostAddress }
        val ipv6 = addresses.filterIsInstance<Inet6Address>().mapNotNull { address ->
            address.hostAddress?.substringBefore('%')
        }

        val status = if (capabilities != null || linkProperties != null) {
            CollectorStatus.Ok
        } else {
            CollectorStatus.Degraded
        }

        return NetworkSnapshot(
            status = status,
            connected = true,
            transport = transportLabel(capabilities),
            interfaceName = linkProperties?.interfaceName,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            metered = capabilities?.let {
                !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            },
            ipv4 = ipv4,
            ipv6 = ipv6,
            dnsServers = linkProperties?.dnsServers.orEmpty().mapNotNull { it.hostAddress },
            estimatedDownKbps = capabilities?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            estimatedUpKbps = capabilities?.linkUpstreamBandwidthKbps?.takeIf { it > 0 },
            totalRxBytes = totalRx,
            totalTxBytes = totalTx,
            rxBytesPerSecond = rxBytesPerSecond,
            txBytesPerSecond = txBytesPerSecond,
            sampledAtMillis = sampledAt,
            message = when (status) {
                CollectorStatus.Ok -> null
                CollectorStatus.Degraded -> "Android reports an active network, but capability details are restricted or unavailable."
                CollectorStatus.Unavailable -> "Network telemetry is unavailable."
            },
        )
    }

    private fun rate(
        current: Long?,
        previous: Long?,
        deltaMillis: Long?,
    ): Long? {
        if (current == null || previous == null || deltaMillis == null) return null
        if (current < previous) return null

        // After a long suspend/background gap, an averaged delta is not an
        // honest "current" transfer rate. Drop that one sample and let the
        // next normal cadence establish a fresh baseline.
        if (deltaMillis !in 250L..5_000L) return null

        val deltaBytes = current - previous
        return (deltaBytes.toDouble() * 1000.0 / deltaMillis.toDouble())
            .takeIf { it.isFinite() && it >= 0.0 && it <= Long.MAX_VALUE.toDouble() }
            ?.toLong()
    }

    private fun transportLabel(capabilities: NetworkCapabilities?): String {
        capabilities ?: return "Unknown"

        val transports = buildList {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
        }
        return transports.takeIf { it.isNotEmpty() }?.joinToString(" + ") ?: "Other"
    }
}
