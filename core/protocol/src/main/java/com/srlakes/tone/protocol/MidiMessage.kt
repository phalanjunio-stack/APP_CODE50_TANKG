package com.srlakes.tone.protocol

/**
 * Representacao interna de MIDI. O app fala MIDI internamente e o
 * TRANSPORTE e um detalhe: BLE MIDI, USB MIDI, RFCOMM ou simulado.
 *
 * Nenhum UUID nem comando proprietario e inventado aqui. O que existe
 * neste arquivo e MIDI padrao, publico e documentado.
 */
sealed interface MidiMessage {
    val channel: Int

    data class ProgramChange(override val channel: Int, val program: Int) : MidiMessage
    data class ControlChange(override val channel: Int, val controller: Int, val value: Int) : MidiMessage
    data class NoteOn(override val channel: Int, val note: Int, val velocity: Int) : MidiMessage
    data class NoteOff(override val channel: Int, val note: Int, val velocity: Int) : MidiMessage

    /**
     * SysEx cru. Aqui e onde os protocolos proprietarios de TANK-G e
     * CODE50 vao morar quando forem mapeados. Ate la, ninguem emite isso.
     */
    class SysEx(val data: ByteArray) : MidiMessage {
        override val channel: Int get() = 0
        override fun toString(): String = "SysEx(" + data.size + " bytes)"
    }
}

object MidiCodec {

    private const val STATUS_NOTE_OFF = 0x80
    private const val STATUS_NOTE_ON = 0x90
    private const val STATUS_CONTROL_CHANGE = 0xB0
    private const val STATUS_PROGRAM_CHANGE = 0xC0
    private const val SYSEX_START = 0xF0
    private const val SYSEX_END = 0xF7

    fun encode(message: MidiMessage): ByteArray = when (message) {
        is MidiMessage.ProgramChange -> byteArrayOf(
            (STATUS_PROGRAM_CHANGE or (message.channel and 0x0F)).toByte(),
            (message.program and 0x7F).toByte()
        )
        is MidiMessage.ControlChange -> byteArrayOf(
            (STATUS_CONTROL_CHANGE or (message.channel and 0x0F)).toByte(),
            (message.controller and 0x7F).toByte(),
            (message.value and 0x7F).toByte()
        )
        is MidiMessage.NoteOn -> byteArrayOf(
            (STATUS_NOTE_ON or (message.channel and 0x0F)).toByte(),
            (message.note and 0x7F).toByte(),
            (message.velocity and 0x7F).toByte()
        )
        is MidiMessage.NoteOff -> byteArrayOf(
            (STATUS_NOTE_OFF or (message.channel and 0x0F)).toByte(),
            (message.note and 0x7F).toByte(),
            (message.velocity and 0x7F).toByte()
        )
        is MidiMessage.SysEx -> {
            val body = message.data
            val needsStart = body.isEmpty() || (body[0].toInt() and 0xFF) != SYSEX_START
            val needsEnd = body.isEmpty() || (body[body.size - 1].toInt() and 0xFF) != SYSEX_END
            val out = ByteArray(body.size + (if (needsStart) 1 else 0) + (if (needsEnd) 1 else 0))
            var i = 0
            if (needsStart) { out[i++] = SYSEX_START.toByte() }
            System.arraycopy(body, 0, out, i, body.size)
            i += body.size
            if (needsEnd) { out[i] = SYSEX_END.toByte() }
            out
        }
    }

    /**
     * Decodifica um pacote de bytes MIDI. Ignora running status
     * (nem BLE MIDI nem USB MIDI dependem dele na pratica).
     */
    fun decode(bytes: ByteArray, length: Int = bytes.size): List<MidiMessage> {
        val out = ArrayList<MidiMessage>(4)
        var i = 0
        while (i < length) {
            val status = bytes[i].toInt() and 0xFF
            if (status < 0x80) { i++; continue }
            val type = status and 0xF0
            val channel = status and 0x0F
            when {
                type == STATUS_PROGRAM_CHANGE && i + 1 < length -> {
                    out += MidiMessage.ProgramChange(channel, bytes[i + 1].toInt() and 0x7F)
                    i += 2
                }
                type == STATUS_CONTROL_CHANGE && i + 2 < length -> {
                    out += MidiMessage.ControlChange(
                        channel,
                        bytes[i + 1].toInt() and 0x7F,
                        bytes[i + 2].toInt() and 0x7F
                    )
                    i += 3
                }
                type == STATUS_NOTE_ON && i + 2 < length -> {
                    out += MidiMessage.NoteOn(channel, bytes[i + 1].toInt() and 0x7F, bytes[i + 2].toInt() and 0x7F)
                    i += 3
                }
                type == STATUS_NOTE_OFF && i + 2 < length -> {
                    out += MidiMessage.NoteOff(channel, bytes[i + 1].toInt() and 0x7F, bytes[i + 2].toInt() and 0x7F)
                    i += 3
                }
                status == SYSEX_START -> {
                    var end = i + 1
                    while (end < length && (bytes[end].toInt() and 0xFF) != SYSEX_END) end++
                    out += MidiMessage.SysEx(bytes.copyOfRange(i, minOf(end + 1, length)))
                    i = end + 1
                }
                else -> i++
            }
        }
        return out
    }

    fun describe(message: MidiMessage): String = when (message) {
        is MidiMessage.ProgramChange -> "PC ch" + (message.channel + 1) + " prog " + message.program
        is MidiMessage.ControlChange -> "CC ch" + (message.channel + 1) + " #" + message.controller + " = " + message.value
        is MidiMessage.NoteOn -> "NoteOn ch" + (message.channel + 1) + " " + message.note
        is MidiMessage.NoteOff -> "NoteOff ch" + (message.channel + 1) + " " + message.note
        is MidiMessage.SysEx -> "SysEx " + message.data.size + " bytes"
    }
}
