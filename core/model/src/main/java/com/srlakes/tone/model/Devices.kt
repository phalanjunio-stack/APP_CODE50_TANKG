package com.srlakes.tone.model

data class DeviceInfo(
    val kind: DeviceKind,
    val displayName: String = kind.displayName,
    val address: String? = null,
    val transport: TransportKind = TransportKind.MOCK,
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val batteryPercent: Int? = null,
    val firmware: String? = null,
    val lastError: String? = null
) {
    val connected: Boolean get() = state == ConnectionState.CONNECTED
}

/** Um dispositivo encontrado durante o scan. */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int?,
    val transport: TransportKind
)
