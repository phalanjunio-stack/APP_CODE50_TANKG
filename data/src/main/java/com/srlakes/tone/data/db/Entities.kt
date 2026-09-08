package com.srlakes.tone.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs", indices = [Index("studioSongId")])
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "",
    val musicalKey: String = "",
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val notes: String = "",
    val position: Int = 0,
    /**
     * Id desta musica na biblioteca do SR Lakes Studio.
     * E o que permite o timbre seguir a troca de musica no notebook.
     * Nulo enquanto ninguem tiver associado.
     */
    val studioSongId: Int? = null
)

@Entity(
    tableName = "presets",
    indices = [Index("category")]
)
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val macroDrive: Float,
    val macroWarmth: Float,
    val macroBody: Float,
    val macroPresence: Float,
    val macroVolume: Float,
    val ampGain: Float,
    val ampBass: Float,
    val ampMiddle: Float,
    val ampTreble: Float,
    val ampPresence: Float,
    val ampResonance: Float,
    val ampVolume: Float,
    val fxDelay: Boolean,
    val fxReverb: Boolean,
    val fxBoost: Boolean,
    val fxGate: Boolean,
    val fxModulation: Boolean,
    val tankGProgram: Int?,
    val codeProgram: Int?,
    val notes: String = "",
    val builtIn: Boolean = false
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId"), Index("presetId")]
)
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val name: String,
    val sceneType: String,
    val presetId: Long?,
    val position: Int = 0
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId"), Index("sceneId")]
)
data class SongSectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val type: String,
    val customLabel: String? = null,
    val sceneId: Long?,
    val bars: Int = 4,
    val position: Int = 0
)

@Entity(tableName = "setlists")
data class SetlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateLabel: String = ""
)

@Entity(
    tableName = "setlist_items",
    foreignKeys = [
        ForeignKey(
            entity = SetlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["setlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("setlistId"), Index("songId")]
)
data class SetlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setlistId: Long,
    val songId: Long,
    val position: Int
)

/**
 * Ultimo aparelho conhecido e os mapeamentos de CC aprendidos pelo usuario.
 *
 * [mappings] guarda o resultado do MIDI Learn no formato
 * "PARAM:cc:canal" separado por ponto e virgula. Formato simples de
 * proposito: nao vale trazer uma biblioteca de JSON so para isso.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val kind: String,
    val address: String? = null,
    val transport: String = "MOCK",
    val lastConnectedAtMs: Long = 0,
    val midiChannel: Int = 0,
    val mappings: String = ""
)

@Entity(tableName = "analysis_sessions")
data class AnalysisSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songTitle: String,
    val context: String,
    val dateLabel: String,
    val createdAtMs: Long,
    val note: String = ""
)

@Entity(
    tableName = "analysis_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class AnalysisSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val label: String,
    val capturedAtMs: Long,
    val durationMs: Long,
    val rmsDb: Float,
    val peakDb: Float,
    val noiseFloorDb: Float,
    val crestFactorDb: Float,
    val bandDb: List<Float>,
    val bandShare: List<Float>,
    val averageSpectrum: List<Float>,
    val clipped: Boolean,
    /** Caminho do WAV cru, quando o audio da medicao foi guardado. */
    val pcmPath: String? = null
)
