package com.example.rawatkasih.feature.obat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ObatDao {
    @Transaction
    @Query("SELECT * FROM rencana_obat WHERE id = :id")
    fun amatiRencana(id: Int): Flow<RencanaObatDenganJadwal?>

    @Transaction
    @Query("SELECT * FROM rencana_obat WHERE id = :id")
    suspend fun ambilRencana(id: Int): RencanaObatDenganJadwal?

    @Insert
    suspend fun tambahRencana(rencana: RencanaObatEntity)

    @Insert
    suspend fun tambahJadwal(jadwal: List<JadwalObatEntity>)

    @Query("UPDATE rencana_obat SET stok = :stok WHERE id = :id")
    suspend fun ubahStok(id: Int, stok: Int)

    @Query("UPDATE jadwal_obat SET status = :status WHERE rencanaId = :id AND jam = :jam")
    suspend fun ubahStatus(id: Int, jam: String, status: StatusJadwal)
}
