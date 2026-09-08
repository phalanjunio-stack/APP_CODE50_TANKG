package com.srlakes.tone.data

import com.srlakes.tone.data.db.SceneEntity
import com.srlakes.tone.data.db.SetlistEntity
import com.srlakes.tone.data.db.SetlistItemEntity
import com.srlakes.tone.data.db.SongEntity
import com.srlakes.tone.data.db.SongSectionEntity
import com.srlakes.tone.data.db.ToneDatabase
import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.MacroSet
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.PresetCategory
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.model.SectionType

/**
 * Dados de demonstracao, criados uma vez, na primeira abertura.
 *
 * Existem para o app ja abrir util: da para ensaiar a navegacao entre
 * musicas, cenas e secoes sem cadastrar nada antes de um ensaio.
 * Tudo pode ser editado ou apagado.
 */
object DemoData {

    suspend fun seedIfEmpty(db: ToneDatabase) {
        val presetDao = db.presetDao()
        val songDao = db.songDao()
        val setlistDao = db.setlistDao()

        if (presetDao.count() > 0 || songDao.count() > 0) return

        val presetIds = HashMap<String, Long>()
        for (preset in builtInPresets()) {
            presetIds[preset.name] = presetDao.insert(preset.toEntity())
        }

        val songIds = ArrayList<Long>()
        demoSongs().forEachIndexed { index, song ->
            val songId = songDao.insert(song.copy(position = index))
            songIds += songId
            seedScenesAndSections(db, songId, presetIds)
        }

        if (setlistDao.count() == 0) {
            val setlistId = setlistDao.insert(
                SetlistEntity(name = "SHOW 07/09", dateLabel = "07/09")
            )
            songIds.forEachIndexed { index, songId ->
                setlistDao.insertItem(
                    SetlistItemEntity(setlistId = setlistId, songId = songId, position = index)
                )
            }
        }
    }

    private suspend fun seedScenesAndSections(
        db: ToneDatabase,
        songId: Long,
        presetIds: Map<String, Long>
    ) {
        val dao = db.songDao()

        val cleanId = dao.insertScene(
            SceneEntity(
                songId = songId,
                name = "CLEAN VALVE",
                sceneType = SceneType.CLEAN.name,
                presetId = presetIds["SR Clean Valve"],
                position = 0
            )
        )
        val crunchId = dao.insertScene(
            SceneEntity(
                songId = songId,
                name = "BASE CRUNCH",
                sceneType = SceneType.BASE.name,
                presetId = presetIds["SR Crunch"],
                position = 1
            )
        )
        val baseId = dao.insertScene(
            SceneEntity(
                songId = songId,
                name = "BASE VALVE",
                sceneType = SceneType.BASE.name,
                presetId = presetIds["SR Pop Rock Base"],
                position = 2
            )
        )
        val soloId = dao.insertScene(
            SceneEntity(
                songId = songId,
                name = "SOLO VALVE",
                sceneType = SceneType.SOLO.name,
                presetId = presetIds["SR Pop Rock Solo"],
                position = 3
            )
        )

        val layout = listOf(
            SectionType.INTRO to cleanId,
            SectionType.VERSO to crunchId,
            SectionType.REFRAO to baseId,
            SectionType.PONTE to crunchId,
            SectionType.SOLO to soloId,
            SectionType.OUTRO to baseId
        )
        layout.forEachIndexed { index, (type, sceneId) ->
            dao.insertSection(
                SongSectionEntity(
                    songId = songId,
                    type = type.name,
                    sceneId = sceneId,
                    bars = if (type == SectionType.INTRO) 4 else 8,
                    position = index
                )
            )
        }
    }

    private fun demoSongs(): List<SongEntity> = listOf(
        SongEntity(title = "Meu Erro", artist = "Biquíni Cavadão", musicalKey = "A", bpm = 126),
        SongEntity(title = "O Sol", artist = "Vitor Kley", musicalKey = "D", bpm = 100),
        SongEntity(title = "Tempo Perdido", artist = "Legião Urbana", musicalKey = "E", bpm = 148),
        SongEntity(title = "Ana Júlia", artist = "Los Hermanos", musicalKey = "G", bpm = 132),
        SongEntity(title = "Só Hoje", artist = "Jota Quest", musicalKey = "C", bpm = 118)
    )

    /**
     * Presets iniciais. Os campos tankGProgram e codeProgram ficam nulos
     * de proposito: so fazem sentido depois que o protocolo real for
     * mapeado e o usuario souber a que patch cada numero corresponde.
     */
    fun builtInPresets(): List<Preset> = listOf(
        Preset(
            name = "SR Clean Valve",
            category = PresetCategory.CLEAN,
            macros = MacroSet(drive = 2.0f, warmth = 6.0f, body = 5.5f, presence = 5.0f, volume = 6.5f),
            amp = AmpParams(gain = 2.0f, bass = 5.5f, middle = 5.0f, treble = 5.5f, presence = 5.0f, resonance = 5.5f, volume = 6.5f),
            effects = EffectState(delay = false, reverb = true, boost = false, gate = true, modulation = false),
            notes = "Base limpa para intros e partes de acompanhamento.",
            builtIn = true
        ),
        Preset(
            name = "SR Dry Valve",
            category = PresetCategory.CLEAN,
            macros = MacroSet(drive = 1.5f, warmth = 4.5f, body = 4.5f, presence = 6.0f, volume = 6.0f),
            amp = AmpParams(gain = 1.5f, bass = 4.5f, middle = 5.0f, treble = 6.5f, presence = 6.5f, resonance = 4.0f, volume = 6.0f),
            effects = EffectState(delay = false, reverb = false, boost = false, gate = true, modulation = false),
            notes = "Limpo seco, sem reverb. Bom para gravar e para conferir o timbre puro.",
            builtIn = true
        ),
        Preset(
            name = "SR Pop Rock Base",
            category = PresetCategory.BASE,
            macros = MacroSet(drive = 4.0f, warmth = 6.5f, body = 5.5f, presence = 6.0f, volume = 7.0f),
            amp = AmpParams(gain = 4.0f, bass = 6.0f, middle = 5.5f, treble = 5.5f, presence = 6.0f, resonance = 5.5f, volume = 7.0f),
            effects = EffectState(delay = false, reverb = true, boost = false, gate = true, modulation = false),
            notes = "A cena BASE VALVE. Ponto de partida do show.",
            builtIn = true
        ),
        Preset(
            name = "SR Pop Rock Solo",
            category = PresetCategory.SOLO,
            macros = MacroSet(drive = 6.5f, warmth = 5.5f, body = 6.5f, presence = 6.5f, volume = 8.0f),
            amp = AmpParams(gain = 6.5f, bass = 5.5f, middle = 6.5f, treble = 6.0f, presence = 6.5f, resonance = 6.0f, volume = 8.0f),
            effects = EffectState(delay = true, reverb = true, boost = true, gate = true, modulation = false),
            notes = "Cerca de +3 dB sobre a base. Confira no BASE x SOLO antes do show.",
            builtIn = true
        ),
        Preset(
            name = "SR Crunch",
            category = PresetCategory.CRUNCH,
            macros = MacroSet(drive = 5.5f, warmth = 5.5f, body = 6.0f, presence = 5.5f, volume = 7.0f),
            amp = AmpParams(gain = 5.5f, bass = 5.5f, middle = 6.0f, treble = 5.5f, presence = 5.5f, resonance = 5.5f, volume = 7.0f),
            effects = EffectState(delay = false, reverb = true, boost = false, gate = true, modulation = false),
            notes = "Meio termo entre limpo e distorcido, para versos com peso.",
            builtIn = true
        ),
        Preset(
            name = "SR High Gain",
            category = PresetCategory.HIGH_GAIN,
            macros = MacroSet(drive = 8.5f, warmth = 5.0f, body = 6.0f, presence = 6.0f, volume = 7.0f),
            amp = AmpParams(gain = 8.5f, bass = 6.0f, middle = 5.5f, treble = 6.0f, presence = 6.0f, resonance = 6.5f, volume = 7.0f),
            effects = EffectState(delay = false, reverb = true, boost = false, gate = true, modulation = false),
            notes = "Cuidado com o fizz de 6 a 10 kHz. O analisador avisa.",
            builtIn = true
        ),
        Preset(
            name = "SR Acoustic Sim",
            category = PresetCategory.ACOUSTIC,
            macros = MacroSet(drive = 1.0f, warmth = 5.0f, body = 4.0f, presence = 7.0f, volume = 6.5f),
            amp = AmpParams(gain = 1.0f, bass = 5.0f, middle = 4.0f, treble = 7.0f, presence = 7.0f, resonance = 4.0f, volume = 6.5f),
            effects = EffectState(delay = false, reverb = true, boost = false, gate = true, modulation = false),
            notes = "Simulacao de violao, para partes mais abertas.",
            builtIn = true
        )
    )
}
