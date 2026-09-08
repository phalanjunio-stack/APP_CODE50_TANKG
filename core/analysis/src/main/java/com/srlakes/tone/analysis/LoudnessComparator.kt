package com.srlakes.tone.analysis

import com.srlakes.tone.model.BaseSoloResult
import com.srlakes.tone.model.LoudnessVerdict
import com.srlakes.tone.model.ToneSnapshot

/**
 * Comparacao de intensidade entre duas medicoes.
 *
 * Valido apenas se as duas capturas foram feitas com o celular na MESMA
 * posicao e sem mexer no ganho de medicao. Por isso o app avisa quando o
 * piso de ruido muda demais entre as duas: e sinal de que algo se moveu.
 */
object LoudnessComparator {

    /** Um solo costuma pedir de +2 a +6 dB sobre a base. */
    const val MIN_HEALTHY_DELTA = 2.0f
    const val MAX_HEALTHY_DELTA = 6.0f

    /** Diferenca de piso de ruido acima da qual a comparacao fica suspeita. */
    const val NOISE_DRIFT_TOLERANCE = 6.0f

    fun compareBaseSolo(base: ToneSnapshot, solo: ToneSnapshot): BaseSoloResult {
        val deltaRms = solo.rmsDb - base.rmsDb
        val verdict = when {
            deltaRms < MIN_HEALTHY_DELTA -> LoudnessVerdict.SOLO_TOO_LOW
            deltaRms > MAX_HEALTHY_DELTA -> LoudnessVerdict.SOLO_TOO_LOUD
            else -> LoudnessVerdict.BALANCED
        }
        return BaseSoloResult(
            base = base,
            solo = solo,
            deltaRmsDb = deltaRms,
            deltaPeakDb = solo.peakDb - base.peakDb,
            verdict = verdict
        )
    }

    /**
     * @return null se a comparacao parece confiavel, ou um aviso em texto.
     */
    fun reliabilityWarning(a: ToneSnapshot, b: ToneSnapshot): String? {
        val drift = Math.abs(a.noiseFloorDb - b.noiseFloorDb)
        return when {
            drift > NOISE_DRIFT_TOLERANCE ->
                "O piso de ruído mudou ${fmt(drift)} dB entre as duas medições. " +
                    "O celular ou o ambiente provavelmente mudaram — a comparação pode não ser válida."
            a.clipped || b.clipped ->
                "Houve clipping durante a captura. Os valores de pico não são confiáveis."
            else -> null
        }
    }

    private fun fmt(v: Float): String = String.format("%.1f", v)
}
