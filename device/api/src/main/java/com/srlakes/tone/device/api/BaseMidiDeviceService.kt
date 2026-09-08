package com.srlakes.tone.device.api

import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.AmpParam
import com.srlakes.tone.model.ConnectionState
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.model.DiscoveredDevice
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.Preset
import com.srlakes.tone.protocol.DeviceProfile
import com.srlakes.tone.protocol.MacroResolver
import com.srlakes.tone.protocol.MidiMessage
import com.srlakes.tone.protocol.ParamId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Implementacao completa de ToneDeviceService em cima de um DeviceTransport.
 *
 * Toda a logica de "qual mensagem enviar" vive aqui, uma vez so.
 * O que muda entre simulado e real e apenas o transporte injetado.
 * Por isso trocar Mock por BLE de verdade nao mexe em UI nem em ViewModel.
 */
open class BaseMidiDeviceService(
    final override val kind: DeviceKind,
    protected val transport: DeviceTransport,
    initialProfile: DeviceProfile,
    scope: CoroutineScope
) : ToneDeviceService {

    private val infoState = MutableStateFlow(
        DeviceInfo(kind = kind, transport = transport.kind)
    )
    private val profileState = MutableStateFlow(initialProfile)

    override val info: StateFlow<DeviceInfo> = infoState.asStateFlow()
    override val profile: StateFlow<DeviceProfile> = profileState.asStateFlow()
    override val incoming: SharedFlow<MidiMessage> get() = transport.incoming
    override val log: SharedFlow<TransportLogEntry> get() = transport.log

    init {
        transport.state
            .onEach { state ->
                infoState.value = infoState.value.copy(
                    state = state,
                    transport = transport.kind,
                    lastError = if (state == ConnectionState.ERROR) infoState.value.lastError else null
                )
            }
            .launchIn(scope)
    }

    override suspend fun scan(timeoutMs: Long): List<DiscoveredDevice> = transport.scan(timeoutMs)

    override suspend fun connect(address: String?): Result<Unit> {
        val result = transport.connect(address)
        result.onSuccess {
            infoState.value = infoState.value.copy(
                address = address ?: infoState.value.address,
                batteryPercent = transport.readBatteryPercent(),
                lastError = null
            )
        }.onFailure { e ->
            infoState.value = infoState.value.copy(lastError = e.message)
        }
        return result
    }

    override suspend fun disconnect() {
        transport.disconnect()
        infoState.value = infoState.value.copy(batteryPercent = null)
    }

    override suspend fun loadPreset(index: Int): Result<Unit> {
        val ready = requireConnected() ?: return failNotConnected()
        return transport.send(ready.programMessage(index))
    }

    override suspend fun savePreset(index: Int): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException(
                "Gravar patch no " + kind.displayName + " exige o protocolo proprietário (SysEx), " +
                    "que ainda não foi mapeado. O preset foi salvo apenas no aplicativo."
            )
        )
    }

    override suspend fun setParam(param: ParamId, value: Float): Result<Unit> {
        val currentProfile = requireConnected() ?: return failNotConnected()
        val message = currentProfile.messageFor(param, MacroResolver.normalize(value))
            ?: return Result.failure(ParameterNotMappedException(param, kind))
        return transport.send(message)
    }

    override suspend fun setEffect(slot: EffectSlot, on: Boolean): Result<Unit> {
        val param = when (slot) {
            EffectSlot.DELAY -> ParamId.DELAY
            EffectSlot.REVERB -> ParamId.REVERB
            EffectSlot.BOOST -> ParamId.BOOST
            EffectSlot.GATE -> ParamId.GATE
            EffectSlot.MODULATION -> ParamId.MODULATION
        }
        return setParam(param, if (on) 10f else 0f)
    }

    override suspend fun applyAmpParams(params: AmpParams): Result<Unit> {
        var lastError: Throwable? = null
        var sent = 0
        for (p in AmpParam.ordered) {
            val id = p.toParamId()
            val r = setParam(id, params.get(p))
            r.onSuccess { sent++ }
            r.onFailure { e -> if (e !is ParameterNotMappedException) lastError = e }
        }
        return when {
            lastError != null -> Result.failure(lastError!!)
            sent == 0 -> Result.failure(
                IllegalStateException(
                    "Nenhum parâmetro do " + kind.displayName + " está mapeado ainda."
                )
            )
            else -> Result.success(Unit)
        }
    }

    override suspend fun applyEffects(effects: EffectState): Result<Unit> {
        var lastError: Throwable? = null
        for (slot in EffectSlot.entries) {
            setEffect(slot, effects.isOn(slot)).onFailure { e ->
                if (e !is ParameterNotMappedException) lastError = e
            }
        }
        return lastError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun applyPreset(preset: Preset): Result<Unit> {
        val program = when (kind) {
            DeviceKind.TANK_G -> preset.tankGProgram
            DeviceKind.CODE50 -> preset.codeProgram
        }
        if (program != null) {
            loadPreset(program).onFailure { return Result.failure(it) }
        }
        val amp = MacroResolver.resolve(preset.macros)
        applyAmpParams(amp)
        applyEffects(preset.effects)
        return Result.success(Unit)
    }

    override fun updateProfile(profile: DeviceProfile) {
        profileState.value = profile
    }

    protected fun setBattery(percent: Int?) {
        infoState.value = infoState.value.copy(batteryPercent = percent)
    }

    private fun requireConnected(): DeviceProfile? =
        if (transport.state.value == ConnectionState.CONNECTED) profileState.value else null

    private fun failNotConnected(): Result<Unit> =
        Result.failure(DeviceNotConnectedException(kind))
}

private fun AmpParam.toParamId(): ParamId = when (this) {
    AmpParam.GAIN -> ParamId.GAIN
    AmpParam.BASS -> ParamId.BASS
    AmpParam.MIDDLE -> ParamId.MIDDLE
    AmpParam.TREBLE -> ParamId.TREBLE
    AmpParam.PRESENCE -> ParamId.PRESENCE
    AmpParam.RESONANCE -> ParamId.RESONANCE
    AmpParam.VOLUME -> ParamId.VOLUME
}
