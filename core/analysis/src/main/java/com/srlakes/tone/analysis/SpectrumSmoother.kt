package com.srlakes.tone.analysis

/**
 * Suavizacao exponencial por bin, com ataque rapido e release lento.
 * Sem isso o grafico pisca de forma cansativa no palco.
 */
class SpectrumSmoother(
    size: Int,
    var factor: Float = 0.72f
) {
    private var state = FloatArray(size) { Db.FLOOR }

    fun resize(size: Int) {
        if (state.size != size) state = FloatArray(size) { Db.FLOOR }
    }

    fun smooth(input: FloatArray): FloatArray {
        resize(input.size)
        val release = factor.coerceIn(0f, 0.98f)
        val attack = release * 0.35f
        for (i in input.indices) {
            val target = input[i]
            val prev = state[i]
            val k = if (target > prev) attack else release
            state[i] = prev * k + target * (1f - k)
        }
        return state
    }

    fun reset() {
        java.util.Arrays.fill(state, Db.FLOOR)
    }
}

/**
 * Reduz os bins lineares da FFT para N pontos espacados logaritmicamente,
 * que e como o ouvido percebe e como plugins profissionais desenham.
 * Feito fora da UI para a tela so precisar percorrer um array pequeno.
 */
class LogSpectrumMapper(
    private val binHz: Float,
    private val binCount: Int,
    val points: Int = 192,
    private val minHz: Float = 20f,
    private val maxHz: Float = 20000f
) {
    private val edges: IntArray = IntArray(points + 1).also { arr ->
        val logMin = Math.log10(minHz.toDouble())
        val logMax = Math.log10(maxHz.toDouble())
        for (i in 0..points) {
            val hz = Math.pow(10.0, logMin + (logMax - logMin) * i / points)
            arr[i] = (hz / binHz).toInt().coerceIn(1, binCount - 1)
        }
    }

    private val out = FloatArray(points)

    /** @param spectrumDb magnitude em dB por bin. */
    fun map(spectrumDb: FloatArray): FloatArray {
        for (i in 0 until points) {
            var from = edges[i]
            val to = edges[i + 1]
            if (to <= from) from = to
            var peak = Db.FLOOR
            for (b in from..to) {
                val v = spectrumDb[b]
                if (v > peak) peak = v
            }
            out[i] = peak
        }
        return out
    }

    fun frequencyAt(index: Int): Float {
        val logMin = Math.log10(minHz.toDouble())
        val logMax = Math.log10(maxHz.toDouble())
        return Math.pow(10.0, logMin + (logMax - logMin) * index / points).toFloat()
    }
}
