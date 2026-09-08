package com.srlakes.tone.analysis

import com.srlakes.tone.model.BandLevels
import com.srlakes.tone.model.ToneBand

/**
 * Soma a potencia dos bins da FFT dentro de cada faixa e devolve
 * energia em dB e a proporcao relativa de cada faixa.
 */
class ToneBandProcessor(
    private val binHz: Float,
    private val binCount: Int
) {

    private val ranges: Array<IntRange> = ToneBand.ordered.map { band ->
        val from = (band.lowHz / binHz).toInt().coerceIn(1, binCount - 1)
        val to = (band.highHz / binHz).toInt().coerceIn(from, binCount - 1)
        from..to
    }.toTypedArray()

    /**
     * @param power potencia linear por bin (magnitude ao quadrado).
     */
    fun process(power: FloatArray): BandLevels {
        val sums = FloatArray(ToneBand.ordered.size)
        var total = 0f
        for (i in sums.indices) {
            var acc = 0f
            for (bin in ranges[i]) acc += power[bin]
            sums[i] = acc
            total += acc
        }
        val db = FloatArray(sums.size) { Db.fromPower(sums[it]) }
        val share = FloatArray(sums.size) {
            if (total <= 0f) 0f else (sums[it] / total)
        }
        return BandLevels(db, share)
    }

    /** Energia relativa de uma faixa arbitraria (usado pelas regras de Tone Insights). */
    fun shareOf(power: FloatArray, lowHz: Float, highHz: Float): Float {
        var acc = 0f
        var total = 0f
        val from = (lowHz / binHz).toInt().coerceIn(1, binCount - 1)
        val to = (highHz / binHz).toInt().coerceIn(from, binCount - 1)
        for (bin in 1 until binCount) {
            val p = power[bin]
            total += p
            if (bin in from..to) acc += p
        }
        return if (total <= 0f) 0f else acc / total
    }
}
