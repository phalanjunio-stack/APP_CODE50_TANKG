package com.srlakes.tone.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FFT radix-2 iterativa (Cooley-Tukey), in-place, sem dependencias externas.
 * Tabelas de twiddle e bit-reversal pre-computadas no construtor:
 * o metodo transform nao aloca nada, pode rodar a 40+ vezes por segundo
 * sem gerar lixo para o GC.
 *
 * NUNCA chamar na thread de UI.
 */
class Fft(val size: Int) {

    init {
        require(size >= 2 && (size and (size - 1)) == 0) {
            "O tamanho da FFT precisa ser potencia de 2 (recebido: $size)"
        }
    }

    private val half = size / 2
    private val cosTable = FloatArray(half) { cos(2.0 * PI * it / size).toFloat() }
    private val sinTable = FloatArray(half) { sin(2.0 * PI * it / size).toFloat() }
    private val reverse = IntArray(size).also { table ->
        var bits = 0
        var n = size
        while (n > 1) { n = n shr 1; bits++ }
        for (i in 0 until size) {
            var x = i
            var r = 0
            for (b in 0 until bits) {
                r = (r shl 1) or (x and 1)
                x = x shr 1
            }
            table[i] = r
        }
    }

    /**
     * Transformada in-place. re e im precisam ter exatamente [size] posicoes.
     */
    fun transform(re: FloatArray, im: FloatArray) {
        require(re.size == size && im.size == size)

        // Reordenacao por bit-reversal
        for (i in 0 until size) {
            val j = reverse[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }

        var len = 2
        while (len <= size) {
            val step = size / len
            val halfLen = len / 2
            var i = 0
            while (i < size) {
                var k = 0
                for (j in i until i + halfLen) {
                    val wr = cosTable[k]
                    val wi = -sinTable[k]
                    val jj = j + halfLen
                    val tr = re[jj] * wr - im[jj] * wi
                    val ti = re[jj] * wi + im[jj] * wr
                    re[jj] = re[j] - tr
                    im[jj] = im[j] - ti
                    re[j] += tr
                    im[j] += ti
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
    }

    companion object {
        /** Janela de Hann normalizada, para reduzir vazamento espectral. */
        fun hannWindow(n: Int): FloatArray =
            FloatArray(n) { (0.5 - 0.5 * cos(2.0 * PI * it / (n - 1))).toFloat() }

        /** Ganho coerente da janela de Hann, usado para corrigir a amplitude. */
        const val HANN_COHERENT_GAIN = 0.5f
    }
}
