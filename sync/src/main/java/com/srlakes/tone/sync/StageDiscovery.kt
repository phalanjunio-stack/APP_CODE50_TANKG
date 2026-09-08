package com.srlakes.tone.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int
) {
    val url: String get() = "http://" + host + ":" + port
}

/**
 * Encontra o notebook do Studio na rede local.
 *
 * O servidor se anuncia por mDNS como "Sr Lakes Stage Sync" no serviço
 * _http._tcp (ver apps/desktop/server.js, bloco Bonjour). Aqui a gente só
 * escuta esse anúncio e filtra pelo nome.
 *
 * mDNS falha em algumas redes de casa de show — Wi-Fi com isolamento de
 * cliente, principalmente. Por isso o IP manual existe e não é um plano B
 * envergonhado: em palco, ele costuma ser o caminho principal.
 */
class StageDiscovery(private val context: Context) {

    fun discover(): Flow<List<DiscoveredServer>> = callbackFlow {
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        if (manager == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val found = LinkedHashMap<String, DiscoveredServer>()

        val resolveQueue = ArrayDeque<NsdServiceInfo>()
        var resolving = false

        fun resolveNext() {
            if (resolving) return
            val next = resolveQueue.removeFirstOrNull() ?: return
            resolving = true
            manager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    resolving = false
                    resolveNext()
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress
                    if (host != null) {
                        val server = DiscoveredServer(
                            name = serviceInfo.serviceName ?: "SR Lakes Studio",
                            host = host,
                            port = serviceInfo.port
                        )
                        found[server.url] = server
                        trySend(found.values.toList())
                    }
                    resolving = false
                    resolveNext()
                }
            })
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!matchesStudio(serviceInfo.serviceName)) return
                resolveQueue.addLast(serviceInfo)
                resolveNext()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName ?: return
                found.entries.removeAll { it.value.name == name }
                trySend(found.values.toList())
            }
        }

        runCatching {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { close() }

        awaitClose {
            runCatching { manager.stopServiceDiscovery(listener) }
        }
    }

    private fun matchesStudio(name: String?): Boolean {
        val normalized = name?.lowercase() ?: return false
        return normalized.contains("sr lakes") || normalized.contains("stage sync")
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
    }
}
