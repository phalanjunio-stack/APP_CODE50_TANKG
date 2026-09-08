package com.srlakes.tone.analysis

import kotlin.math.log10
import kotlin.math.max

object Db {
    const val FLOOR = -120f
    private const val EPS = 1e-7f

    /** Amplitude linear (0..1) para dBFS. */
    fun fromAmplitude(a: Float): Float =
        max(FLOOR, 20f * log10(max(a, EPS)))

    /** Potencia linear para dBFS. */
    fun fromPower(p: Float): Float =
        max(FLOOR, 10f * log10(max(p, EPS * EPS)))

    fun toAmplitude(db: Float): Float =
        Math.pow(10.0, (db / 20f).toDouble()).toFloat()

    /** Normaliza dBFS para 0..1 usando uma janela de exibicao. */
    fun normalize(db: Float, minDb: Float = -60f, maxDb: Float = 0f): Float =
        ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
}
