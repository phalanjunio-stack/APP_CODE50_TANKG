package com.srlakes.tone.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY position ASC, title ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun byId(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE studioSongId = :studioSongId LIMIT 1")
    suspend fun byStudioId(studioSongId: Int): SongEntity?

    @Query("SELECT * FROM songs")
    suspend fun all(): List<SongEntity>

    @Query("UPDATE songs SET studioSongId = NULL WHERE studioSongId = :studioSongId")
    suspend fun clearStudioLink(studioSongId: Int)

    @Query("UPDATE songs SET studioSongId = :studioSongId WHERE id = :songId")
    suspend fun setStudioLink(songId: Long, studioSongId: Int?)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity): Long

    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("SELECT * FROM scenes WHERE songId = :songId ORDER BY position ASC")
    fun observeScenes(songId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE songId = :songId ORDER BY position ASC")
    suspend fun scenesOf(songId: Long): List<SceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity): Long

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Delete
    suspend fun deleteScene(scene: SceneEntity)

    @Query("SELECT * FROM sections WHERE songId = :songId ORDER BY position ASC")
    fun observeSections(songId: Long): Flow<List<SongSectionEntity>>

    @Query("SELECT * FROM sections WHERE songId = :songId ORDER BY position ASC")
    suspend fun sectionsOf(songId: Long): List<SongSectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SongSectionEntity): Long

    @Update
    suspend fun updateSection(section: SongSectionEntity)

    @Delete
    suspend fun deleteSection(section: SongSectionEntity)
}

@Dao
interface PresetDao {

    @Query("SELECT * FROM presets ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun byId(id: Long): PresetEntity?

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    @Update
    suspend fun update(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)
}

@Dao
interface SetlistDao {

    @Query("SELECT * FROM setlists ORDER BY id DESC")
    fun observeAll(): Flow<List<SetlistEntity>>

    @Query("SELECT * FROM setlists WHERE id = :id")
    suspend fun byId(id: Long): SetlistEntity?

    @Query("SELECT COUNT(*) FROM setlists")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setlist: SetlistEntity): Long

    @Update
    suspend fun update(setlist: SetlistEntity)

    @Delete
    suspend fun delete(setlist: SetlistEntity)

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN setlist_items ON songs.id = setlist_items.songId " +
            "WHERE setlist_items.setlistId = :setlistId " +
            "ORDER BY setlist_items.position ASC"
    )
    fun observeSongs(setlistId: Long): Flow<List<SongEntity>>

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN setlist_items ON songs.id = setlist_items.songId " +
            "WHERE setlist_items.setlistId = :setlistId " +
            "ORDER BY setlist_items.position ASC"
    )
    suspend fun songsOf(setlistId: Long): List<SongEntity>

    @Query("SELECT * FROM setlist_items WHERE setlistId = :setlistId ORDER BY position ASC")
    suspend fun itemsOf(setlistId: Long): List<SetlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SetlistItemEntity): Long

    @Delete
    suspend fun deleteItem(item: SetlistItemEntity)

    @Query("DELETE FROM setlist_items WHERE setlistId = :setlistId")
    suspend fun clearItems(setlistId: Long)

    @Transaction
    suspend fun replaceItems(setlistId: Long, songIds: List<Long>) {
        clearItems(setlistId)
        songIds.forEachIndexed { index, songId ->
            insertItem(SetlistItemEntity(setlistId = setlistId, songId = songId, position = index))
        }
    }
}

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE kind = :kind")
    suspend fun byKind(kind: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)
}

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analysis_sessions ORDER BY createdAtMs DESC")
    fun observeSessions(): Flow<List<AnalysisSessionEntity>>

    @Query("SELECT * FROM analysis_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): AnalysisSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AnalysisSessionEntity): Long

    @Update
    suspend fun updateSession(session: AnalysisSessionEntity)

    @Delete
    suspend fun deleteSession(session: AnalysisSessionEntity)

    @Query("SELECT * FROM analysis_snapshots WHERE sessionId = :sessionId ORDER BY id ASC")
    fun observeSnapshots(sessionId: Long): Flow<List<AnalysisSnapshotEntity>>

    @Query("SELECT * FROM analysis_snapshots WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun snapshotsOf(sessionId: Long): List<AnalysisSnapshotEntity>

    @Query("SELECT * FROM analysis_snapshots ORDER BY id DESC")
    fun observeAllSnapshots(): Flow<List<AnalysisSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: AnalysisSnapshotEntity): Long
}
