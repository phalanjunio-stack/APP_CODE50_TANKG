package com.srlakes.tone.device.api

import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.MacroSet
import com.srlakes.tone.model.Preset
import com.srlakes.tone.protocol.MacroResolver
import com.srlakes.tone.protocol.ParamId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Ponto unico por onde a UI fala com os aparelhos.
 *
 * Regra do projeto: nenhuma tela importa :device:blemidi ou :device:mock.
 * Todas conhecem apenas esta classe e as interfaces de :device:api.
 * Trocar simulado por real e trocar a injecao no AppContainer.
 */
class DeviceManager(
    val tankG: TankGService,
    val code50: MarshallCodeService
) {

    val devices: List<ToneDeviceService> = listOf(tankG, code50)

    val infos: Flow<List<DeviceInfo>> = combine(tankG.info, code50.info) { a, b -> listOf(a, b) }

    val anyConnected: Flow<Boolean> = combine(tankG.info, code50.info) { a, b ->
        a.connected || b.connected
    }

    /** Envia o preset para os dois aparelhos, cada um pegando a sua parte. */
    suspend fun applyPreset(preset: Preset): DeviceOutcome {
        val results = devices.map { it to it.applyPreset(preset) }
        return DeviceOutcome.from(results)
    }

    /** Um macro mudou: reenvia so o que aquele macro afeta. */
    suspend fun applyMacros(macros: MacroSet): DeviceOutcome {
        val amp = MacroResolver.resolve(macros)
        return applyAmpParams(amp)
    }

    suspend fun applyAmpParams(params: AmpParams): DeviceOutcome {
        val results = devices.map { it to it.applyAmpParams(params) }
        return DeviceOutcome.from(results)
    }

    suspend fun setEffect(slot: EffectSlot, on: Boolean): DeviceOutcome {
        val results = devices.map { it to it.setEffect(slot, on) }
        return DeviceOutcome.from(results)
    }

    suspend fun applyEffects(effects: EffectState): DeviceOutcome {
        val results = devices.map { it to it.applyEffects(effects) }
        return DeviceOutcome.from(results)
    }

    suspend fun setParam(param: ParamId, value: Float): DeviceOutcome {
        val results = devices.map { it to it.setParam(param, value) }
        return DeviceOutcome.from(results)
    }

    suspend fun disconnectAll() {
        devices.forEach { it.disconnect() }
    }
}

/**
 * Resultado agregado. Guarda o que deu certo e o que nao deu, para a UI
 * poder avisar sem travar o show.
 */
data class DeviceOutcome(
    val delivered: Int,
    val failures: List<String>
) {
    val fullyDelivered: Boolean get() = failures.isEmpty() && delivered > 0
    val nothingDelivered: Boolean get() = delivered == 0

    companion object {
        fun from(results: List<Pair<ToneDeviceService, Result<Unit>>>): DeviceOutcome {
            var ok = 0
            val errors = ArrayList<String>()
            for ((service, result) in results) {
                result.onSuccess { ok++ }
                result.onFailure { e ->
                    if (e !is DeviceNotConnectedException) {
                        errors += service.kind.displayName + ": " + (e.message ?: "falha desconhecida")
                    }
                }
            }
            return DeviceOutcome(ok, errors)
        }
    }
}
