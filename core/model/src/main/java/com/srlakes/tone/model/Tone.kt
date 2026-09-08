package com.srlakes.tone.model

/**
 * Macros apresentados ao musico. Cada macro resolve internamente varios
 * parametros do amp/pedal (ver MacroResolver em :core:protocol).
 * Faixa 0.0 .. 10.0 como nos knobs de um amplificador.
 */
data class MacroSet(
    val drive: Float = 5f,
    val warmth: Float = 5f,
    val body: Float = 5f,
    val presence: Float = 5f,
    val volume: Float = 5f
) {
    fun get(macro: Macro): Float = when (macro) {
        Macro.DRIVE -> drive
        Macro.WARMTH -> warmth
        Macro.BODY -> body
        Macro.PRESENCE -> presence
        Macro.VOLUME -> volume
    }

    fun with(macro: Macro, value: Float): MacroSet {
        val v = value.coerceIn(0f, 10f)
        return when (macro) {
            Macro.DRIVE -> copy(drive = v)
            Macro.WARMTH -> copy(warmth = v)
            Macro.BODY -> copy(body = v)
            Macro.PRESENCE -> copy(presence = v)
            Macro.VOLUME -> copy(volume = v)
        }
    }

    companion object {
        val DEFAULT = MacroSet()
    }
}

enum class Macro(val label: String) {
    DRIVE("Drive"),
    WARMTH("Warmth"),
    BODY("Body"),
    PRESENCE("Presence"),
    VOLUME("Volume");

    companion object {
        val ordered = listOf(DRIVE, WARMTH, BODY, PRESENCE, VOLUME)
    }
}

/** Parametros individuais (tela avancada). Faixa 0.0 .. 10.0. */
data class AmpParams(
    val gain: Float = 5f,
    val bass: Float = 5f,
    val middle: Float = 5f,
    val treble: Float = 5f,
    val presence: Float = 5f,
    val resonance: Float = 5f,
    val volume: Float = 5f
) {
    fun get(p: AmpParam): Float = when (p) {
        AmpParam.GAIN -> gain
        AmpParam.BASS -> bass
        AmpParam.MIDDLE -> middle
        AmpParam.TREBLE -> treble
        AmpParam.PRESENCE -> presence
        AmpParam.RESONANCE -> resonance
        AmpParam.VOLUME -> volume
    }

    fun with(p: AmpParam, value: Float): AmpParams {
        val v = value.coerceIn(0f, 10f)
        return when (p) {
            AmpParam.GAIN -> copy(gain = v)
            AmpParam.BASS -> copy(bass = v)
            AmpParam.MIDDLE -> copy(middle = v)
            AmpParam.TREBLE -> copy(treble = v)
            AmpParam.PRESENCE -> copy(presence = v)
            AmpParam.RESONANCE -> copy(resonance = v)
            AmpParam.VOLUME -> copy(volume = v)
        }
    }

    companion object {
        val DEFAULT = AmpParams()
    }
}

enum class AmpParam(val label: String) {
    GAIN("Gain"),
    BASS("Bass"),
    MIDDLE("Middle"),
    TREBLE("Treble"),
    PRESENCE("Presence"),
    RESONANCE("Resonance"),
    VOLUME("Volume");

    companion object {
        val ordered = listOf(GAIN, BASS, MIDDLE, TREBLE, PRESENCE, RESONANCE, VOLUME)
    }
}

/** Liga/desliga dos efeitos rapidos. */
data class EffectState(
    val delay: Boolean = false,
    val reverb: Boolean = true,
    val boost: Boolean = false,
    val gate: Boolean = true,
    val modulation: Boolean = false
) {
    fun isOn(slot: EffectSlot): Boolean = when (slot) {
        EffectSlot.DELAY -> delay
        EffectSlot.REVERB -> reverb
        EffectSlot.BOOST -> boost
        EffectSlot.GATE -> gate
        EffectSlot.MODULATION -> modulation
    }

    fun toggle(slot: EffectSlot): EffectState = set(slot, !isOn(slot))

    fun set(slot: EffectSlot, on: Boolean): EffectState = when (slot) {
        EffectSlot.DELAY -> copy(delay = on)
        EffectSlot.REVERB -> copy(reverb = on)
        EffectSlot.BOOST -> copy(boost = on)
        EffectSlot.GATE -> copy(gate = on)
        EffectSlot.MODULATION -> copy(modulation = on)
    }

    companion object {
        val DEFAULT = EffectState()
    }
}

/**
 * Um preset pode apontar para um patch do TANK-G, um patch do CODE50, ou ambos.
 * Os campos de programa sao null enquanto o protocolo real nao estiver mapeado.
 */
data class Preset(
    val id: Long = 0L,
    val name: String,
    val category: PresetCategory,
    val macros: MacroSet = MacroSet.DEFAULT,
    val amp: AmpParams = AmpParams.DEFAULT,
    val effects: EffectState = EffectState.DEFAULT,
    val tankGProgram: Int? = null,
    val codeProgram: Int? = null,
    val notes: String = "",
    val builtIn: Boolean = false
)
