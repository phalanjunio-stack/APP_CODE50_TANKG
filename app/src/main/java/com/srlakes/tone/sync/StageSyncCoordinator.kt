package com.srlakes.tone.sync

import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.data.SongRepository
import com.srlakes.tone.model.Song
import com.srlakes.tone.model.SongLink
import com.srlakes.tone.model.StageState
import com.srlakes.tone.model.StageSyncSettings
import com.srlakes.tone.model.StudioClip
import com.srlakes.tone.model.StudioSong
import com.srlakes.tone.model.SyncState
import com.srlakes.tone.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Liga o SR Lakes Studio ao SR Lakes Tone.
 *
 * Ele resolve três coisas e para por aí:
 *   1. achar o notebook na rede (mDNS, ou o IP que você digitou);
 *   2. manter a conexão de receptor viva;
 *   3. traduzir o `songId` do Studio para uma música daqui.
 *
 * Ele **não** aplica preset nenhum. Quem decide aplicar é o
 * PerformanceViewModel, que é onde vive a regra de segurança de palco.
 * Essa separação é de propósito: assim existe um lugar só onde o timbre
 * muda, e dá para auditar.
 */
class StageSyncCoordinator(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val discovery: StageDiscovery,
    private val client: StageSyncClient = StageSyncClient(),
    private val api: StudioApi = StudioApi()
) {

    val status: StateFlow<SyncStatus> = client.status
    val stage: StateFlow<StageState?> = client.stage

    private val discoveredState = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discovered: StateFlow<List<DiscoveredServer>> = discoveredState.asStateFlow()

    private val studioSongsState = MutableStateFlow<List<StudioSong>>(emptyList())
    val studioSongs: StateFlow<List<StudioSong>> = studioSongsState.asStateFlow()

    private val messageState = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = messageState.asStateFlow()

    val settings: StateFlow<StageSyncSettings> = settingsRepository.settings
        .map { it.sync }
        .stateIn(scope, SharingStarted.Eagerly, StageSyncSettings.DEFAULT)

    /**
     * A música daqui que corresponde ao que o Studio está tocando agora.
     * Null quando não há estado ainda, ou quando ninguém associou a música.
     */
    val followTarget: StateFlow<FollowTarget?> = combine(
        client.stage,
        songRepository.observeSongs(),
        studioSongsState
    ) { state, songs, studio ->
        if (state == null) return@combine null
        val studioSong = studio.firstOrNull { it.id == state.studioSongId }
        FollowTarget(
            studioSongId = state.studioSongId,
            localSong = songs.firstOrNull { it.studioSongId == state.studioSongId },
            partIndex = state.partIndex,
            timelinePos = state.timelinePos,
            // A Controladora tem prioridade sobre a trilha de seções:
            // ela é o que você desenhou de propósito para o timbre.
            control = studioSong?.controlAt(state.timelinePos),
            section = studioSong?.sectionAt(state.timelinePos)
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    /** Casamento entre as duas bibliotecas, para a tela de associação. */
    val links: StateFlow<List<SongLink>> = combine(
        studioSongsState,
        songRepository.observeSongs()
    ) { studio, locals ->
        studio.map { s ->
            SongLink(
                studioSong = s,
                localSong = locals.firstOrNull { it.studioSongId == s.id },
                automatic = false
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Repertorio daqui, para a tela de associacao poder escolher. */
    val localSongs: StateFlow<List<Song>> = songRepository.observeSongs()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var discoveryJob: Job? = null
    private var connectedUrl: String? = null

    init {
        scope.launch {
            // StateFlow ja e conflado: nao precisa de distinctUntilChanged.
            settings.collect { applySettings(it) }
        }
        // Assim que a conexão sobe, sincroniza a biblioteca e casa os títulos.
        scope.launch {
            status
                .map { it.state }
                .distinctUntilChanged()
                .collect { state ->
                    if (state == SyncState.CONNECTED) refreshLibrary()
                }
        }
    }

    private fun applySettings(current: StageSyncSettings) {
        if (!current.enabled) {
            stopDiscovery()
            client.disconnect()
            connectedUrl = null
            return
        }
        val known = current.manualUrl ?: current.lastUrl
        if (known != null) {
            connect(known)
        } else {
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        if (discoveryJob != null) return
        discoveryJob = scope.launch {
            discovery.discover().collect { servers ->
                discoveredState.value = servers
                // Só conecta sozinho quando há exatamente um candidato.
                // Com dois notebooks na rede, quem escolhe é a pessoa.
                if (connectedUrl == null && servers.size == 1) {
                    connect(servers.first().url)
                }
            }
        }
    }

    private fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        discoveredState.value = emptyList()
    }

    fun connect(rawUrl: String) {
        val url = StudioApi.normalizeUrl(rawUrl, StageSyncSettings.DEFAULT_PORT)
        if (url == null) {
            messageState.value = "Endereço vazio."
            return
        }
        connectedUrl = url
        client.connect(url, settings.value.deviceName)
        scope.launch {
            settingsRepository.updateSync { it.copy(lastUrl = url) }
        }
    }

    fun disconnect() {
        client.disconnect()
        connectedUrl = null
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch { settingsRepository.updateSync { it.copy(enabled = enabled) } }
    }

    fun setFollowSong(follow: Boolean) {
        scope.launch { settingsRepository.updateSync { it.copy(followSong = follow) } }
    }

    fun setFollowSections(follow: Boolean) {
        scope.launch { settingsRepository.updateSync { it.copy(followSections = follow) } }
    }

    fun setManualUrl(raw: String) {
        val url = StudioApi.normalizeUrl(raw, StageSyncSettings.DEFAULT_PORT)
        scope.launch {
            settingsRepository.updateSync { it.copy(manualUrl = url, lastUrl = url) }
        }
    }

    fun clearManualUrl() {
        scope.launch { settingsRepository.updateSync { it.copy(manualUrl = null) } }
    }

    /** Lê a biblioteca do Studio e casa os títulos que ainda não foram associados. */
    fun refreshLibrary() {
        val url = connectedUrl ?: status.value.serverUrl ?: return
        scope.launch {
            api.songs(url)
                .onSuccess { songs ->
                    studioSongsState.value = songs
                    val linked = songRepository.autoLinkByTitle(songs)
                    messageState.value = when {
                        songs.isEmpty() -> "O Studio não tem músicas cadastradas."
                        linked > 0 -> "Biblioteca sincronizada: " + linked + " música(s) associadas pelo título."
                        else -> "Biblioteca sincronizada: " + songs.size + " música(s) no Studio."
                    }
                }
                .onFailure { messageState.value = "Não consegui ler a biblioteca do Studio: " + it.message }
        }
    }

    fun linkSong(studioSongId: Int, localSong: Song) {
        scope.launch { songRepository.linkStudioSong(localSong.id, studioSongId) }
    }

    fun unlinkSong(localSong: Song) {
        scope.launch { songRepository.linkStudioSong(localSong.id, null) }
    }

    fun clearMessage() {
        messageState.value = null
    }
}

/** O que o Studio está tocando, já traduzido para o repertório daqui. */
data class FollowTarget(
    val studioSongId: Int,
    val localSong: Song?,
    val partIndex: Int,
    val timelinePos: Float = 0f,
    /** Bloco ativo da trilha Controladora, quando ela existir. */
    val control: StudioClip? = null,
    /** Bloco ativo da trilha de seções (`tr-section`). */
    val section: StudioClip? = null
) {
    /**
     * O bloco que manda agora. A Controladora ganha da trilha de seções:
     * uma é o que você desenhou para o timbre, a outra é a estrutura da
     * música. Quando as duas existem, a intenção explícita vence.
     */
    val activeClip: StudioClip? get() = control ?: section

    /** Identidade do que está valendo, para o app só agir quando muda. */
    val clipKey: String? get() = activeClip?.let { studioSongId.toString() + ":" + it.key }
}
