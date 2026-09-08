package com.srlakes.tone.device.api

import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.model.DiscoveredDevice
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.Preset
import com.srlakes.tone.protocol.DeviceProfile
import com.srlakes.tone.protocol.MidiMessage
import com.srlakes.tone.protocol.ParamId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato comum aos dois aparelhos. Toda a UI conversa somente com
 * esta interface - nunca com Bluetooth diretamente.
 *
 * Valores de parametro sao sempre 0..10, como nos knobs de um amp.
 * A conversao para MIDI acontece no DeviceProfile.
 */
interface ToneDeviceService {

    val kind: DeviceKind

    val info: StateFlow<DeviceInfo>

    val profile: StateFlow<DeviceProfile>

    val incoming: SharedFlow<MidiMessage>

    val log: SharedFlow<TransportLogEntry>

    suspend fun scan(timeoutMs: Long = 6000L): List<DiscoveredDevice>

    suspend fun connect(address: String? = null): Result<Unit>

    suspend fun disconnect()

    /** Carrega um patch interno do aparelho pelo indice. */
    suspend fun loadPreset(index: Int): Result<Unit>

    /**
     * Grava no aparelho. Enquanto o protocolo proprietario nao existir,
     * isto falha de forma explicita em vez de fingir que funcionou.
     */
    suspend fun savePreset(index: Int): Result<Unit>

    suspend fun setParam(param: ParamId, value: Float): Result<Unit>

    suspend fun setEffect(slot: EffectSlot, on: Boolean): Result<Unit>

    /** Envia um conjunto completo de parametros. */
    suspend fun applyAmpParams(params: AmpParams): Result<Unit>

    suspend fun applyEffects(effects: EffectState): Result<Unit>

    /** Aplica a parte deste aparelho que o preset descreve. */
    suspend fun applyPreset(preset: Preset): Result<Unit>

    /** Substitui o perfil (usado pelo MIDI Learn / Protocol Lab). */
    fun updateProfile(profile: DeviceProfile)
}

/**
 * M-VAVE TANK-G.
 *
 * Os metodos nomeados sao acucar sobre setParam, para o codigo de cima
 * ficar legivel. O protocolo real entra em :device:blemidi na fase 2.
 */
interface TankGService : ToneDeviceService {
    suspend fun setAmp(index: Int): Result<Unit> = loadPreset(index)
    suspend fun setGain(value: Float): Result<Unit> = setParam(ParamId.GAIN, value)
    suspend fun setBass(value: Float): Result<Unit> = setParam(ParamId.BASS, value)
    suspend fun setMid(value: Float): Result<Unit> = setParam(ParamId.MIDDLE, value)
    suspend fun setTreble(value: Float): Result<Unit> = setParam(ParamId.TREBLE, value)
    suspend fun setDelay(on: Boolean): Result<Unit> = setEffect(EffectSlot.DELAY, on)
    suspend fun setReverb(on: Boolean): Result<Unit> = setEffect(EffectSlot.REVERB, on)
    suspend fun setGate(on: Boolean): Result<Unit> = setEffect(EffectSlot.GATE, on)
    suspend fun setIR(index: Int): Result<Unit> = setParam(ParamId.IR, index.toFloat())
}

/**
 * Marshall CODE50.
 */
interface MarshallCodeService : ToneDeviceService {
    suspend fun setPreamp(index: Int): Result<Unit> = setParam(ParamId.PREAMP, index.toFloat())
    suspend fun setGain(value: Float): Result<Unit> = setParam(ParamId.GAIN, value)
    suspend fun setBass(value: Float): Result<Unit> = setParam(ParamId.BASS, value)
    suspend fun setMiddle(value: Float): Result<Unit> = setParam(ParamId.MIDDLE, value)
    suspend fun setTreble(value: Float): Result<Unit> = setParam(ParamId.TREBLE, value)
    suspend fun setVolume(value: Float): Result<Unit> = setParam(ParamId.VOLUME, value)
    suspend fun setPresence(value: Float): Result<Unit> = setParam(ParamId.PRESENCE, value)
    suspend fun setResonance(value: Float): Result<Unit> = setParam(ParamId.RESONANCE, value)
    suspend fun setPreFX(on: Boolean): Result<Unit> = setEffect(EffectSlot.BOOST, on)
    suspend fun setModulation(on: Boolean): Result<Unit> = setEffect(EffectSlot.MODULATION, on)
    suspend fun setDelay(on: Boolean): Result<Unit> = setEffect(EffectSlot.DELAY, on)
    suspend fun setReverb(on: Boolean): Result<Unit> = setEffect(EffectSlot.REVERB, on)
    suspend fun setPowerAmp(index: Int): Result<Unit> = setParam(ParamId.POWER_AMP, index.toFloat())
    suspend fun setCabinet(index: Int): Result<Unit> = setParam(ParamId.CABINET, index.toFloat())
}

/** Erro usado quando o protocolo ainda nao foi mapeado para aquele parametro. */
class ParameterNotMappedException(param: ParamId, device: DeviceKind) : Exception(
    "O parâmetro " + param.label + " ainda não está mapeado para o " + device.displayName +
        ". Use a tela Dispositivos > Protocol Lab para aprender o CC correspondente."
)

class DeviceNotConnectedException(device: DeviceKind) : Exception(
    device.displayName + " não está conectado."
)
