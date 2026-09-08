package com.srlakes.tone.analysis

import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AudioAnalysis
import com.srlakes.tone.model.ToneBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FftTest {

    @Test
    fun `senoide de 1 kHz cai no bin correto`() {
        val size = 2048
        val sampleRate = 48000
        val fft = Fft(size)
        val window = Fft.hannWindow(size)
        val re = FloatArray(size) { (sin(2.0 * PI * 1000.0 * it / sampleRate) * window[it]).toFloat() }
        val im = FloatArray(size)
        fft.transform(re, im)

        var best = 0
        var bestPower = 0f
        for (i in 1 until size / 2) {
            val p = re[i] * re[i] + im[i] * im[i]
            if (p > bestPower) { bestPower = p; best = i }
        }
        val hz = best * sampleRate.toFloat() / size
        assertEquals(1000f, hz, 30f)
    }

    @Test
    fun `dc puro nao vaza para bins altos`() {
        val size = 512
        val fft = Fft(size)
        val re = FloatArray(size) { 1f }
        val im = FloatArray(size)
        fft.transform(re, im)
        val dc = re[0] * re[0] + im[0] * im[0]
        val high = re[200] * re[200] + im[200] * im[200]
        assertTrue("DC deveria dominar", dc > high * 1000)
    }
}

class LevelMeterTest {

    @Test
    fun `onda plena da aproximadamente 0 dBFS de pico`() {
        val meter = LevelMeterProcessor(48000)
        val samples = FloatArray(4800) { sin(2.0 * PI * 440.0 * it / 48000).toFloat() }
        val r = meter.process(samples)
        assertEquals(0f, r.peakDb, 0.5f)
        // RMS de uma senoide = pico / raiz(2) => cerca de -3 dB
        assertEquals(-3.01f, r.rmsDb, 0.5f)
    }

    @Test
    fun `detecta clipping`() {
        val meter = LevelMeterProcessor(48000)
        val samples = FloatArray(1000) { 1f }
        assertTrue(meter.process(samples).clipping)
    }
}

class AudioAnalyzerTest {

    @Test
    fun `analisador aponta a frequencia dominante`() {
        val analyzer = AudioAnalyzer(AnalyzerSettings.DEFAULT.copy(spectrumAveraging = 1), 48000)
        val sampleRate = 48000
        var result: AudioAnalysis? = null
        var phase = 0.0
        repeat(20) {
            val block = ShortArray(2048) {
                val v = sin(2.0 * PI * 440.0 * phase / sampleRate) * 0.5
                phase += 1.0
                (v * 32767).toInt().toShort()
            }
            analyzer.push(block)?.let { result = it }
        }
        val r = requireNotNull(result) { "o analisador deveria ter produzido um quadro" }
        assertEquals(440f, r.dominantFrequency, 25f)
        assertTrue("energia deveria estar em LOW_MID", r.bands.share(ToneBand.LOW_MID) > 0.5f)
    }

    @Test
    fun `silencio nao gera clipping nem sinal`() {
        val analyzer = AudioAnalyzer(AnalyzerSettings.DEFAULT, 48000)
        var result: AudioAnalysis? = null
        repeat(10) { analyzer.push(ShortArray(2048))?.let { result = it } }
        val r = requireNotNull(result)
        assertTrue(!r.clipping)
        assertTrue(!r.hasSignal)
    }
}

class SnapshotTest {

    @Test
    fun `base x solo detecta solo baixo`() {
        val base = snapshot("BASE", -16.8f)
        val solo = snapshot("SOLO", -16.0f)
        val r = LoudnessComparator.compareBaseSolo(base, solo)
        assertEquals(com.srlakes.tone.model.LoudnessVerdict.SOLO_TOO_LOW, r.verdict)
        assertEquals(0.8f, r.deltaRmsDb, 0.01f)
    }

    @Test
    fun `base x solo aprova diferenca saudavel`() {
        val r = LoudnessComparator.compareBaseSolo(snapshot("BASE", -16.8f), snapshot("SOLO", -13.5f))
        assertEquals(com.srlakes.tone.model.LoudnessVerdict.BALANCED, r.verdict)
        assertEquals(3.3f, r.deltaRmsDb, 0.01f)
    }

    private fun snapshot(label: String, rms: Float) = com.srlakes.tone.model.ToneSnapshot(
        label = label,
        capturedAtMs = 0L,
        durationMs = 8000L,
        rmsDb = rms,
        peakDb = rms + 8f,
        noiseFloorDb = -62f,
        crestFactorDb = 8f,
        bandDb = List(5) { -20f },
        bandShare = List(5) { 0.2f },
        averageSpectrum = List(192) { -40f },
        clipped = false
    )
}
