package com.srlakes.tone.protocol

import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.model.MacroSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiCodecTest {

    @Test
    fun `control change vai e volta`() {
        val message = MidiMessage.ControlChange(channel = 2, controller = 42, value = 99)
        val decoded = MidiCodec.decode(MidiCodec.encode(message))
        assertEquals(listOf(message), decoded)
    }

    @Test
    fun `program change vai e volta`() {
        val message = MidiMessage.ProgramChange(channel = 0, program = 17)
        assertEquals(listOf(message), MidiCodec.decode(MidiCodec.encode(message)))
    }

    @Test
    fun `decodifica varias mensagens no mesmo pacote`() {
        val bytes = MidiCodec.encode(MidiMessage.ControlChange(0, 7, 100)) +
            MidiCodec.encode(MidiMessage.ProgramChange(0, 3))
        val decoded = MidiCodec.decode(bytes)
        assertEquals(2, decoded.size)
        assertTrue(decoded[0] is MidiMessage.ControlChange)
        assertTrue(decoded[1] is MidiMessage.ProgramChange)
    }

    @Test
    fun `sysex ganha delimitadores`() {
        val encoded = MidiCodec.encode(MidiMessage.SysEx(byteArrayOf(0x01, 0x02)))
        assertEquals(0xF0, encoded.first().toInt() and 0xFF)
        assertEquals(0xF7, encoded.last().toInt() and 0xFF)
    }
}

class DeviceProfileTest {

    @Test
    fun `perfil sem mapeamento nao envia nada`() {
        val profile = DeviceProfile.empty(DeviceKind.TANK_G)
        assertNull(profile.messageFor(ParamId.GAIN, 0.5f))
    }

    @Test
    fun `parametro mapeado vira control change`() {
        val profile = DeviceProfile.empty(DeviceKind.CODE50)
            .withMapping(ParamMapping(ParamId.GAIN, controller = 41, learned = true))
        val message = profile.messageFor(ParamId.GAIN, 1f)
        assertEquals(41, message?.controller)
        assertEquals(127, message?.value)
    }

    @Test
    fun `valor normalizado respeita a faixa do mapeamento`() {
        val mapping = ParamMapping(ParamId.VOLUME, controller = 7, minValue = 20, maxValue = 100)
        assertEquals(20, mapping.toMidiValue(0f))
        assertEquals(100, mapping.toMidiValue(1f))
        assertEquals(60, mapping.toMidiValue(0.5f))
    }
}

class MidiLearnSessionTest {

    @Test
    fun `aprende o cc depois de mensagens suficientes`() {
        val session = MidiLearnSession(ParamId.BASS)
        repeat(MidiLearnSession.MESSAGES_TO_CONFIRM - 1) {
            assertTrue(!session.observe(MidiMessage.ControlChange(0, 21, it)))
        }
        assertTrue(session.observe(MidiMessage.ControlChange(0, 21, 64)))
        val result = session.result
        assertEquals(21, result?.controller)
        assertEquals(ParamId.BASS, result?.param)
        assertTrue(result?.learned == true)
    }

    @Test
    fun `ignora mensagens que nao sao control change`() {
        val session = MidiLearnSession(ParamId.GAIN)
        assertTrue(!session.observe(MidiMessage.ProgramChange(0, 5)))
        assertNull(session.result)
    }
}

class MacroResolverTest {

    @Test
    fun `warmth engorda os graves e recua os agudos`() {
        val neutro = MacroResolver.resolve(MacroSet.DEFAULT)
        val quente = MacroResolver.resolve(MacroSet.DEFAULT.copy(warmth = 9f))
        assertTrue("bass deveria subir", quente.bass > neutro.bass)
        assertTrue("treble deveria descer", quente.treble < neutro.treble)
    }

    @Test
    fun `presence abre os agudos`() {
        val neutro = MacroResolver.resolve(MacroSet.DEFAULT)
        val brilhante = MacroResolver.resolve(MacroSet.DEFAULT.copy(presence = 9f))
        assertTrue(brilhante.presence > neutro.presence)
        assertTrue(brilhante.treble > neutro.treble)
    }

    @Test
    fun `drive e volume passam direto`() {
        val amp = MacroResolver.resolve(MacroSet(drive = 7.5f, volume = 8.5f))
        assertEquals(7.5f, amp.gain, 0.001f)
        assertEquals(8.5f, amp.volume, 0.001f)
    }

    @Test
    fun `nenhum parametro sai da faixa 0 a 10`() {
        for (w in 0..10) {
            for (b in 0..10) {
                val amp = MacroResolver.resolve(
                    MacroSet(drive = 10f, warmth = w.toFloat(), body = b.toFloat(), presence = 10f, volume = 10f)
                )
                for (value in listOf(amp.gain, amp.bass, amp.middle, amp.treble, amp.presence, amp.resonance, amp.volume)) {
                    assertTrue("valor fora da faixa: " + value, value in 0f..10f)
                }
            }
        }
    }

    @Test
    fun `aproximacao inversa mantem drive e volume`() {
        val amp = AmpParams(gain = 6f, volume = 4f)
        val macros = MacroResolver.approximate(amp)
        assertEquals(6f, macros.drive, 0.001f)
        assertEquals(4f, macros.volume, 0.001f)
    }

    @Test
    fun `normalizacao converte a faixa do knob para midi`() {
        assertEquals(0f, MacroResolver.normalize(0f), 0.001f)
        assertEquals(1f, MacroResolver.normalize(10f), 0.001f)
        assertEquals(0.5f, MacroResolver.normalize(5f), 0.001f)
    }
}
