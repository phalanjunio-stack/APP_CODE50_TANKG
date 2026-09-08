package com.srlakes.tone.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.analysis.LoudnessComparator
import com.srlakes.tone.audio.AudioInputManager
import com.srlakes.tone.audio.CaptureResult
import com.srlakes.tone.audio.WavWriter
import com.srlakes.tone.data.AnalysisRepository
import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.model.AnalysisSession
import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AudioSourceKind
import com.srlakes.tone.model.BaseSoloResult
import com.srlakes.tone.model.SnapshotDelta
import com.srlakes.tone.model.SnapshotRole
import com.srlakes.tone.model.ToneSnapshot
import com.srlakes.tone.model.compare
import com.srlakes.tone.ui.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Estado da tela de analise.
 *
 * O analisador so MEDE e SUGERE. Nenhuma linha desta classe fala com
 * DeviceManager, e isso e proposital: nao existe caminho de codigo em
 * que uma medicao altere um timbre.
 */
class AnalyzerViewModel(
    private val audio: AudioInputManager,
    private val settingsRepository: SettingsRepository,
    private val analysisRepository: AnalysisRepository,
    private val captureDir: File
) : ViewModel() {

    val analysis = audio.analysis
    val insights = audio.insights
    val status = audio.status
    val captureProgress = audio.capture

    val settings: StateFlow<AnalyzerSettings> = settingsRepository.settings
        .map { it.analyzer }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyzerSettings.DEFAULT)

    private val snapshotAState = MutableStateFlow<ToneSnapshot?>(null)
    val snapshotA: StateFlow<ToneSnapshot?> = snapshotAState.asStateFlow()

    private val snapshotBState = MutableStateFlow<ToneSnapshot?>(null)
    val snapshotB: StateFlow<ToneSnapshot?> = snapshotBState.asStateFlow()

    private val baseState = MutableStateFlow<ToneSnapshot?>(null)
    val baseSnapshot: StateFlow<ToneSnapshot?> = baseState.asStateFlow()

    private val soloState = MutableStateFlow<ToneSnapshot?>(null)
    val soloSnapshot: StateFlow<ToneSnapshot?> = soloState.asStateFlow()

    private val messageState = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = messageState.asStateFlow()

    private val savedPcm = HashMap<String, String>()

    /**
     * Derivados como StateFlow, e nao como getters: um getter comum nao
     * dispara recomposicao no Compose quando o valor muda.
     */
    val abDelta: StateFlow<SnapshotDelta?> =
        combine(snapshotAState, snapshotBState) { a, b ->
            if (a == null || b == null) null else compare(a, b)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val abWarning: StateFlow<String?> =
        combine(snapshotAState, snapshotBState) { a, b ->
            if (a == null || b == null) null else LoudnessComparator.reliabilityWarning(a, b)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val baseSoloResult: StateFlow<BaseSoloResult?> =
        combine(baseState, soloState) { base, solo ->
            if (base == null || solo == null) null
            else LoudnessComparator.compareBaseSolo(base, solo)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val baseSoloWarning: StateFlow<String?> =
        combine(baseState, soloState) { base, solo ->
            if (base == null || solo == null) null
            else LoudnessComparator.reliabilityWarning(base, solo)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            settings.collect { audio.updateSettings(it, viewModelScope) }
        }
    }

    fun start() = audio.start(viewModelScope)

    fun stop() = audio.stop()

    fun setSource(kind: AudioSourceKind) {
        viewModelScope.launch {
            settingsRepository.updateAnalyzer { it.copy(source = kind) }
        }
    }

    fun captureA() = capture("A") { snapshotAState.value = it }

    fun captureB() = capture("B") { snapshotBState.value = it }

    fun captureBase() = capture("BASE", useBaseSoloDuration = true) { baseState.value = it }

    fun captureSolo() = capture("SOLO", useBaseSoloDuration = true) { soloState.value = it }

    fun clearAb() {
        snapshotAState.value = null
        snapshotBState.value = null
    }

    fun clearBaseSolo() {
        baseState.value = null
        soloState.value = null
    }

    fun clearMessage() {
        messageState.value = null
    }

    private fun capture(
        label: String,
        useBaseSoloDuration: Boolean = false,
        onResult: (ToneSnapshot) -> Unit
    ) {
        viewModelScope.launch {
            val seconds = if (useBaseSoloDuration) {
                settings.value.baseSoloCaptureSeconds
            } else {
                settings.value.abCaptureSeconds
            }
            val result = audio.captureSnapshot(label, seconds)
            result
                .onSuccess { captureResult ->
                    onResult(captureResult.snapshot)
                    persistPcm(label, captureResult)
                    messageState.value = "Medição " + label + " concluída."
                }
                .onFailure { messageState.value = it.message }
        }
    }

    /** Guarda o audio cru para a medicao poder ser reouvida depois. */
    private fun persistPcm(label: String, result: CaptureResult) {
        val pcm = result.pcm ?: return
        runCatching {
            val file = File(captureDir, "cap_" + label + "_" + System.currentTimeMillis() + ".wav")
            WavWriter.write(file, pcm, result.sampleRate)
            savedPcm[label] = file.absolutePath
        }
    }

    /** Salva o par BASE/SOLO no historico, com a musica e o contexto. */
    fun saveBaseSoloSession(songTitle: String, context: String, note: String) {
        val base = baseState.value
        val solo = soloState.value
        if (base == null || solo == null) {
            messageState.value = "Grave BASE e SOLO antes de salvar."
            return
        }
        viewModelScope.launch {
            val sessionId = analysisRepository.createSession(
                AnalysisSession(
                    songTitle = songTitle.ifBlank { "Sem título" },
                    context = context.ifBlank { "Ensaio" },
                    dateLabel = Format.today(),
                    createdAtMs = System.currentTimeMillis(),
                    note = note
                )
            )
            analysisRepository.addSnapshot(sessionId, SnapshotRole.BASE, base, savedPcm["BASE"])
            analysisRepository.addSnapshot(sessionId, SnapshotRole.SOLO, solo, savedPcm["SOLO"])
            messageState.value = "Sessão salva no histórico."
        }
    }

    fun saveAbSession(songTitle: String, context: String, note: String) {
        val a = snapshotAState.value
        val b = snapshotBState.value
        if (a == null || b == null) {
            messageState.value = "Grave A e B antes de salvar."
            return
        }
        viewModelScope.launch {
            val sessionId = analysisRepository.createSession(
                AnalysisSession(
                    songTitle = songTitle.ifBlank { "Sem título" },
                    context = context.ifBlank { "Passagem de som" },
                    dateLabel = Format.today(),
                    createdAtMs = System.currentTimeMillis(),
                    note = note
                )
            )
            analysisRepository.addSnapshot(sessionId, SnapshotRole.A, a, savedPcm["A"])
            analysisRepository.addSnapshot(sessionId, SnapshotRole.B, b, savedPcm["B"])
            messageState.value = "Comparação salva no histórico."
        }
    }

    override fun onCleared() {
        audio.stop()
        super.onCleared()
    }
}
