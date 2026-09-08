package com.srlakes.tone.audio

import android.content.Context
import com.srlakes.tone.analysis.AudioAnalyzer
import com.srlakes.tone.analysis.RuleBasedToneAdvisor
import com.srlakes.tone.analysis.SnapshotRecorder
import com.srlakes.tone.analysis.ToneAdvisor
import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AudioAnalysis
import com.srlakes.tone.model.AudioSourceKind
import com.srlakes.tone.model.ToneInsight
import com.srlakes.tone.model.ToneSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Dono do ciclo de vida da captura de audio.
 *
 * Uma unica corrotina, em Dispatchers.IO, faz leitura + FFT. A UI so
 * observa StateFlows. Nada de FFT na thread principal, e a emissao para
 * a tela e limitada ao refresh configurado, para o Compose nao recompor
 * 47 vezes por segundo sem necessidade.
 *
 * Este objeto NAO processa o audio que vai para o amplificador.
 * Ele so escuta. Zero latencia adicionada na guitarra.
 */
class AudioInputManager(
    private val context: Context,
    private val advisor: ToneAdvisor = RuleBasedToneAdvisor()
) {

    private val analysisState = MutableStateFlow(AudioAnalysis.silent())
    val analysis: StateFlow<AudioAnalysis> = analysisState.asStateFlow()

    private val insightsState = MutableStateFlow<List<ToneInsight>>(emptyList())
    val insights: StateFlow<List<ToneInsight>> = insightsState.asStateFlow()

    private val statusState = MutableStateFlow(AudioStatus())
    val status: StateFlow<AudioStatus> = statusState.asStateFlow()

    private val captureState = MutableStateFlow<CaptureProgress?>(null)
    val capture: StateFlow<CaptureProgress?> = captureState.asStateFlow()

    private var settings: AnalyzerSettings = AnalyzerSettings.DEFAULT
    private var job: Job? = null
    private var analyzer = AudioAnalyzer(settings)

    @Volatile
    private var activeRecorder: SnapshotRecorder? = null

    @Volatile
    private var pcmSink: ShortArrayBuilder? = null

    fun updateSettings(newSettings: AnalyzerSettings, scope: CoroutineScope) {
        val sourceChanged = newSettings.source != settings.source ||
            newSettings.preferUnprocessedMic != settings.preferUnprocessedMic ||
            newSettings.preferredMicId != settings.preferredMicId ||
            newSettings.preferredUsbDeviceId != settings.preferredUsbDeviceId
        settings = newSettings
        analyzer.updateSettings(newSettings)
        if (sourceChanged && job != null) {
            restart(scope)
        }
    }

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) { runLoop() }
    }

    fun stop() {
        job?.cancel()
        job = null
        statusState.value = statusState.value.copy(running = false)
        analysisState.value = AudioAnalysis.silent()
        insightsState.value = emptyList()
        advisor.reset()
    }

    fun restart(scope: CoroutineScope) {
        stop()
        start(scope)
    }

    private suspend fun runLoop() {
        val source = createSource()
        val opened = source.open()
        if (opened.isFailure) {
            statusState.value = statusState.value.copy(
                running = false,
                error = opened.exceptionOrNull()?.message ?: "Falha ao abrir a entrada de audio.",
                descriptor = null
            )
            return
        }

        val descriptor = source.descriptor
        analyzer = AudioAnalyzer(settings, descriptor.sampleRate)
        advisor.reset()
        statusState.value = AudioStatus(
            running = true,
            descriptor = descriptor,
            error = null
        )

        val buffer = ShortArray(AudioRecordSource.BLOCK_SAMPLES)
        val minEmitIntervalMs = (1000L / settings.refreshRateFps.coerceIn(5, 60))
        var lastEmit = 0L

        try {
            while (currentCoroutineContext().isActive) {
                val read = source.read(buffer)
                if (read <= 0) {
                    if (read < 0) {
                        statusState.value = statusState.value.copy(
                            running = false,
                            error = "A leitura de audio falhou (codigo " + read + ")."
                        )
                        return
                    }
                    continue
                }

                pcmSink?.append(buffer, read)

                val result = analyzer.push(buffer, read) ?: continue
                activeRecorder?.add(result)

                val now = System.currentTimeMillis()
                if (now - lastEmit >= minEmitIntervalMs) {
                    lastEmit = now
                    analysisState.value = result
                    insightsState.value = advisor.evaluate(result)
                }
            }
        } finally {
            source.close()
            statusState.value = statusState.value.copy(running = false)
        }
    }

    private fun createSource(): AudioSource = when (settings.source) {
        AudioSourceKind.AMP_MIC -> MicrophoneAudioSource(
            context = context,
            preferUnprocessed = settings.preferUnprocessedMic,
            preferredDeviceId = settings.preferredMicId
        )
        AudioSourceKind.USB_DIRECT -> UsbAudioSource(
            context = context,
            preferUnprocessed = settings.preferUnprocessedMic,
            preferredDeviceId = settings.preferredUsbDeviceId
        )
    }

    /**
     * Mede por alguns segundos e devolve um retrato do que foi tocado.
     * Base de A/B e de BASE x SOLO.
     *
     * @param keepPcm guarda o audio cru, para a medicao poder ser
     *        reanalisada ou ouvida depois.
     */
    suspend fun captureSnapshot(
        label: String,
        seconds: Int = settings.abCaptureSeconds,
        keepPcm: Boolean = true
    ): Result<CaptureResult> {
        if (job == null) {
            return Result.failure(IllegalStateException("O analisador nao esta rodando."))
        }
        if (activeRecorder != null) {
            return Result.failure(IllegalStateException("Ja existe uma medicao em andamento."))
        }

        val recorder = SnapshotRecorder(label, settings.sensitivityDb)
        val sink = if (keepPcm) ShortArrayBuilder() else null
        activeRecorder = recorder
        pcmSink = sink

        val totalMs = seconds * 1000L
        val startedAt = System.currentTimeMillis()
        try {
            while (true) {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed >= totalMs) break
                captureState.value = CaptureProgress(
                    label = label,
                    elapsedMs = elapsed,
                    totalMs = totalMs
                )
                delay(100L)
            }
        } finally {
            activeRecorder = null
            pcmSink = null
            captureState.value = null
        }

        val snapshot = recorder.build()
        if (!recorder.isUsable()) {
            return Result.failure(
                IllegalStateException(
                    "Nao houve sinal suficiente durante a medicao. Toque com o celular " +
                        "apontado para o amplificador e tente de novo."
                )
            )
        }
        return Result.success(
            CaptureResult(
                snapshot = snapshot,
                pcm = sink?.build(),
                sampleRate = statusState.value.descriptor?.sampleRate ?: 48000
            )
        )
    }

    fun cancelCapture() {
        activeRecorder = null
        pcmSink = null
        captureState.value = null
    }
}

data class AudioStatus(
    val running: Boolean = false,
    val descriptor: SourceDescriptor? = null,
    val error: String? = null
) {
    val sourceKind: AudioSourceKind get() = descriptor?.kind ?: AudioSourceKind.AMP_MIC
    val sampleRate: Int get() = descriptor?.sampleRate ?: 48000

    /**
     * Falso quando o Android esta processando o microfone. A interface
     * mostra um aviso: sem isso o analisador mentiria para o musico.
     */
    val trustworthy: Boolean get() = descriptor?.unprocessed == true
}

data class CaptureProgress(
    val label: String,
    val elapsedMs: Long,
    val totalMs: Long
) {
    val fraction: Float get() = (elapsedMs.toFloat() / totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val remainingSeconds: Int get() = (((totalMs - elapsedMs) / 1000L) + 1).toInt().coerceAtLeast(0)
}

data class CaptureResult(
    val snapshot: ToneSnapshot,
    val pcm: ShortArray?,
    val sampleRate: Int
)

/** Acumulador simples de PCM, sem realocar a cada bloco. */
class ShortArrayBuilder(initialCapacity: Int = 48000 * 10) {
    private var data = ShortArray(initialCapacity)
    private var size = 0

    fun append(src: ShortArray, count: Int) {
        if (size + count > data.size) {
            val grown = ShortArray(maxOf(data.size * 2, size + count))
            System.arraycopy(data, 0, grown, 0, size)
            data = grown
        }
        System.arraycopy(src, 0, data, size, count)
        size += count
    }

    fun build(): ShortArray = data.copyOf(size)
}
