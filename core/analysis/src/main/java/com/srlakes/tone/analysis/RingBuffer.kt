package com.srlakes.tone.analysis

/**
 * Buffer circular de floats. O produtor (thread de audio) escreve,
 * o consumidor (thread de analise) le janelas com sobreposicao.
 * Uso de uma thread so nas duas pontas -> sem locks.
 */
class FloatRingBuffer(val capacity: Int) {

    private val data = FloatArray(capacity)
    private var writeIndex = 0
    private var filled = 0

    val available: Int get() = filled

    fun write(src: FloatArray, count: Int = src.size) {
        var i = 0
        while (i < count) {
            val chunk = minOf(count - i, capacity - writeIndex)
            System.arraycopy(src, i, data, writeIndex, chunk)
            writeIndex = (writeIndex + chunk) % capacity
            i += chunk
        }
        filled = minOf(capacity, filled + count)
    }

    /**
     * Copia as [out.size] amostras mais recentes para [out].
     * Retorna false se ainda nao houver amostras suficientes.
     */
    fun readLatest(out: FloatArray): Boolean {
        val n = out.size
        if (filled < n) return false
        var start = writeIndex - n
        if (start < 0) start += capacity
        val firstChunk = minOf(n, capacity - start)
        System.arraycopy(data, start, out, 0, firstChunk)
        if (firstChunk < n) {
            System.arraycopy(data, 0, out, firstChunk, n - firstChunk)
        }
        return true
    }

    fun clear() {
        writeIndex = 0
        filled = 0
        java.util.Arrays.fill(data, 0f)
    }
}
