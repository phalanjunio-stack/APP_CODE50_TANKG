package com.srlakes.tone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        SceneEntity::class,
        SongSectionEntity::class,
        PresetEntity::class,
        SetlistEntity::class,
        SetlistItemEntity::class,
        DeviceEntity::class,
        AnalysisSessionEntity::class,
        AnalysisSnapshotEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ToneDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun presetDao(): PresetDao
    abstract fun setlistDao(): SetlistDao
    abstract fun deviceDao(): DeviceDao
    abstract fun analysisDao(): AnalysisDao

    companion object {

        private const val NAME = "srlakes-tone.db"

        /**
         * v2: liga cada musica ao id dela no SR Lakes Studio, para o
         * timbre poder seguir a troca de musica no notebook.
         * Migracao de verdade: ninguem perde o repertorio numa atualizacao.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN studioSongId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_studioSongId ON songs(studioSongId)")
            }
        }

        @Volatile
        private var instance: ToneDatabase? = null

        fun get(context: Context): ToneDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): ToneDatabase =
            Room.databaseBuilder(context, ToneDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}
