package com.example.rawatkasih.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rawatkasih.feature.obat.data.local.JadwalObatEntity
import com.example.rawatkasih.feature.obat.data.local.ObatDao
import com.example.rawatkasih.feature.obat.data.local.RencanaObatEntity

@Database(entities = [RencanaObatEntity::class, JadwalObatEntity::class], version = 1)
internal abstract class RawatKasihDatabase : RoomDatabase() {
    abstract fun obatDao(): ObatDao

    companion object {
        @Volatile
        private var instance: RawatKasihDatabase? = null

        fun ambil(context: Context): RawatKasihDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                RawatKasihDatabase::class.java,
                "rawat-kasih.db"
            ).build().also { instance = it }
        }
    }
}
