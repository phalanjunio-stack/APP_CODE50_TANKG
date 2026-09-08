package com.srlakes.tone.data

import com.srlakes.tone.data.db.AnalysisDao
import com.srlakes.tone.data.db.DeviceDao
import com.srlakes.tone.data.db.DeviceEntity
import com.srlakes.tone.data.db.PresetDao
import com.srlakes.tone.data.db.SetlistDao
import com.srlakes.tone.data.db.SongDao
import com.srlakes.tone.model.AnalysisSession
import com.srlakes.tone.model.AnalysisSnapshotRecord
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.Scene
import com.srlakes.tone.model.Setlist
import com.srlakes.tone.model.SetlistDetail
import com.srlakes.tone.model.SnapshotRole
import com.srlakes.tone.model.Song
import com.srlakes.tone.model.StudioSong
import com.srlakes.tone.model.normalizeTitle
import com.srlakes.tone.model.SongDetail
import com.srlakes.tone.model.SongSection
import com.srlakes.tone.model.ToneSnapshot
import com.srlakes.tone.model.TransportKind
import com.srlakes.tone.protocol.DeviceProfile
import com.srlakes.tone.protocol.ParamId
import com.srlakes.tone.protocol.ParamMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SongRepository(private val dao: SongDao) {

    fun observeSongs(): Flow<List<Song>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeDetail(songId: Long): Flow<SongDetail?> =
        combine(
            dao.observeAll(),
            dao.observeScenes(songId),
            dao.observeSections(songId)
        ) { songs, scenes, sections ->
            val song = songs.firstOrNull { it.id == songId } ?: return@combine null
            SongDetail(
                song = song.toModel(),
                scenes = scenes.map { it.toModel() },
                sections = sections.map { it.toModel() }
            )
        }

    suspend fun detail(songId: Long): SongDetail? {
        val song = dao.byId(songId) ?: return null
        return SongDetail(
            song = song.toModel(),
            scenes = dao.scenesOf(songId).map { it.toModel() },
            sections = dao.sectionsOf(songId).map { it.toModel() }
        )
    }

    suspend fun saveSong(song: Song, position: Int = 0): Long =
        if (song.id == 0L) dao.insert(song.toEntity(position))
        else { dao.update(song.toEntity(position)); song.id }

    suspend fun deleteSong(song: Song) = dao.delete(song.toEntity())

    suspend fun songByStudioId(studioSongId: Int): Song? =
        dao.byStudioId(studioSongId)?.toModel()

    /**
     * Associa uma musica local a uma musica do Studio. Um id do Studio
     * pertence a uma musica so: associar de novo solta a anterior.
     */
    suspend fun linkStudioSong(songId: Long, studioSongId: Int?) {
        if (studioSongId != null) dao.clearStudioLink(studioSongId)
        dao.setStudioLink(songId, studioSongId)
    }

    /**
     * Casa automaticamente pelo titulo normalizado - sem acento, sem
     * pontuacao, sem caixa. So preenche o que ainda esta vazio; nunca
     * desfaz uma associacao que a pessoa fez na mao.
     *
     * @return quantas associacoes novas foram criadas.
     */
    suspend fun autoLinkByTitle(studioSongs: List<StudioSong>): Int {
        val locals = dao.all()
        val taken = locals.mapNotNull { it.studioSongId }.toMutableSet()
        val byTitle = locals
            .filter { it.studioSongId == null }
            .associateBy { normalizeTitle(it.title) }
            .toMutableMap()

        var linked = 0
        for (studio in studioSongs) {
            if (studio.id in taken) continue
            val match = byTitle.remove(normalizeTitle(studio.title)) ?: continue
            dao.setStudioLink(match.id, studio.id)
            taken += studio.id
            linked++
        }
        return linked
    }

    suspend fun saveScene(scene: Scene): Long =
        if (scene.id == 0L) dao.insertScene(scene.toEntity())
        else { dao.updateScene(scene.toEntity()); scene.id }

    suspend fun deleteScene(scene: Scene) = dao.deleteScene(scene.toEntity())

    suspend fun saveSection(section: SongSection): Long =
        if (section.id == 0L) dao.insertSection(section.toEntity())
        else { dao.updateSection(section.toEntity()); section.id }

    suspend fun deleteSection(section: SongSection) = dao.deleteSection(section.toEntity())
}

class PresetRepository(private val dao: PresetDao) {

    fun observePresets(): Flow<List<Preset>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun byId(id: Long): Preset? = dao.byId(id)?.toModel()

    suspend fun save(preset: Preset): Long =
        if (preset.id == 0L) dao.insert(preset.toEntity())
        else { dao.update(preset.toEntity()); preset.id }

    suspend fun delete(preset: Preset) = dao.delete(preset.toEntity())
}

class SetlistRepository(
    private val dao: SetlistDao
) {

    fun observeSetlists(): Flow<List<Setlist>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeDetail(setlistId: Long): Flow<SetlistDetail?> =
        combine(dao.observeAll(), dao.observeSongs(setlistId)) { setlists, songs ->
            val setlist = setlists.firstOrNull { it.id == setlistId } ?: return@combine null
            SetlistDetail(setlist.toModel(), songs.map { it.toModel() })
        }

    suspend fun detail(setlistId: Long): SetlistDetail? {
        val setlist = dao.byId(setlistId) ?: return null
        return SetlistDetail(setlist.toModel(), dao.songsOf(setlistId).map { it.toModel() })
    }

    suspend fun save(setlist: Setlist): Long =
        if (setlist.id == 0L) dao.insert(setlist.toEntity())
        else { dao.update(setlist.toEntity()); setlist.id }

    suspend fun delete(setlist: Setlist) = dao.delete(setlist.toEntity())

    suspend fun setSongs(setlistId: Long, songIds: List<Long>) =
        dao.replaceItems(setlistId, songIds)
}

/**
 * Guarda o que o usuario descobriu sobre cada aparelho.
 *
 * Os mapeamentos aprendidos no MIDI Learn moram aqui, num formato de
 * texto simples: "GAIN:20:0;BASS:21:0". Nao vale trazer uma dependencia
 * de JSON para tao pouco, e assim da para inspecionar o banco a olho.
 */
class DeviceRepository(private val dao: DeviceDao) {

    fun observeAll(): Flow<List<DeviceEntity>> = dao.observeAll()

    suspend fun loadProfile(kind: DeviceKind, fallback: DeviceProfile): DeviceProfile {
        val entity = dao.byKind(kind.name) ?: return fallback
        val learned = decodeMappings(entity.mappings)
        if (learned.isEmpty()) return fallback.copy(channel = entity.midiChannel)
        return fallback.copy(
            channel = entity.midiChannel,
            mappings = fallback.mappings + learned
        )
    }

    suspend fun saveProfile(profile: DeviceProfile, address: String?, transport: TransportKind) {
        dao.upsert(
            DeviceEntity(
                kind = profile.kind.name,
                address = address,
                transport = transport.name,
                lastConnectedAtMs = System.currentTimeMillis(),
                midiChannel = profile.channel,
                mappings = encodeMappings(profile.mappings.values.filter { it.learned })
            )
        )
    }

    private fun encodeMappings(mappings: Collection<ParamMapping>): String =
        mappings.joinToString(separator = ";") {
            it.param.name + ":" + it.controller + ":" + it.channel
        }

    private fun decodeMappings(text: String): Map<ParamId, ParamMapping> {
        if (text.isBlank()) return emptyMap()
        return text.split(";").mapNotNull { chunk ->
            val parts = chunk.split(":")
            if (parts.size < 3) return@mapNotNull null
            val param = ParamId.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val cc = parts[1].toIntOrNull() ?: return@mapNotNull null
            val channel = parts[2].toIntOrNull() ?: 0
            param to ParamMapping(param, cc, channel, learned = true)
        }.toMap()
    }
}

class AnalysisRepository(private val dao: AnalysisDao) {

    fun observeSessions(): Flow<List<AnalysisSession>> =
        dao.observeSessions().map { list -> list.map { it.toModel() } }

    fun observeSnapshots(sessionId: Long): Flow<List<AnalysisSnapshotRecord>> =
        dao.observeSnapshots(sessionId).map { list -> list.map { it.toModel() } }

    suspend fun snapshotsOf(sessionId: Long): List<AnalysisSnapshotRecord> =
        dao.snapshotsOf(sessionId).map { it.toModel() }

    suspend fun createSession(session: AnalysisSession): Long = dao.insertSession(session.toEntity())

    suspend fun updateNote(session: AnalysisSession, note: String) =
        dao.updateSession(session.copy(note = note).toEntity())

    suspend fun deleteSession(session: AnalysisSession) = dao.deleteSession(session.toEntity())

    suspend fun addSnapshot(
        sessionId: Long,
        role: SnapshotRole,
        snapshot: ToneSnapshot,
        pcmPath: String? = null
    ): Long = dao.insertSnapshot(
        AnalysisSnapshotRecord(sessionId = sessionId, role = role, snapshot = snapshot)
            .toEntity(pcmPath)
    )
}
