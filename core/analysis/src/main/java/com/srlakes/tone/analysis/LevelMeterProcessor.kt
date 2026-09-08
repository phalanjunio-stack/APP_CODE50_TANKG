package com.srlakes.tone.analysis

import kotlin.math.abs
import kotlin.math.sqrt

data class LevelReading(
    val peakDb: Float,
    val rmsDb: Float,
    val noiseFloorDb: Float,
    val clipping: Boolean,
    val crestFactorDb: Float,
    val dynamicRangeDb: Float
)

/**
 * Peak, RMS, noise floor, clipping e crest factor.
 *
 * O noise floor e um seguidor de minimo lento: cai rapido quando o sinal
 * some e sobe devagar, para nao ser puxado pelas notas.
 *
 * ATENCAO: tudo aqui e dBFS (fundo de escala digital), nao dB SPL.
 * O microfone do celular nao e calibrado; so a DIFERENCA entre duas
 * medicoes feitas na mesma posicao tem significado.
 */
class LevelMeterProcessor(
    private val sampleRate: Int,
    /** Amostras acima disso contam como clipping. */
    private val clipThreshold: Float = 0.985f,
    /** Quantas amostras estouradas seguidas ja acusam clipping. */
    private val clipCountThreshold: Int = 3
) {

    private var peakHold = 0f
    private var peakHoldFrames = 0
    private var noiseFloor = Db.FLOOR
    private var rmsMax = Db.FLOOR
    private var rmsMin = 0f
    private var initialized = false

    private val peakHoldMaxFrames = 20

    fun reset() {
        peakHold = 0f
        peakHoldFrames = 0
        noiseFloor = Db.FLOOR
        rmsMax = Db.FLOOR
        rmsMin = 0f
        initialized = false
    }

    fun process(samples: FloatArray, count: Int = samples.size): LevelReading {
        var peak = 0f
        var sumSquares = 0.0
        var clipRun = 0
        var maxClipRun = 0

        for (i in 0 until count) {
            val s = samples[i]
            val a = abs(s)
            if (a > peak) peak = a
            sumSquares += (s.toDouble() * s.toDouble())
            if (a >= clipThreshold) {
                clipRun++
                if (clipRun > maxClipRun) maxClipRun = clipRun
            } else {
                clipRun = 0
            }
        }

        val rms = sqrt(sumSquares / count.coerceAtLeast(1)).toFloat()
        val rmsDb = Db.fromAmplitude(rms)

        // Peak hold com decaimento
        if (peak >= peakHold) {
            peakHold = peak
            peakHoldFrames = 0
        } else {
            peakHoldFrames++
            if (peakHoldFrames > peakHoldMaxFrames) {
                peakHold *= 0.88f
            }
        }
        val peakDb = Db.fromAmplitude(peakHold)

        if (!initialized) {
            noiseFloor = rmsDb
            rmsMax = rmsDb
            rmsMin = rmsDb
            initialized = true
        } else {
            // Cai rapido (silencio novo), sobe devagar (nao seguir notas)
            noiseFloor = if (rmsDb < noiseFloor) {
                noiseFloor + (rmsDb - noiseFloor) * 0.35f
            } else {
                noiseFloor + (rmsDb - noiseFloor) * 0.0015f
            }
            rmsMax = maxOf(rmsMax * 0.9995f + rmsDb * 0.0005f, rmsDb).coerceAtLeast(Db.FLOOR)
            rmsMin = minOf(rmsMin, rmsDb)
        }

        val crest = (peakDb - rmsDb).coerceIn(0f, 60f)
        val dynamicRange = (rmsMax - noiseFloor).coerceIn(0f, 120f)

        return LevelReading(
            peakDb = peakDb,
            rmsDb = rmsDb,
            noiseFloorDb = noiseFloor,
            clipping = maxClipRun >= clipCountThreshold,
            crestFactorDb = crest,
            dynamicRangeDb = dynamicRange
        )
    }
}
