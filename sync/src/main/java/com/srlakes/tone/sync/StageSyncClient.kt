package com.srlakes.tone.sync

import com.srlakes.tone.model.StageState
import com.srlakes.tone.model.SyncState
import com.srlakes.tone.model.SyncStatus
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI

/**
 * Cliente do SR Lakes Studio.
 *
 * O Studio (apps/desktop/server.js) mantém um servidor Socket.IO na porta
 * 7575 e repassa o estado do transmissor para todos os receptores. Este
 * cliente entra como RECEPTOR: identifica-se com mode "rx" e **nunca**
 * emite o evento "state". Ele só escuta.
 *
 * Isso é deliberado. O transmissor é o notebook do líder; um segundo
 * aparelho publicando estado brigaria com ele no meio do show.
 */
class StageSyncClient {

    private val statusState = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = statusState.asStateFlow()

    private val stageState = MutableStateFlow<StageState?>(null)
    val stage: StateFlow<StageState?> = stageState.asStateFlow()

    private var socket: Socket? = null
    private var deviceName: String = "SR Lakes Tone"

    fun connect(url: String, deviceName: String) {
        disconnect()
        this.deviceName = deviceName
        statusState.value = SyncStatus(state = SyncState.CONNECTING, serverUrl = url)

        val created = runCatching {
            val options = IO.Options.builder()
                .setReconnection(true)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(5000)
                .setTimeout(8000)
                .build()
            IO.socket(URI.create(url), options)
        }.getOrElse { error ->
            statusState.value = SyncStatus(
                state = SyncState.ERROR,
                serverUrl = url,
                lastError = "Endereço inválido: " + (error.message ?: url)
            )
            return
        }

        socket = created

        created.on(Socket.EVENT_CONNECT) {
            statusState.value = statusState.value.copy(
                state = SyncState.CONNECTED,
                serverUrl = url,
                lastError = null
            )
            // Aparece na lista de aparelhos do Studio como receptor.
            created.emit("identify", JSONObject().put("name", this.deviceName).put("mode", "rx"))
        }

        created.on(Socket.EVENT_DISCONNECT) {
            statusState.value = statusState.value.copy(state = SyncState.CONNECTING)
        }

        created.on(Socket.EVENT_CONNECT_ERROR) { args ->
            statusState.value = statusState.value.copy(
                state = SyncState.ERROR,
                lastError = describeError(args)
            )
        }

        // Quem entra recebe primeiro um snapshot com o estado atual.
        created.on("snapshot") { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@on
            val state = payload.optJSONObject("state") ?: return@on
            publish(state)
        }

        created.on("state") { args ->
            val payload = args.firstOrNull() as? JSONObject ?: return@on
            publish(payload)
        }

        created.connect()
    }

    fun disconnect() {
        socket?.let { existing ->
            existing.off()
            existing.disconnect()
            existing.close()
        }
        socket = null
        stageState.value = null
        statusState.value = SyncStatus()
    }

    private fun publish(payload: JSONObject) {
        val songId = payload.optInt("songId", -1)
        if (songId < 0) return
        val state = StageState(
            studioSongId = songId,
            partIndex = payload.optInt("partIdx", 0),
            playing = payload.optBoolean("playing", false),
            bpm = payload.optInt("bpm", 0),
            bar = payload.optInt("bar", 1),
            beat = payload.optInt("beat", 1),
            activeChord = payload.optString("activeChord", ""),
            activeLyric = payload.optString("activeLyric", ""),
            timelinePos = payload.optDouble("timelinePos", 0.0).toFloat()
        )
        stageState.value = state
        statusState.value = statusState.value.copy(
            state = SyncState.CONNECTED,
            lastStateAtMs = state.receivedAtMs
        )
    }

    private fun describeError(args: Array<out Any?>): String {
        val raw = args.firstOrNull()
        val message = (raw as? Exception)?.message ?: raw?.toString()
        return "Não foi possível conectar ao Studio" + (if (message.isNullOrBlank()) "." else ": " + message)
    }
}
