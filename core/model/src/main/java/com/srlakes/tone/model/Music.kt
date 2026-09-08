package com.srlakes.tone.model

data class Song(
    val id: Long = 0L,
    val title: String,
    val artist: String = "",
    val key: String = "",
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val notes: String = "",
    /** Id desta musica no SR Lakes Studio, quando associada. */
    val studioSongId: Int? = null
)

/**
 * Cena dentro de uma musica (ex.: "BASE VALVE"). Aponta para um preset.
 */
data class Scene(
    val id: Long = 0L,
    val songId: Long,
    val name: String,
    val sceneType: SceneType,
    val presetId: Long?,
    val position: Int = 0
)

/**
 * Secao da musica (INTRO, VERSO, ...). Cada secao carrega uma cena
 * quando o musico toca nela. Nunca automaticamente.
 */
data class SongSection(
    val id: Long = 0L,
    val songId: Long,
    val type: SectionType,
    val customLabel: String? = null,
    val sceneId: Long?,
    val bars: Int = 4,
    val position: Int = 0
) {
    val label: String get() = customLabel ?: type.label
}

/** Musica com suas cenas e secoes ja resolvidas. */
data class SongDetail(
    val song: Song,
    val scenes: List<Scene>,
    val sections: List<SongSection>
) {
    fun sceneById(id: Long?): Scene? = scenes.firstOrNull { it.id == id }
    fun sceneFor(section: SongSection): Scene? = sceneById(section.sceneId)
}

data class Setlist(
    val id: Long = 0L,
    val name: String,
    val dateLabel: String = ""
)

data class SetlistEntry(
    val id: Long = 0L,
    val setlistId: Long,
    val songId: Long,
    val position: Int
)

data class SetlistDetail(
    val setlist: Setlist,
    val songs: List<Song>
)
