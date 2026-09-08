package com.srlakes.tone.device.api

import com.srlakes.tone.model.ConnectionState
import com.srlakes.tone.model.DiscoveredDevice
import com.srlakes.tone.model.TransportKind
import com.srlakes.tone.protocol.MidiMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Canal fisico ate um aparelho.
 *
 * Esta abstracao existe porque o app NAO assume BLE para todo mundo:
 *  - o M-VAVE TANK-G provavelmente fala BLE MIDI padrao;
 *  - o Marshall CODE50 faz streaming de audio Bluetooth, o que sugere
 *    Bluetooth Classic (RFCOMM), e tambem aceita USB MIDI.
 *
 * Trocar de transporte nao pode obrigar a reescrever a logica de cima.
 */
interface DeviceTransport {

    val kind: TransportKind

    val state: StateFlow<ConnectionState>

    /** Mensagens vindas do aparelho. Base do MIDI Learn. */
    val incoming: SharedFlow<MidiMessage>

    /** Tudo que entrou e saiu, para a tela de diagnostico. */
    val log: SharedFlow<TransportLogEntry>

    suspend fun scan(timeoutMs: Long = 6000L): List<DiscoveredDevice>

    suspend fun connect(address: String? = null): Result<Unit>

    suspend fun disconnect()

    suspend fun send(message: MidiMessage): Result<Unit>

    /** Informacao extra que o transporte consegue ler (bateria, firmware). */
    suspend fun readBatteryPercent(): Int? = null
}

enum class LogDirection { IN, OUT, INFO, ERROR }

data class TransportLogEntry(
    val atMs: Long,
    val direction: LogDirection,
    val text: String
)
