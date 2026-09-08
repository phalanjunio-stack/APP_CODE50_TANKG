package com.srlakes.tone.analysis

import com.srlakes.tone.model.AudioAnalysis
import com.srlakes.tone.model.ToneBand
import com.srlakes.tone.model.ToneSnapshot

/**
 * Acumula alguns segundos de analise e produz um ToneSnapshot.
 * Base do A/B e do BASE x SOLO.
 *
 * As medias sao feitas no dominio de POTENCIA, nao em dB. Media de dB
 * e matematicamente errada e subestima os picos.
 *
 * Os quadros abaixo do gate (ruido + sensibilidade) sao descartados:
 * queremos a intensidade do que foi TOCADO, nao dos silencios entre frases.
 */
class SnapshotRecorder(
    private val label: String,
    private val sensitivityDb: Float = 8f,
    private val spectrumPoints: Int = 192
) {

    private var frames = 0
    private var gatedFrames = 0
    private var rmsPowerSum = 0.0
    private var peakDb = Db.FLOOR
    private var noiseFloorDb = 0f
    private var crestSum = 0.0
    private var clipped = false
    private var startedAtMs = 0L

    private val bandPowerSum = DoubleArray(ToneBand.ordered.size)
    private val bandShareSum = DoubleArray(ToneBand.ordered.size)
    private var spectrumPowerSum = DoubleArray(spectrumPoints)

    val frameCount: Int get() = frames
    val gatedFrameCount: Int get() = gatedFrames

    fun add(a: AudioAnalysis) {
        if (frames == 0) {
            startedAtMs = a.timestampMs
            noiseFloorDb = a.noiseFloorDb
        }
        frames++
        noiseFloorDb = minOf(noiseFloorDb, a.noiseFloorDb)
        if (a.clipping) clipped = true

        val gate = a.noiseFloorDb + sensitivityDb
        if (a.rmsDb < gate) return

        gatedFrames++
        rmsPowerSum += powerOf(a.rmsDb)
        peakDb = maxOf(peakDb, a.peakDb)
        crestSum += a.crestFactorDb.toDouble()

        for (i in bandPowerSum.indices) {
            bandPowerSum[i] += powerOf(a.bands.db(ToneBand.ordered[i]))
            bandShareSum[i] += a.bands.share(ToneBand.ordered[i]).toDouble()
        }

        if (spectrumPowerSum.size != a.spectrum.size) {
            spectrumPowerSum = DoubleArray(a.spectrum.size)
        }
        for (i in a.spectrum.indices) {
            spectrumPowerSum[i] += powerOf(a.spectrum[i])
        }
    }

    fun build(endedAtMs: Long = System.currentTimeMillis()): ToneSnapshot {
        val n = gatedFrames.coerceAtLeast(1)
        return ToneSnapshot(
            label = label,
            capturedAtMs = if (startedAtMs == 0L) endedAtMs else startedAtMs,
            durationMs = if (startedAtMs == 0L) 0L else endedAtMs - startedAtMs,
            rmsDb = dbOf(rmsPowerSum / n),
            peakDb = peakDb,
            noiseFloorDb = noiseFloorDb,
            crestFactorDb = (crestSum / n).toFloat(),
            bandDb = bandPowerSum.map { dbOf(it / n) },
            bandShare = bandShareSum.map { (it / n).toFloat() },
            averageSpectrum = spectrumPowerSum.map { dbOf(it / n) },
            clipped = clipped
        )
    }

    /** Verdadeiro quando ha material suficiente para a medicao valer. */
    fun isUsable(): Boolean = gatedFrames >= 5

    private fun powerOf(db: Float): Double = Math.pow(10.0, db.toDouble() / 10.0)

    private fun dbOf(power: Double): Float =
        (10.0 * Math.log10(power.coerceAtLeast(1e-12))).toFloat().coerceAtLeast(Db.FLOOR)
}
