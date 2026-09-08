package com.srlakes.tone.feature.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.data.PresetRepository
import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.data.SetlistRepository
import com.srlakes.tone.data.SongRepository
import com.srlakes.tone.device.api.DeviceManager
import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.Macro
import com.srlakes.tone.model.MacroSet
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.Scene
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.model.Song
import com.srlakes.tone.model.SongDetail
import com.srlakes.tone.model.SongSection
import com.srlakes.tone.model.ControlCommand
import com.srlakes.tone.model.FollowState
import com.srlakes.tone.model.StudioClip
import com.srlakes.tone.model.normalizeTitle
import com.srlakes.tone.protocol.MacroResolver
import com.srlakes.tone.sync.FollowTarget
import com.srlakes.tone.sync.StageSyncCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tela principal de palco.
 *
 * Regra que atravessa toda a classe: NADA muda de cena sozinho.
 * O analisador nao interfere aqui. Quem escolhe e o musico, tocando
 * num botao de cena ou numa secao da musica.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceViewModel(
    private val songRepository: SongRepository,
    private val presetRepository: PresetRepository,
    private val setlistRepository: SetlistRepository,
    private val settingsRepository: SettingsRepository,
    private val deviceManager: DeviceManager,
    private val stageSync: StageSyncCoordinator
) : ViewModel() {

    private val allSongs = songRepository.observeSongs()

    /** A lista que a tela navega: a setlist ativa, ou todas as musicas. */
    val songs: StateFlow<List<Song>> = settingsRepository.settings
        .map { it.activeSetlistId }
        .flatMapLatest { setlistId ->
            if (setlistId == null) allSongs
            else setlistRepository.observeDetail(setlistId).flatMapLatest { detail ->
                if (detail == null || detail.songs.isEmpty()) allSongs else flowOf(detail.songs)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val setlistName: StateFlow<String?> = settingsRepository.settings
        .map { it.activeSetlistId }
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else setlistRepository.observeDetail(id).map { it?.setlist?.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val selectedSongId = MutableStateFlow<Long?>(null)

    val currentSong: StateFlow<Song?> = combine(songs, selectedSongId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nextSong: StateFlow<Song?> = combine(songs, currentSong) { list, current ->
        val index = list.indexOfFirst { it.id == current?.id }
        if (index >= 0 && index + 1 < list.size) list[index + 1] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val detail: StateFlow<SongDetail?> = currentSong
        .flatMapLatest { song ->
            if (song == null) flowOf(null) else songRepository.observeDetail(song.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val presets: StateFlow<List<Preset>> = presetRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeSceneIdState = MutableStateFlow<Long?>(null)
    val activeSceneId: StateFlow<Long?> = activeSceneIdState.asStateFlow()

    private val activeSectionIdState = MutableStateFlow<Long?>(null)
    val activeSectionId: StateFlow<Long?> = activeSectionIdState.asStateFlow()

    private val macrosState = MutableStateFlow(MacroSet.DEFAULT)
    val macros: StateFlow<MacroSet> = macrosState.asStateFlow()

    private val ampState = MutableStateFlow(AmpParams.DEFAULT)
    val amp: StateFlow<AmpParams> = ampState.asStateFlow()

    private val effectsState = MutableStateFlow(EffectState.DEFAULT)
    val effects: StateFlow<EffectState> = effectsState.asStateFlow()

    private val messageState = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = messageState.asStateFlow()

    // ---------------------------------------------------------------
    // Piloto automatico do SR Lakes Studio.
    //
    // Trocou de musica no notebook -> o preset inteiro da cena BASE vai
    // junto: macros, EQ e os efeitos (reverb, delay, boost, gate).
    //
    // Mas com uma valvula de escape: se o guitarrista mexer em qualquer
    // coisa com a mao, o app SOLTA o piloto automatico ate a proxima
    // troca de musica. Ninguem perde o timbre no meio de um solo porque
    // alguem encostou no notebook.
    // ---------------------------------------------------------------
    private val manualOverrideState = MutableStateFlow(false)

    /** Ultimo songId do Studio que ja foi aplicado, para nao reaplicar. */
    private var appliedStudioSongId: Int? = null

    /** Ultimo bloco da timeline aplicado. O app so age quando ele muda. */
    private var appliedClipKey: String? = null

    val syncStatus = stageSync.status
    val stageState = stageSync.stage

    val followState: StateFlow<FollowState> = combine(
        stageSync.settings,
        stageSync.status,
        stageSync.followTarget,
        manualOverrideState
    ) { settings, status, target, manual ->
        when {
            !settings.enabled || !settings.followSong -> FollowState.DISABLED
            !status.connected -> FollowState.WAITING
            target == null -> FollowState.WAITING
            target.localSong == null -> FollowState.UNMAPPED
            manual -> FollowState.MANUAL
            else -> FollowState.FOLLOWING
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FollowState.DISABLED)

    /**
      * O que o Studio esta tocando agora, ja com o bloco da timeline:
      * "So Por Meu Prazer - REFRAO". E a linha que diz, de relance, se o
      * app esta mesmo acompanhando a musica.
      */
    val studioNowPlaying: StateFlow<String?> = stageSync.followTarget
        .map { target ->
            val song = target?.localSong?.title ?: return@map null
            val clip = target.activeClip?.label?.takeIf { it.isNotBlank() }
            if (clip == null) song else song + "  -  " + clip.uppercase()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val deviceInfos: StateFlow<List<DeviceInfo>> = deviceManager.infos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeScene: StateFlow<Scene?> = combine(detail, activeSceneId) { d, id ->
        d?.scenes?.firstOrNull { it.id == id } ?: d?.scenes?.firstOrNull { it.sceneType == SceneType.BASE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Um coletor so para tudo que vem do Studio: assim a ordem
        // (musica primeiro, bloco depois) e deterministica.
        viewModelScope.launch {
            stageSync.followTarget.collect { target -> onStudioUpdate(target) }
        }

        // Ao trocar de musica, posiciona na primeira secao e na cena dela.
        // Fica quieto quando o Studio esta conduzindo a timeline: ali quem
        // posiciona e o bloco ativo, nao o primeiro da lista.
        viewModelScope.launch {
            detail.collect { d ->
                if (d == null || studioIsDrivingTimeline()) return@collect
                if (d.sections.none { it.id == activeSectionIdState.value }) {
                    val first = d.sections.firstOrNull()
                    if (first != null) {
                        activeSectionIdState.value = first.id
                        d.sceneById(first.sceneId)?.let { loadScene(it) }
                    } else {
                        val base = d.scenes.firstOrNull { it.sceneType == SceneType.BASE }
                            ?: d.scenes.firstOrNull()
                        if (base != null) loadScene(base)
                    }
                }
            }
        }
    }

    /**
     * Tudo que chega do Studio passa por aqui, nesta ordem: primeiro a
     * troca de musica, depois o bloco da timeline.
     */
    private suspend fun onStudioUpdate(target: FollowTarget?) {
        if (target == null) return
        val settings = stageSync.settings.value
        if (!settings.enabled) return

        if (settings.followSong && target.studioSongId != appliedStudioSongId) {
            appliedStudioSongId = target.studioSongId
            appliedClipKey = null
            // Musica nova: o piloto automatico volta a valer.
            manualOverrideState.value = false

            val song = target.localSong
            if (song == null) {
                messageState.value = "O Studio trocou para uma musica que ainda nao " +
                    "esta associada. Associe em Dispositivos, SR Lakes Studio."
                return
            }
            selectedSongId.value = song.id
            activeSectionIdState.value = null
            activeSceneIdState.value = null
        }

        if (!settings.followSections || manualOverrideState.value) return
        val key = target.clipKey ?: return
        if (key == appliedClipKey) return
        val clip = target.activeClip ?: return
        val song = target.localSong ?: return
        appliedClipKey = key
        applyStudioClip(clip, song)
    }

    /**
     * Aplica um bloco da timeline do Studio.
     *
     * A ordem de resolucao vai do mais explicito ao mais generico:
     *   1. o nome escrito no bloco da Controladora bate com uma CENA;
     *   2. bate com um tipo de cena (CLEAN / BASE / SOLO);
     *   3. o tipo do bloco bate com uma SECAO desta musica;
     *   4. o nome bate com um PRESET solto.
     *
     * Nao achou nada? Nao mexe no timbre. Errar a cena no palco e pior
     * do que nao mudar.
     */
    private suspend fun applyStudioClip(clip: StudioClip, song: Song) {
        val detail = songRepository.detail(song.id) ?: return
        val control = clip.control
        val wanted = control?.scene?.takeIf { it.isNotBlank() }?.let { normalizeTitle(it) }

        var scene: Scene? = null
        if (wanted != null) {
            scene = detail.scenes.firstOrNull { normalizeTitle(it.name) == wanted }
                ?: SceneType.entries
                    .firstOrNull { normalizeTitle(it.label) == wanted }
                    ?.let { type -> detail.scenes.firstOrNull { it.sceneType == type } }
        }

        val localSection = clip.sectionType?.let { type ->
            detail.sections.firstOrNull { it.type == type }
        }
        if (scene == null) scene = localSection?.let { detail.sceneById(it.sceneId) }

        if (scene == null && wanted != null) {
            val preset = presets.value.firstOrNull { normalizeTitle(it.name) == wanted }
            if (preset != null) {
                if (localSection != null) activeSectionIdState.value = localSection.id
                applyPresetWithOverrides(preset, control)
                return
            }
        }

        val resolved = scene
        if (resolved == null) {
            messageState.value = "O bloco " + clip.label + " do Studio nao corresponde a " +
                "nenhuma cena desta musica. O timbre nao foi alterado."
            return
        }

        if (localSection != null) activeSectionIdState.value = localSection.id
        loadScene(resolved, control)
    }

    /** Verdadeiro quando o Studio esta conduzindo o timbre pela timeline. */
    private fun studioIsDrivingTimeline(): Boolean {
        val settings = stageSync.settings.value
        return settings.enabled &&
            settings.followSections &&
            !manualOverrideState.value &&
            stageSync.followTarget.value?.activeClip != null
    }

    fun selectSong(song: Song) {
        manualOverrideState.value = true
        selectedSongId.value = song.id
        activeSectionIdState.value = null
        activeSceneIdState.value = null
    }

    fun goToNextSong() {
        val list = songs.value
        val index = list.indexOfFirst { it.id == currentSong.value?.id }
        if (index >= 0 && index + 1 < list.size) selectSong(list[index + 1])
    }

    fun goToPreviousSong() {
        val list = songs.value
        val index = list.indexOfFirst { it.id == currentSong.value?.id }
        if (index > 0) selectSong(list[index - 1])
    }

    /** Toque numa secao: carrega a cena daquela secao. Acao do musico. */
    fun selectSection(section: SongSection) {
        manualOverrideState.value = true
        activeSectionIdState.value = section.id
        val scene = detail.value?.sceneById(section.sceneId)
        if (scene != null) selectScene(scene)
    }

    /** Toque num dos tres botoes gigantes. */
    fun selectSceneType(type: SceneType) {
        manualOverrideState.value = true
        val scene = detail.value?.scenes?.firstOrNull { it.sceneType == type } ?: return
        selectScene(scene)
    }

    fun selectScene(scene: Scene) {
        manualOverrideState.value = true
        viewModelScope.launch { loadScene(scene) }
    }

    /**
     * Carrega uma cena de fato: preset inteiro para os dois aparelhos -
     * macros, equalizacao E efeitos.
     *
     * Nao marca acao manual: e usado tanto pelo toque do musico quanto
     * pelo piloto automatico do Studio.
     */
    private suspend fun loadScene(scene: Scene, overrides: ControlCommand? = null) {
        activeSceneIdState.value = scene.id
        val preset = scene.presetId?.let { presetRepository.byId(it) } ?: return
        applyPresetWithOverrides(preset, overrides)
    }

    /**
     * Manda o preset inteiro para os dois aparelhos - macros, equalizacao
     * E efeitos - com os liga/desliga do bloco da Controladora por cima.
     */
    private suspend fun applyPresetWithOverrides(preset: Preset, overrides: ControlCommand?) {
        var effects = preset.effects
        overrides?.effects?.forEach { (slot, on) -> effects = effects.set(slot, on) }
        val macros = overrides?.volume?.let { preset.macros.with(Macro.VOLUME, it) } ?: preset.macros
        val amp = MacroResolver.resolve(macros)

        macrosState.value = macros
        ampState.value = amp
        effectsState.value = effects

        val outcome = deviceManager.applyPreset(
            preset.copy(macros = macros, amp = amp, effects = effects)
        )
        reportOutcome(outcome.failures, outcome.nothingDelivered)
    }

    fun setMacro(macro: Macro, value: Float) {
        manualOverrideState.value = true
        val next = macrosState.value.with(macro, value)
        macrosState.value = next
        ampState.value = MacroResolver.resolve(next)
        viewModelScope.launch {
            val outcome = deviceManager.applyMacros(next)
            reportOutcome(outcome.failures, outcome.nothingDelivered)
        }
    }

    /** Tela avancada: parametro individual, sem passar pelos macros. */
    fun setAmpParams(params: AmpParams) {
        manualOverrideState.value = true
        ampState.value = params
        macrosState.value = MacroResolver.approximate(params)
        viewModelScope.launch {
            val outcome = deviceManager.applyAmpParams(params)
            reportOutcome(outcome.failures, outcome.nothingDelivered)
        }
    }

    fun toggleEffect(slot: EffectSlot) {
        manualOverrideState.value = true
        val next = effectsState.value.toggle(slot)
        effectsState.value = next
        viewModelScope.launch {
            val outcome = deviceManager.setEffect(slot, next.isOn(slot))
            reportOutcome(outcome.failures, outcome.nothingDelivered)
        }
    }

    /**
     * PANIC: volta para a cena BASE da musica atual.
     * Um toque longo, para nao disparar sem querer.
     */
    /** Devolve o comando ao Studio sem esperar a proxima musica. */
    fun resumeFollowing() {
        manualOverrideState.value = false
        val target = stageSync.followTarget.value ?: return
        val song = target.localSong ?: return
        appliedStudioSongId = target.studioSongId
        selectedSongId.value = song.id
        activeSectionIdState.value = null
        activeSceneIdState.value = null
    }

    fun setFollowEnabled(enabled: Boolean) {
        stageSync.setEnabled(enabled)
        if (enabled) manualOverrideState.value = false
    }

    fun panic() {
        manualOverrideState.value = true
        val base = detail.value?.scenes?.firstOrNull { it.sceneType == SceneType.BASE }
            ?: detail.value?.scenes?.firstOrNull()
        if (base != null) {
            selectScene(base)
            messageState.value = "Cena segura carregada: " + base.name
        }
    }

    /** Salva os valores atuais por cima do preset da cena ativa. */
    fun storeCurrentIntoScenePreset() {
        viewModelScope.launch {
            val scene = activeScene.value
            val presetId = scene?.presetId
            if (scene == null || presetId == null) {
                messageState.value = "A cena atual nao tem preset associado."
                return@launch
            }
            val preset = presetRepository.byId(presetId) ?: return@launch
            presetRepository.save(
                preset.copy(
                    macros = macrosState.value,
                    amp = ampState.value,
                    effects = effectsState.value
                )
            )
            messageState.value = "Preset " + preset.name + " atualizado."
        }
    }

    fun clearMessage() {
        messageState.value = null
    }

    suspend fun currentSetlistFirstSong(): Song? = songs.first().firstOrNull()

    private fun reportOutcome(failures: List<String>, nothingDelivered: Boolean) {
        messageState.value = when {
            failures.isNotEmpty() -> failures.first()
            nothingDelivered -> "Nenhum aparelho conectado. Os valores ficaram salvos no aplicativo."
            else -> null
        }
    }
}
