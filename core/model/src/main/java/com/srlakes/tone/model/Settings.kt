package com.srlakes.tone.model

/**
 * Ajustes do analisador. Todos afetam apenas a MEDICAO, nunca o som
 * que vai para o amplificador.
 */
data class AnalyzerSettings(
    val source: AudioSourceKind = AudioSourceKind.AMP_MIC,
    /** Ganho aplicado somente na medicao, em dB. */
    val inputGainDb: Float = 0f,
    /** Sensibilidade do detector de sinal: quantos dB acima do ruido conta como nota. */
    val sensitivityDb: Float = 8f,
    /** Duracao da captura de cada lado do A/B, em segundos. */
    val abCaptureSeconds: Int = 8,
    /** Duracao da captura de BASE e de SOLO, em segundos. */
    val baseSoloCaptureSeconds: Int = 8,
    /** 0 = sem suavizacao, 0.95 = muito lento. */
    val spectrumSmoothing: Float = 0.72f,
    /** Quantos frames de FFT sao mediados antes de desenhar. */
    val spectrumAveraging: Int = 2,
    /** Quadros por segundo do grafico. */
    val refreshRateFps: Int = 24,
    val fftSize: Int = 2048,
    val preferUnprocessedMic: Boolean = true,
    val preferredMicId: Int? = null,
    val preferredUsbDeviceId: Int? = null
) {
    companion object {
        val DEFAULT = AnalyzerSettings()
        val FFT_SIZES = listOf(1024, 2048, 4096)
    }
}

data class AppSettings(
    val analyzer: AnalyzerSettings = AnalyzerSettings.DEFAULT,
    val sync: StageSyncSettings = StageSyncSettings.DEFAULT,
    val keepScreenOn: Boolean = true,
    val activeSetlistId: Long? = null,
    val useMockDevices: Boolean = true
) {
    companion object {
        val DEFAULT = AppSettings()
    }
}
