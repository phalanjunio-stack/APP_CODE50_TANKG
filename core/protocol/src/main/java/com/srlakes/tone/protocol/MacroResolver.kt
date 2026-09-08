package com.srlakes.tone.protocol

import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.MacroSet

/**
 * O motor dos macros.
 *
 * O musico ve cinco controles. Cada um mexe em varios parametros reais
 * de forma coerente, do jeito que um tecnico de som mexeria.
 * A tela avancada continua expondo os parametros individuais.
 *
 * Todas as faixas sao 0..10, como nos knobs de um amplificador.
 */
object MacroResolver {

    /** Macros -> parametros individuais. */
    fun resolve(macros: MacroSet): AmpParams {
        val drive = macros.drive
        val warmth = c(macros.warmth)
        val body = c(macros.body)
        val presence = c(macros.presence)

        return AmpParams(
            gain = drive.coerceIn(0f, 10f),
            // Warmth engorda os graves e recua os agudos.
            bass = clamp(5f + warmth * 0.70f + body * 0.35f),
            // Body concentra os medios e o corpo do falante.
            middle = clamp(5f + body * 0.90f - presence * 0.15f),
            // Presence abre os agudos; warmth fecha.
            treble = clamp(5f - warmth * 0.60f + presence * 0.70f),
            presence = clamp(5f + presence * 0.90f - warmth * 0.35f),
            resonance = clamp(5f + body * 0.60f + warmth * 0.30f),
            volume = macros.volume.coerceIn(0f, 10f)
        )
    }

    /**
     * Aproximacao inversa, para quando o usuario edita a tela avancada
     * e o app precisa reposicionar os knobs de macro.
     * Nao e exata (o mapeamento nao e bijetivo) e nao precisa ser:
     * serve so para os macros nao ficarem mentindo na tela.
     */
    fun approximate(amp: AmpParams): MacroSet {
        val warmth = clamp(5f + ((amp.bass - 5f) * 0.55f - (amp.treble - 5f) * 0.45f))
        val body = clamp(5f + (amp.middle - 5f) * 0.85f + (amp.resonance - 5f) * 0.25f)
        val presence = clamp(5f + (amp.presence - 5f) * 0.75f + (amp.treble - 5f) * 0.35f)
        return MacroSet(
            drive = amp.gain,
            warmth = warmth,
            body = body,
            presence = presence,
            volume = amp.volume
        )
    }

    /** Parametro 0..10 para 0.0..1.0, que e o que a camada MIDI espera. */
    fun normalize(value: Float): Float = (value / 10f).coerceIn(0f, 1f)

    fun denormalize(value: Float): Float = (value * 10f).coerceIn(0f, 10f)

    /** Desvio a partir do centro. */
    private fun c(v: Float): Float = v.coerceIn(0f, 10f) - 5f

    private fun clamp(v: Float): Float = v.coerceIn(0f, 10f)
}
