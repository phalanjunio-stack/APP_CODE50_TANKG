package com.srlakes.tone.device.mock

import com.srlakes.tone.device.api.BaseMidiDeviceService
import com.srlakes.tone.device.api.MarshallCodeService
import com.srlakes.tone.device.api.TankGService
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.protocol.DeviceProfile
import com.srlakes.tone.protocol.ParamId
import com.srlakes.tone.protocol.ParamMapping
import kotlinx.coroutines.CoroutineScope

/**
 * Perfis SIMULADOS.
 *
 * Leia com atencao: os numeros de CC abaixo sao arbitrarios e existem
 * apenas para o simulador ter o que registrar. Eles NAO representam o
 * protocolo real de nenhum dos dois aparelhos, e por isso o campo
 * learned e false e o transporte e MOCK - nada sai pelo Bluetooth.
 *
 * Os mapeamentos de verdade vao nascer na tela Protocol Lab (MIDI Learn),
 * marcados com learned = true, e so entao passam a valer.
 */
object MockProfiles {

    private fun cc(param: ParamId, controller: Int) =
        param to ParamMapping(param = param, controller = controller, learned = false)

    val tankG = DeviceProfile(
        kind = DeviceKind.TANK_G,
        name = "M-VAVE TANK-G (simulado)",
        channel = 0,
        programCount = 36,
        mappings = mapOf(
            cc(ParamId.GAIN, 20),
            cc(ParamId.BASS, 21),
            cc(ParamId.MIDDLE, 22),
            cc(ParamId.TREBLE, 23),
            cc(ParamId.PRESENCE, 24),
            cc(ParamId.RESONANCE, 25),
            cc(ParamId.VOLUME, 7),
            cc(ParamId.IR, 26),
            cc(ParamId.DELAY, 27),
            cc(ParamId.REVERB, 28),
            cc(ParamId.GATE, 29),
            cc(ParamId.BOOST, 30),
            cc(ParamId.MODULATION, 31)
        )
    )

    val code50 = DeviceProfile(
        kind = DeviceKind.CODE50,
        name = "Marshall CODE50 (simulado)",
        channel = 0,
        programCount = 100,
        mappings = mapOf(
            cc(ParamId.PREAMP, 40),
            cc(ParamId.GAIN, 41),
            cc(ParamId.BASS, 42),
            cc(ParamId.MIDDLE, 43),
            cc(ParamId.TREBLE, 44),
            cc(ParamId.PRESENCE, 45),
            cc(ParamId.RESONANCE, 46),
            cc(ParamId.VOLUME, 7),
            cc(ParamId.POWER_AMP, 47),
            cc(ParamId.CABINET, 48),
            cc(ParamId.PRE_FX, 49),
            cc(ParamId.MODULATION, 50),
            cc(ParamId.DELAY, 51),
            cc(ParamId.REVERB, 52),
            cc(ParamId.GATE, 53),
            cc(ParamId.BOOST, 54)
        )
    )
}

class MockTankGService(
    scope: CoroutineScope,
    private val mockTransport: MockTransport = MockTransport(
        deviceName = "M-VAVE TANK-G",
        simulatedAddress = "MOCK:TANK-G",
        startBattery = 78
    )
) : BaseMidiDeviceService(
    kind = DeviceKind.TANK_G,
    transport = mockTransport,
    initialProfile = MockProfiles.tankG,
    scope = scope
), TankGService {

    /** No simulador gravar funciona, para o fluxo da UI poder ser testado. */
    override suspend fun savePreset(index: Int): Result<Unit> = Result.success(Unit)

    val transportForTests: MockTransport get() = mockTransport
}

class MockMarshallCodeService(
    scope: CoroutineScope,
    private val mockTransport: MockTransport = MockTransport(
        deviceName = "Marshall CODE50",
        simulatedAddress = "MOCK:CODE50",
        connectDelayMs = 1200L
    )
) : BaseMidiDeviceService(
    kind = DeviceKind.CODE50,
    transport = mockTransport,
    initialProfile = MockProfiles.code50,
    scope = scope
), MarshallCodeService {

    override suspend fun savePreset(index: Int): Result<Unit> = Result.success(Unit)

    val transportForTests: MockTransport get() = mockTransport
}
