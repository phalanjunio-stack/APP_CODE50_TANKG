package com.srlakes.tone.feature.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.data.PresetRepository
import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.data.SetlistRepository
import com.srlakes.tone.data.SongRepository
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.Scene
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.model.SectionType
import com.srlakes.tone.model.Setlist
import com.srlakes.tone.model.SetlistDetail
import com.srlakes.tone.model.Song
import com.srlakes.tone.model.SongDetail
import com.srlakes.tone.model.SongSection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SongsViewModel(
    private val songRepository: SongRepository,
    private val presetRepository: PresetRepository,
    private val setlistRepository: SetlistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val songs: StateFlow<List<Song>> = songRepository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<Preset>> = presetRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val setlists: StateFlow<List<Setlist>> = setlistRepository.observeSetlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSetlistId: StateFlow<Long?> = settingsRepository.settings
        .map { it.activeSetlistId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val editingSongId = MutableStateFlow<Long?>(null)

    val editingSong: StateFlow<SongDetail?> = editingSongId
        .flatMapLatest { id -> if (id == null) flowOf(null) else songRepository.observeDetail(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val editingSetlistId = MutableStateFlow<Long?>(null)

    val editingSetlist: StateFlow<SetlistDetail?> = editingSetlistId
        .flatMapLatest { id -> if (id == null) flowOf(null) else setlistRepository.observeDetail(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun openSong(id: Long) {
        editingSongId.value = id
    }

    fun openSetlist(id: Long) {
        editingSetlistId.value = id
    }

    fun createSong(title: String) {
        viewModelScope.launch {
            val id = songRepository.saveSong(
                Song(title = title.ifBlank { "Nova música" }),
                position = songs.value.size
            )
            // Toda musica nasce com o esqueleto de cenas e secoes: sem isso
            // a tela Performance abriria vazia justamente no ensaio.
            val cleanId = songRepository.saveScene(
                Scene(songId = id, name = "CLEAN", sceneType = SceneType.CLEAN, presetId = defaultPreset(SceneType.CLEAN), position = 0)
            )
            val baseId = songRepository.saveScene(
                Scene(songId = id, name = "BASE", sceneType = SceneType.BASE, presetId = defaultPreset(SceneType.BASE), position = 1)
            )
            val soloId = songRepository.saveScene(
                Scene(songId = id, name = "SOLO", sceneType = SceneType.SOLO, presetId = defaultPreset(SceneType.SOLO), position = 2)
            )
            val defaults = listOf(
                SectionType.INTRO to cleanId,
                SectionType.VERSO to baseId,
                SectionType.REFRAO to baseId,
                SectionType.SOLO to soloId,
                SectionType.OUTRO to baseId
            )
            defaults.forEachIndexed { index, (type, sceneId) ->
                songRepository.saveSection(
                    SongSection(songId = id, type = type, sceneId = sceneId, position = index)
                )
            }
            editingSongId.value = id
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch { songRepository.saveSong(song) }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songRepository.deleteSong(song)
            if (editingSongId.value == song.id) editingSongId.value = null
        }
    }

    fun updateScene(scene: Scene) {
        viewModelScope.launch { songRepository.saveScene(scene) }
    }

    fun addScene(songId: Long, type: SceneType) {
        viewModelScope.launch {
            val position = editingSong.value?.scenes?.size ?: 0
            songRepository.saveScene(
                Scene(
                    songId = songId,
                    name = type.label,
                    sceneType = type,
                    presetId = defaultPreset(type),
                    position = position
                )
            )
        }
    }

    fun deleteScene(scene: Scene) {
        viewModelScope.launch { songRepository.deleteScene(scene) }
    }

    fun updateSection(section: SongSection) {
        viewModelScope.launch { songRepository.saveSection(section) }
    }

    fun addSection(songId: Long, type: SectionType) {
        viewModelScope.launch {
            val detail = editingSong.value
            songRepository.saveSection(
                SongSection(
                    songId = songId,
                    type = type,
                    sceneId = detail?.scenes?.firstOrNull { it.sceneType == SceneType.BASE }?.id
                        ?: detail?.scenes?.firstOrNull()?.id,
                    position = detail?.sections?.size ?: 0
                )
            )
        }
    }

    fun deleteSection(section: SongSection) {
        viewModelScope.launch { songRepository.deleteSection(section) }
    }

    fun createSetlist(name: String, dateLabel: String) {
        viewModelScope.launch {
            val id = setlistRepository.save(
                Setlist(name = name.ifBlank { "Novo show" }, dateLabel = dateLabel)
            )
            editingSetlistId.value = id
        }
    }

    fun deleteSetlist(setlist: Setlist) {
        viewModelScope.launch { setlistRepository.delete(setlist) }
    }

    fun setActiveSetlist(id: Long?) {
        viewModelScope.launch { settingsRepository.setActiveSetlist(id) }
    }

    fun addSongToSetlist(setlistId: Long, songId: Long) {
        viewModelScope.launch {
            val current = setlistRepository.detail(setlistId)?.songs.orEmpty().map { it.id }
            if (songId in current) return@launch
            setlistRepository.setSongs(setlistId, current + songId)
        }
    }

    fun removeSongFromSetlist(setlistId: Long, songId: Long) {
        viewModelScope.launch {
            val current = setlistRepository.detail(setlistId)?.songs.orEmpty().map { it.id }
            setlistRepository.setSongs(setlistId, current.filterNot { it == songId })
        }
    }

    fun moveSongInSetlist(setlistId: Long, songId: Long, delta: Int) {
        viewModelScope.launch {
            val current = setlistRepository.detail(setlistId)?.songs.orEmpty().map { it.id }.toMutableList()
            val index = current.indexOf(songId)
            val target = index + delta
            if (index < 0 || target < 0 || target >= current.size) return@launch
            current.removeAt(index)
            current.add(target, songId)
            setlistRepository.setSongs(setlistId, current)
        }
    }

    private fun defaultPreset(type: SceneType): Long? {
        val list = presets.value
        val wanted = when (type) {
            SceneType.CLEAN -> "SR Clean Valve"
            SceneType.BASE -> "SR Pop Rock Base"
            SceneType.SOLO -> "SR Pop Rock Solo"
        }
        return list.firstOrNull { it.name == wanted }?.id ?: list.firstOrNull()?.id
    }
}
