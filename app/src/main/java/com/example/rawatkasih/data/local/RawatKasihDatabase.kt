package com.example.rawatkasih.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rawatkasih.feature.obat.data.local.JadwalObatEntity
import com.example.rawatkasih.feature.obat.data.local.ObatDao
import com.example.rawatkasih.feature.obat.data.local.RencanaObatEntity

@Database(entities = [RencanaObatEntity::class, JadwalObatEntity::class], version = 2)
internal abstract class RawatKasihDatabase : RoomDatabase() {
    abstract fun obatDao(): ObatDao

    companion object {
        @Volatile
        private var instance: RawatKasihDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jadwal_obat ADD COLUMN alasanPenolakan TEXT NOT NULL DEFAULT ''")
            }
        }

        fun ambil(context: Context): RawatKasihDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RawatKasihDatabase::class.java,
                "rawat-kasih.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
