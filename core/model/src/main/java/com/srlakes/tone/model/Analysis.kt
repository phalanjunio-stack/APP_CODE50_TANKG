package com.srlakes.tone.model

/** Faixas de frequencia usadas em todo o app. */
enum class ToneBand(val label: String, val lowHz: Float, val highHz: Float) {
    LOW("LOW", 20f, 250f),
    LOW_MID("LOW MID", 250f, 800f),
    MID("MID", 800f, 2000f),
    HIGH_MID("HIGH MID", 2000f, 6000f),
    HIGH("HIGH", 6000f, 20000f);

    val rangeLabel: String
        get() {
            fun fmt(hz: Float) = if (hz >= 1000f) "${(hz / 1000f).toInt()}k" else "${hz.toInt()}"
            return "${fmt(lowHz)} - ${fmt(highHz)} Hz"
        }

    companion object {
        val ordered = listOf(LOW, LOW_MID, MID, HIGH_MID, HIGH)
    }
}

/** Energia por banda, em dBFS, e a proporcao relativa (0..1) de cada uma. */
class BandLevels(
    val energyDb: FloatArray,
    val share: FloatArray
) {
    fun db(band: ToneBand): Float = energyDb.getOrElse(band.ordinal) { -120f }
    fun share(band: ToneBand): Float = share.getOrElse(band.ordinal) { 0f }

    fun state(band: ToneBand): LevelState {
        val s = share(band)
        val target = TARGET_SHARE[band.ordinal]
        return when {
            s > target * 1.55f -> LevelState.HIGH
            s < target * 0.55f -> LevelState.LOW
            else -> LevelState.GOOD
        }
    }

    companion object {
        /** Distribuicao de referencia para guitarra eletrica com amp microfonado. */
        val TARGET_SHARE = floatArrayOf(0.20f, 0.28f, 0.26f, 0.18f, 0.08f)

        val EMPTY = BandLevels(FloatArray(5) { -120f }, FloatArray(5))
    }
}

/**
 * Resultado de um bloco de analise. Imutavel: um objeto novo por frame,
 * publicado por StateFlow para a UI.
 *
 * IMPORTANTE: todos os valores sao dBFS (relativo ao fundo de escala digital),
 * NAO dB SPL. So diferencas entre medicoes tem significado.
 */
class AudioAnalysis(
    val timestampMs: Long,
    val sampleRate: Int,
    val peakDb: Float,
    val rmsDb: Float,
    val noiseFloorDb: Float,
    val clipping: Boolean,
    val crestFactorDb: Float,
    val dynamicRangeDb: Float,
    val dominantFrequency: Float,
    val bands: BandLevels,
    /** Magnitude em dBFS por bin da FFT, ja suavizada. */
    val spectrum: FloatArray,
    /** Largura de cada bin em Hz na FFT (informativo). */
    val binHz: Float,
    /**
     * Proporcoes de faixas estreitas usadas pelas regras de Tone Insights.
     * Indices: DIAG_MUD (150-300 Hz), DIAG_HARSH (3-5 kHz), DIAG_FIZZ (6-10 kHz).
     */
    val diagnostics: FloatArray = FloatArray(3)
) {
    val lowEnergy: Float get() = bands.db(ToneBand.LOW)
    val lowMidEnergy: Float get() = bands.db(ToneBand.LOW_MID)
    val midEnergy: Float get() = bands.db(ToneBand.MID)
    val highMidEnergy: Float get() = bands.db(ToneBand.HIGH_MID)
    val highEnergy: Float get() = bands.db(ToneBand.HIGH)

    val hasSignal: Boolean get() = rmsDb > noiseFloorDb + 8f

    companion object {
        const val DIAG_MUD = 0
        const val DIAG_HARSH = 1
        const val DIAG_FIZZ = 2

        /** O espectro exibido cobre sempre esta faixa, em escala logaritmica. */
        const val DISPLAY_MIN_HZ = 20f
        const val DISPLAY_MAX_HZ = 20000f

        fun silent(sampleRate: Int = 48000, bins: Int = 192): AudioAnalysis = AudioAnalysis(
            timestampMs = 0L,
            sampleRate = sampleRate,
            peakDb = -120f,
            rmsDb = -120f,
            noiseFloorDb = -120f,
            clipping = false,
            crestFactorDb = 0f,
            dynamicRangeDb = 0f,
            dominantFrequency = 0f,
            bands = BandLevels.EMPTY,
            spectrum = FloatArray(bins) { -120f },
            binHz = sampleRate / 2048f,
            diagnostics = FloatArray(3)
        )
    }
}

enum class InsightSeverity { OK, INFO, WARN, ALERT }

/**
 * Sugestao. O app NUNCA aplica nada sozinho - so mostra.
 */
data class ToneInsight(
    val id: String,
    val title: String,
    val message: String,
    val severity: InsightSeverity,
    val band: ToneBand? = null,
    /** true = sobra energia (seta pra baixo), false = falta (seta pra cima), null = neutro */
    val excess: Boolean? = null
)

/**
 * Medicao congelada de alguns segundos de execucao. Base de A/B e BASE x SOLO.
 */
data class ToneSnapshot(
    val label: String,
    val capturedAtMs: Long,
    val durationMs: Long,
    val rmsDb: Float,
    val peakDb: Float,
    val noiseFloorDb: Float,
    val crestFactorDb: Float,
    val bandDb: List<Float>,
    val bandShare: List<Float>,
    /** Espectro medio em dBFS, reamostrado para um numero fixo de pontos. */
    val averageSpectrum: List<Float>,
    val clipped: Boolean
) {
    fun band(b: ToneBand): Float = bandDb.getOrElse(b.ordinal) { -120f }
}

data class SnapshotDelta(
    val rmsDb: Float,
    val peakDb: Float,
    val bandDb: List<Float>
)

fun compare(a: ToneSnapshot, b: ToneSnapshot): SnapshotDelta = SnapshotDelta(
    rmsDb = b.rmsDb - a.rmsDb,
    peakDb = b.peakDb - a.peakDb,
    bandDb = ToneBand.ordered.map { b.band(it) - a.band(it) }
)

enum class LoudnessVerdict(val label: String) {
    SOLO_TOO_LOW("SOLO MUITO BAIXO"),
    BALANCED("SOLO EQUILIBRADO"),
    SOLO_TOO_LOUD("SOLO MUITO ALTO")
}

data class BaseSoloResult(
    val base: ToneSnapshot,
    val solo: ToneSnapshot,
    val deltaRmsDb: Float,
    val deltaPeakDb: Float,
    val verdict: LoudnessVerdict
)

/** Sessao salva no historico. */
data class AnalysisSession(
    val id: Long = 0L,
    val songTitle: String,
    val context: String,
    val dateLabel: String,
    val createdAtMs: Long,
    val note: String = ""
)

enum class SnapshotRole { A, B, BASE, SOLO, SINGLE }

data class AnalysisSnapshotRecord(
    val id: Long = 0L,
    val sessionId: Long,
    val role: SnapshotRole,
    val snapshot: ToneSnapshot
)
