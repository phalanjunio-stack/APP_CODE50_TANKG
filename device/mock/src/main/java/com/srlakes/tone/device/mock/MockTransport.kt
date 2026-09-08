package com.srlakes.tone.device.mock

import com.srlakes.tone.device.api.DeviceTransport
import com.srlakes.tone.device.api.LogDirection
import com.srlakes.tone.device.api.TransportLogEntry
import com.srlakes.tone.model.ConnectionState
import com.srlakes.tone.model.DiscoveredDevice
import com.srlakes.tone.model.TransportKind
import com.srlakes.tone.protocol.MidiCodec
import com.srlakes.tone.protocol.MidiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Transporte simulado. Permite usar o aplicativo INTEIRO - performance,
 * cenas, macros, efeitos - sem nenhum aparelho por perto, e sem
 * inventar protocolo nenhum.
 *
 * Ele apenas registra o que teria sido enviado. A tela Dispositivos
 * mostra esse log, o que ja e util para conferir se as cenas estao
 * disparando o que deveriam.
 */
class MockTransport(
    private val deviceName: String,
    private val simulatedAddress: String,
    private val connectDelayMs: Long = 900L,
    private val startBattery: Int? = null
) : DeviceTransport {

    override val kind: TransportKind = TransportKind.MOCK

    private val stateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state: StateFlow<ConnectionState> = stateFlow.asStateFlow()

    private val incomingFlow = MutableSharedFlow<MidiMessage>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<MidiMessage> = incomingFlow.asSharedFlow()

    private val logFlow = MutableSharedFlow<TransportLogEntry>(replay = 120, extraBufferCapacity = 64)
    override val log: SharedFlow<TransportLogEntry> = logFlow.asSharedFlow()

    private var battery = startBattery
    private var sentCount = 0

    val sentMessages = ArrayList<MidiMessage>()

    override suspend fun scan(timeoutMs: Long): List<DiscoveredDevice> {
        stateFlow.value = ConnectionState.SCANNING
        emit(LogDirection.INFO, "Procurando dispositivos (simulado)...")
        delay(minOf(timeoutMs, 1200L))
        stateFlow.value = ConnectionState.DISCONNECTED
        emit(LogDirection.INFO, "Encontrado: " + deviceName)
        return listOf(
            DiscoveredDevice(
                name = deviceName,
                address = simulatedAddress,
                rssi = -40 - Random.nextInt(30),
                transport = TransportKind.MOCK
            )
        )
    }

    override suspend fun connect(address: String?): Result<Unit> {
        stateFlow.value = ConnectionState.CONNECTING
        emit(LogDirection.INFO, "Conectando (simulado)...")
        delay(connectDelayMs)
        stateFlow.value = ConnectionState.CONNECTED
        emit(LogDirection.INFO, "Conectado a " + deviceName + " (simulado)")
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        stateFlow.value = ConnectionState.DISCONNECTED
        emit(LogDirection.INFO, "Desconectado")
    }

    override suspend fun send(message: MidiMessage): Result<Unit> {
        if (stateFlow.value != ConnectionState.CONNECTED) {
            return Result.failure(IllegalStateException(deviceName + " nao esta conectado."))
        }
        sentMessages += message
        sentCount++
        val b = battery
        if (b != null && sentCount % 120 == 0) {
            battery = (b - 1).coerceAtLeast(0)
        }
        emit(LogDirection.OUT, MidiCodec.describe(message))
        return Result.success(Unit)
    }

    override suspend fun readBatteryPercent(): Int? = battery

    /** Injeta uma mensagem como se tivesse vindo do aparelho (testa MIDI Learn). */
    suspend fun simulateIncoming(message: MidiMessage) {
        emit(LogDirection.IN, MidiCodec.describe(message))
        incomingFlow.emit(message)
    }

    private suspend fun emit(direction: LogDirection, text: String) {
        logFlow.emit(TransportLogEntry(System.currentTimeMillis(), direction, text))
    }
}
