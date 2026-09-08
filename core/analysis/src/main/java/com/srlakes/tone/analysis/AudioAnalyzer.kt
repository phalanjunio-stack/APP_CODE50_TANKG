package com.srlakes.tone.analysis

import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AudioAnalysis

/**
 * Orquestra todo o processamento de um bloco de audio.
 *
 * Fluxo: PCM -> ganho de medicao -> buffer circular -> janela de Hann ->
 * FFT -> magnitude -> suavizacao -> mapeamento logaritmico -> AudioAnalysis.
 *
 * Esta classe NAO toca no audio que vai para o amplificador. Ela so mede.
 * Por isso o app nao adiciona latencia nenhuma na guitarra.
 *
 * Nao e thread-safe: use uma instancia por thread de analise.
 */
class AudioAnalyzer(
    settings: AnalyzerSettings = AnalyzerSettings.DEFAULT,
    sampleRate: Int = 48000,
    private val displayPoints: Int = 192
) {

    var sampleRate: Int = sampleRate
        private set

    private var settings: AnalyzerSettings = settings

    private var fftSize = settings.fftSize
    private var hopSize = fftSize / 2

    private lateinit var fft: Fft
    private lateinit var window: FloatArray
    private lateinit var ring: FloatRingBuffer
    private lateinit var re: FloatArray
    private lateinit var im: FloatArray
    private lateinit var power: FloatArray
    private lateinit var magDb: FloatArray
    private lateinit var bandProcessor: ToneBandProcessor
    private lateinit var mapper: LogSpectrumMapper
    private lateinit var averagePool: Array<FloatArray>

    private val smoother = SpectrumSmoother(displayPoints, settings.spectrumSmoothing)
    private var levelMeter = LevelMeterProcessor(sampleRate)

    private var samplesSinceLastFrame = 0
    private var averageIndex = 0
    private var averageFilled = 0
    private var gainLinear = 1f
    private var scratch = FloatArray(0)

    init {
        rebuild()
    }

    fun updateSettings(newSettings: AnalyzerSettings, newSampleRate: Int = sampleRate) {
        val needsRebuild = newSettings.fftSize != fftSize ||
            newSampleRate != sampleRate ||
            newSettings.spectrumAveraging != settings.spectrumAveraging
        settings = newSettings
        sampleRate = newSampleRate
        smoother.factor = newSettings.spectrumSmoothing
        gainLinear = Db.toAmplitude(newSettings.inputGainDb)
        if (needsRebuild) rebuild()
    }

    fun reset() {
        ring.clear()
        smoother.reset()
        levelMeter.reset()
        samplesSinceLastFrame = 0
        averageIndex = 0
        averageFilled = 0
    }

    private fun rebuild() {
        fftSize = settings.fftSize
        hopSize = (fftSize / 2).coerceAtLeast(128)
        fft = Fft(fftSize)
        window = Fft.hannWindow(fftSize)
        ring = FloatRingBuffer(fftSize * 4)
        re = FloatArray(fftSize)
        im = FloatArray(fftSize)
        val bins = fftSize / 2
        power = FloatArray(bins)
        magDb = FloatArray(bins)
        val binHz = sampleRate.toFloat() / fftSize
        bandProcessor = ToneBandProcessor(binHz, bins)
        mapper = LogSpectrumMapper(
            binHz = binHz,
            binCount = bins,
            points = displayPoints,
            minHz = AudioAnalysis.DISPLAY_MIN_HZ,
            maxHz = AudioAnalysis.DISPLAY_MAX_HZ
        )
        val avg = settings.spectrumAveraging.coerceIn(1, 8)
        averagePool = Array(avg) { FloatArray(displayPoints) { Db.FLOOR } }
        averageIndex = 0
        averageFilled = 0
        levelMeter = LevelMeterProcessor(sampleRate)
        smoother.factor = settings.spectrumSmoothing
        smoother.resize(displayPoints)
        gainLinear = Db.toAmplitude(settings.inputGainDb)
        samplesSinceLastFrame = 0
    }

    /**
     * Recebe um bloco de PCM 16 bits mono.
     * @return uma analise nova quando ha amostras suficientes, ou null.
     */
    fun push(pcm: ShortArray, count: Int = pcm.size): AudioAnalysis? {
        if (scratch.size < count) scratch = FloatArray(count)
        for (i in 0 until count) {
            scratch[i] = (pcm[i] / 32768f) * gainLinear
        }
        return pushFloats(scratch, count)
    }

    fun pushFloats(samples: FloatArray, count: Int = samples.size): AudioAnalysis? {
        ring.write(samples, count)
        samplesSinceLastFrame += count
        if (samplesSinceLastFrame < hopSize) return null
        samplesSinceLastFrame = 0
        return computeFrame()
    }

    private fun computeFrame(): AudioAnalysis? {
        if (!ring.readLatest(re)) return null

        val level = levelMeter.process(re, fftSize)

        // Janela + parte imaginaria zerada
        for (i in 0 until fftSize) {
            re[i] = re[i] * window[i]
            im[i] = 0f
        }
        fft.transform(re, im)

        val bins = fftSize / 2
        val norm = 2f / (fftSize * Fft.HANN_COHERENT_GAIN)
        var peakBin = 1
        var peakPower = 0f
        for (i in 0 until bins) {
            val r = re[i]
            val m = im[i]
            val p = (r * r + m * m) * norm * norm
            power[i] = p
            magDb[i] = Db.fromPower(p)
            if (i > 0 && p > peakPower) {
                peakPower = p
                peakBin = i
            }
        }

        val binHz = sampleRate.toFloat() / fftSize
        val dominant = interpolatePeak(peakBin) * binHz

        val bands = bandProcessor.process(power)
        val diagnostics = floatArrayOf(
            bandProcessor.shareOf(power, 150f, 300f),
            bandProcessor.shareOf(power, 3000f, 5000f),
            bandProcessor.shareOf(power, 6000f, 10000f)
        )

        val mapped = mapper.map(magDb)
        val averaged = averageFrames(mapped)
        val smoothed = smoother.smooth(averaged).copyOf()

        return AudioAnalysis(
            timestampMs = System.currentTimeMillis(),
            sampleRate = sampleRate,
            peakDb = level.peakDb,
            rmsDb = level.rmsDb,
            noiseFloorDb = level.noiseFloorDb,
            clipping = level.clipping,
            crestFactorDb = level.crestFactorDb,
            dynamicRangeDb = level.dynamicRangeDb,
            dominantFrequency = dominant,
            bands = bands,
            spectrum = smoothed,
            binHz = binHz,
            diagnostics = diagnostics
        )
    }

    /** Interpolacao parabolica para achar a frequencia dominante entre bins. */
    private fun interpolatePeak(bin: Int): Float {
        if (bin <= 0 || bin >= power.size - 1) return bin.toFloat()
        val a = magDb[bin - 1]
        val b = magDb[bin]
        val c = magDb[bin + 1]
        val denom = (a - 2f * b + c)
        if (denom == 0f) return bin.toFloat()
        val delta = 0.5f * (a - c) / denom
        return bin + delta.coerceIn(-0.5f, 0.5f)
    }

    private fun averageFrames(frame: FloatArray): FloatArray {
        if (averagePool.size == 1) return frame
        System.arraycopy(frame, 0, averagePool[averageIndex], 0, frame.size)
        averageIndex = (averageIndex + 1) % averagePool.size
        if (averageFilled < averagePool.size) averageFilled++
        val out = FloatArray(frame.size)
        for (i in frame.indices) {
            var acc = 0f
            for (f in 0 until averageFilled) acc += averagePool[f][i]
            out[i] = acc / averageFilled
        }
        return out
    }
}
